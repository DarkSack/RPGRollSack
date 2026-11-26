package com.sack.rpgroll.structures.deposit;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.MainQueue;
import com.sack.rpgroll.structures.api.MaterialProvider;
import com.sack.rpgroll.structures.material.MaterialRegistry;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.storage.Database;
import com.sack.rpgroll.structures.storage.Dialect;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.Movement;
import com.sack.rpgroll.structures.storage.ProjectRecord;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Apagado con escrituras en vuelo: en Paper, dentro de onDisable runTask lanza y las tareas en cola se cancelan. Con
 * la cola propia, la barrera de la base de datos y el vaciado en el sitio, lo ya apuntado se entrega (o va a
 * pendientes) igualmente.
 */
class ShutdownDeliveryTest {

    static final String KEY = "minecraft:stone";

    @TempDir Path dir;
    Database db;
    ProjectRepository repo;
    final MainQueue queue = new MainQueue(Logger.getAnonymousLogger());
    final UUID owner = UUID.randomUUID();
    final Player player = mock(Player.class);
    final PlayerInventory inv = mock(PlayerInventory.class);
    final List<Integer> given = new ArrayList<>();
    final List<Project> finished = new ArrayList<>();
    Player online = player;
    ReturnService service;

    @BeforeEach void setUp() throws Exception {
        db = Database.open(Dialect.SQLITE, "jdbc:sqlite:" + dir.resolve("data.db"), "", "",
                Logger.getAnonymousLogger());
        repo = new ProjectRepository(db, Dialect.SQLITE);
        MaterialRegistry materials = new MaterialRegistry();
        materials.register(new MaterialProvider() {
            public String type() { return "minecraft"; }
            public boolean knows(String id) { return true; }
            public boolean matches(String id, ItemStack s) { return false; }
            public Optional<ItemStack> sample(String id) { return Optional.of(stack(1)); }
        });
        when(player.getUniqueId()).thenReturn(owner);
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(inv);
        when(inv.addItem(any(ItemStack.class))).thenAnswer(i -> {
            given.add(((ItemStack) i.getArgument(0)).getAmount());
            return new HashMap<Integer, ItemStack>();
        });
        service = new ReturnService(repo, materials, finished::add, id -> id.equals(owner) ? online : null,
                queue::execute, (p, key) -> { }, s -> new byte[] { 7 }, b -> stack(1), null, Logger.getAnonymousLogger());
    }

    @AfterEach void tearDown() {
        db.close();
    }

    ItemStack stack(int amount) {
        ItemStack s = mock(ItemStack.class);
        AtomicInteger n = new AtomicInteger(amount);
        when(s.getMaxStackSize()).thenReturn(4);
        when(s.getAmount()).thenAnswer(i -> n.get());
        doAnswer(i -> { n.set(i.getArgument(0)); return null; }).when(s).setAmount(anyInt());
        when(s.clone()).thenAnswer(i -> stack(n.get()));
        return s;
    }

    /** Obra en la base de datos con 8 depositadas. */
    Project project(ProjectState state) {
        ProjectRecord r = new ProjectRecord(UUID.randomUUID(), UUID.randomUUID(), "casa", owner, state, "world",
                0, 64, 0, Rotation.NONE, 0, 0, 0, 10, null, 1L, 1L);
        repo.insert(r, Map.of(KEY, 8), 1, 1, 1).join();
        assertTrue(repo.applyMovement(new Movement(UUID.randomUUID(), r.id(), owner, Movement.Kind.DEPOSIT, KEY, 8))
                .join());
        return new Project(r, Map.of(KEY, new MaterialRow(8, 8, 0)));
    }

    boolean shutdown() {
        return queue.drainAtShutdown(() -> db.submit(c -> null), 5_000, 5);
    }

    @Test void withdrawInFlightIsDeliveredAtShutdown() {
        Project p = project(ProjectState.WAITING_MATERIALS);
        CompletableFuture<Boolean> f = service.withdraw(player, p);
        db.submit(c -> null).join();
        assertFalse(f.isDone(), "sin vaciar la cola, la entrega espera (con runTask se habría cancelado)");
        assertTrue(shutdown());
        assertTrue(f.join());
        assertEquals(8, given.stream().mapToInt(Integer::intValue).sum());
        assertFalse(p.busy());
    }

    @Test void cancelInFlightIsDeliveredAtShutdown() {
        Project p = project(ProjectState.READY);
        CompletableFuture<Boolean> f = service.cancel(player, p, 100);
        assertTrue(shutdown());
        assertTrue(f.join());
        assertEquals(8, given.stream().mapToInt(Integer::intValue).sum());
        assertEquals(List.of(p), finished);
        assertEquals(ProjectState.CANCELLED, p.state());
    }

    @Test void cancelForOfflineOwnerGoesToPendingAtShutdown() {
        online = null;
        Project p = project(ProjectState.READY);
        service.cancel(player, p, 100);
        assertTrue(shutdown());
        // 8 en pilas de 4: dos filas, ya escritas antes de cerrar (la segunda barrera las espera).
        assertEquals(2, repo.pending(owner).join().size());
    }

    @Test void claimNeedsTwoRoundsAndStillDelivers() {
        repo.addPending(owner, new byte[] { 7 }).join();
        CompletableFuture<Integer> f = service.claimAll(player);
        assertTrue(shutdown());
        assertEquals(1, f.join());
        assertEquals(List.of(1), given);
        assertTrue(repo.pending(owner).join().isEmpty());
    }

    @Test void oneFailingTaskDoesNotStopTheRest() {
        List<String> ran = new ArrayList<>();
        queue.execute(() -> { throw new IllegalStateException("roto"); });
        queue.execute(() -> ran.add("b"));
        assertEquals(2, queue.drain());
        assertEquals(List.of("b"), ran);
    }
}
