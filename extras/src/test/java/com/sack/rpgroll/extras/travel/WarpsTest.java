package com.sack.rpgroll.extras.travel;

import com.sack.rpgroll.extras.travel.Warps.Warp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpsTest {

    @TempDir
    File folder;

    @Test
    void warpsSurviveARestartAndCountPerOwner() throws IOException {
        File file = new File(folder, "warps.yml");
        UUID ana = UUID.randomUUID();
        UUID luis = UUID.randomUUID();
        Warp tienda = new Warp("tienda", ana, "Ana", "world", 10.5, 64, -3.5, 90f, 0f);

        Warps warps = new Warps(file);
        warps.put(tienda);
        warps.put(new Warp("granja", ana, "Ana", "world", 0, 70, 0, 0f, 0f));
        warps.put(new Warp("mina", luis, "Luis", "recursos", 5, 12, 5, 0f, 0f));
        warps.remove("granja");

        Warps reloaded = new Warps(file);
        assertEquals(Optional.of(tienda), reloaded.get("tienda"));
        assertTrue(reloaded.get("granja").isEmpty());
        assertEquals(1, reloaded.ownedBy(ana));
        assertEquals(1, reloaded.ownedBy(luis));
    }
}
