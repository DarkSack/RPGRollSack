package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.config.WorkbenchSettings.Size;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.WorkbenchService;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Skin, ids de fondo y casillas de los menús de la mesa. El plan 8 dibuja los fondos con estos datos. */
public final class StructuresMenus {
    private StructuresMenus() {}

    /**
     * Casillas de un menú con fondo dibujado. {@code painted}: botones fijos cuyo dibujo va en el fondo (el ítem
     * es invisible y solo deja el tooltip). {@code visible}: ítems que se ven sobre un hueco dibujado (cambian o
     * dependen de la mesa). El resto de casillas es relleno o contenido. El generador de fondos copia esta tabla.
     */
    public record Layout(String id, int rows, List<Integer> painted, List<Integer> visible) {
        public boolean isPainted(int slot) {
            return painted.contains(slot);
        }

        /** El ítem tal como va en {@code slot}: invisible si su dibujo está en el fondo. */
        public ItemStack dress(int slot, ItemStack item) {
            return isPainted(slot) ? skin().button(item) : item;
        }

        /** Si el pack de Sackito tiene su fondo. Los menús nuevos aún no: sin él saldrían «▯» y botones invisibles. */
        public boolean drawn() {
            return WITH_ART.contains(id);
        }

        /** El skin del módulo si este menú tiene fondo dibujado; si no, {@link MenuSkin#OFF}. */
        public MenuSkin skin() {
            return drawn() ? StructuresMenus.skin() : MenuSkin.OFF;
        }

        /** Relleno de este menú: el del skin si tiene fondo, el normal si no. */
        public void fill(Inventory inv) {
            ItemStack filler = drawn() ? filler() : plainFiller();
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, filler.clone());
            }
        }
    }

    // shortcut: solo estos cinco tienen fondo en sackito:menus; al dibujar los demás (plan 4), añadirlos aquí.
    private static final java.util.Set<String> WITH_ART = java.util.Set.of("centro", "mejoras", "acceso", "ayuda",
            "confirmar");

    public static final Layout CENTRO = new Layout("centro", 6, List.of(49, 53),
            List.of(4, 20, 22, 24, 29, 31, 33));
    public static final Layout MEJORAS = new Layout("mejoras", 4, List.of(31),
            Stream.concat(Stream.of(4, 22), IntStream.rangeClosed(10, 16).boxed()).toList());
    public static final Layout ACCESO = new Layout("acceso", 6, List.of(48),
            Stream.concat(IntStream.range(0, 45).boxed(), Stream.of(45, 50, 53)).toList());
    public static final Layout AYUDA = new Layout("ayuda", 3, List.of(), List.of(13, 18, 22, 26));
    public static final Layout CONFIRMAR = new Layout("confirmar", 3, List.of(11, 15), List.of(13));

    public static final Layout CATEGORIAS = new Layout("categorias", 6, List.of(), with(grid(1, 3), 49));
    public static final Layout LISTA = new Layout("lista", 6, List.of(49),
            Stream.concat(Stream.of(4), with(grid(1, 4), 45, 53).stream()).toList());
    public static final Layout FICHA = new Layout("ficha", 6, List.of(49),
            Stream.concat(Stream.of(4), with(grid(2, 2), 47, 51).stream()).toList());
    public static final Layout MATERIALES = new Layout("materiales", 6, List.of(45, 47, 49),
            Stream.concat(Stream.of(4), with(grid(1, 4), 51, 53).stream()).toList());
    public static final Layout VISTA = new Layout("vista", 3,
            with(IntStream.rangeClosed(10, 16).boxed().toList(), 18, 26), List.of(4, 22));
    public static final Layout OBRA = new Layout("obra", 4, List.of(22, 24, 31), List.of(4, 13, 20));
    public static final Layout CONSTRUCCIONES = new Layout("construcciones", 6, List.of(),
            with(IntStream.range(0, 45).boxed().toList(), 45, 49, 53));

    public static final List<Layout> LAYOUTS = List.of(CENTRO, MEJORAS, ACCESO, AYUDA, CONFIRMAR, CATEGORIAS, LISTA,
            FICHA, MATERIALES, VISTA, OBRA, CONSTRUCCIONES);

    /** Columnas 1 a 7 de {@code rows} filas desde {@code firstRow}: el hueco de contenido con marco. */
    public static List<Integer> grid(int firstRow, int rows) {
        return IntStream.range(firstRow, firstRow + rows).boxed()
                .flatMap(r -> IntStream.rangeClosed(r * 9 + 1, r * 9 + 7).boxed()).toList();
    }

    private static List<Integer> with(List<Integer> slots, Integer... more) {
        return Stream.concat(slots.stream(), Stream.of(more)).toList();
    }

    // ---- lógica pura de los menús (con pruebas) ----

    /** Cuántas páginas de {@code size} hacen falta (mínimo 1, aunque no haya nada). */
    public static int pageCount(int total, int size) {
        return Math.max(1, (total + size - 1) / size);
    }

    /** Los elementos de una página (la página se acota a lo que existe). */
    public static <T> List<T> pageOf(List<T> all, int page, int size) {
        int p = Math.max(0, Math.min(page, pageCount(all.size(), size) - 1));
        return all.subList(Math.min(all.size(), p * size), Math.min(all.size(), (p + 1) * size));
    }

    /** Segundos de obra para {@code blocks} bloques: {@code ceil(blocks / porCiclo) * ticksPorCiclo / 20}, hacia arriba. */
    public static long estimateSeconds(int blocks, int blocksPerCycle, int ticksPerCycle) {
        long cycles = (Math.max(0, blocks) + Math.max(1, blocksPerCycle) - 1L) / Math.max(1, blocksPerCycle);
        return (cycles * Math.max(1, ticksPerCycle) + 19) / 20;
    }

    /** {@code 45s}, {@code 3m 05s}, {@code 2h 03m}. */
    public static String duration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        if (seconds < 3600) {
            return String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60);
        }
        return String.format(Locale.ROOT, "%dh %02dm", seconds / 3600, seconds % 3600 / 60);
    }

    /** Porcentaje hecho, hacia abajo, entre 0 y 100; sin total cuenta como terminado. */
    public static int percent(int done, int total) {
        return total <= 0 ? 100 : (int) Math.min(100, Math.max(0, done * 100L / total));
    }

    /**
     * Hilo principal. Vuelve a mirar el acceso a la mesa (pudieron quitárselo con el menú abierto) y, ya en el hilo
     * principal, corre {@code action} si lo tiene y sigue con el menú {@code expected} abierto (null = no hace falta).
     * Sin acceso: aviso y cierra. Mesa que ya no existe: cierra.
     */
    public static void withAccess(Player p, StructuresPlugin plugin, UUID benchId, Inventory expected,
                                  Runnable action) {
        withAccess(p, plugin.service(), plugin.lang(), r -> sync(plugin, r), benchId, expected, action);
    }

    static void withAccess(Player p, WorkbenchService service, LangManager lang, Consumer<Runnable> main,
                           UUID benchId, Inventory expected, Runnable action) {
        Optional<WorkbenchRecord> bench = benchId == null ? Optional.empty() : service.byId(benchId);
        if (bench.isEmpty()) {
            main.accept(p::closeInventory);
            return;
        }
        CompletableFuture<Boolean> check;
        try {
            check = service.canUse(p, bench.get());
        } catch (RuntimeException e) {
            check = CompletableFuture.failedFuture(e);
        }
        check.whenComplete((ok, err) -> main.accept(() -> {
            if (err != null || !Boolean.TRUE.equals(ok)) {
                lang.send(p, "workbench.use.denied");
                p.closeInventory();
            } else if (isOpen(p, expected)) {
                action.run();
            }
        }));
    }

    /** Si el jugador sigue mirando ese inventario ({@code null} = no se espera ninguno: sí). */
    public static boolean isOpen(Player p, Inventory expected) {
        return expected == null || expected.equals(p.getOpenInventory().getTopInventory());
    }

    /** Vuelve al hilo principal (las respuestas de la base de datos tocan inventarios y menús). */
    public static void sync(StructuresPlugin plugin, Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }

    /** {@code centro_taller}, {@code centro_ingenio}, {@code centro_arcano}... */
    public static String background(String menu, WorkbenchTier tier) {
        return menu + "_" + family(tier);
    }

    public static String background(Layout menu, WorkbenchTier tier) {
        return background(menu.id(), tier);
    }

    public static String family(WorkbenchTier tier) {
        return switch (tier) {
            case CONSTRUCTOR -> "taller";
            case INGENIERO -> "ingenio";
            case ARQUITECTO -> "arcano";
        };
    }

    public static MenuSkin skin() {
        return MenuSkin.get(StructuresPlugin.MENU_MODULE);
    }

    /** Sin cursiva: el cliente pone en cursiva todo nombre y lore propio que no diga lo contrario. */
    public static Component plain(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> plain(List<Component> lines) {
        return lines.stream().map(StructuresMenus::plain).toList();
    }

    /** Relleno sin recuadro de nombre (sin pack se veía un tooltip vacío, y su id con F3+H). */
    public static ItemStack filler() {
        return skin().filler(plainFiller());
    }

    private static ItemStack plainFiller() {
        ItemStack plain = ItemBuilder.createFiller();
        ItemMeta meta = plain.getItemMeta();
        meta.setHideTooltip(true);
        plain.setItemMeta(meta);
        return plain;
    }

    public static void fill(Inventory inv) {
        ItemStack filler = filler();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler.clone());
        }
    }

    /** Un ítem de menú con nombre y lore sin cursiva. Visible: para un botón dibujado, {@link Layout#dress}. */
    public static ItemStack item(Material icon, Component name, List<Component> lore) {
        return new ItemBuilder(icon).setName(plain(name)).setLore(plain(lore)).build();
    }

    /** Icono del nivel (casilla 4 del centro y de mejoras): tamaño máximo y nivel siguiente. */
    public static ItemStack tierIcon(StructuresPlugin plugin, WorkbenchTier tier) {
        var lang = plugin.lang();
        Size size = plugin.workbench().tiers().get(tier).maxSize();
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("menu.center.level-size", "width", size.width(), "height", size.height(),
                "length", size.length()));
        lore.add(tier.next()
                .map(n -> lang.component("menu.center.level-next", "tier", tierName(plugin, n)))
                .orElseGet(() -> lang.component("menu.center.level-max")));
        return item(Material.NETHER_STAR, lang.component("menu.center.level", "tier", tierName(plugin, tier)), lore);
    }

    /** Título con el fondo {@code <menu>_<familia>} y el texto de {@code gui.yml}. */
    public static Component title(StructuresPlugin plugin, Layout layout, WorkbenchTier tier, String menu) {
        return layout.skin().title(background(layout, tier), plugin.menuTitle(menu));
    }

    /** Icono de un id de material ({@code tipo:id}) con nombre y lore propios; {@code PAPER} si no hay muestra. */
    public static ItemStack icon(StructuresPlugin plugin, String id, Component name, List<Component> lore) {
        ItemStack sample = id == null || id.isBlank() ? null : plugin.materials().sample(id).orElse(null);
        if (sample == null || sample.isEmpty()) {
            return item(Material.PAPER, name, lore);
        }
        ItemStack out = sample.clone();
        out.setAmount(1);
        ItemMeta meta = out.getItemMeta();
        meta.displayName(plain(name));
        meta.lore(plain(lore));
        out.setItemMeta(meta);
        return out;
    }

    /** Nombre visible de un material del catálogo; el id si no hay proveedor. */
    public static Component materialName(StructuresPlugin plugin, String id) {
        return plugin.materials().sample(id).map(ItemStack::effectiveName).orElseGet(() -> Component.text(id));
    }

    /** Vuelve al centro de la mesa, o cierra si se abrió sin mesa (comando) o la mesa ya no está. */
    public static void back(Player player, StructuresPlugin plugin, UUID benchId) {
        Optional<WorkbenchRecord> bench = benchId == null ? Optional.empty() : plugin.service().byId(benchId);
        if (bench.isPresent()) {
            new CenterMenu(player, plugin, bench.get()).open();
        } else {
            player.closeInventory();
        }
    }

    /** Botón de volver (con mesa) o de cerrar (sin mesa). */
    public static ItemStack backOrClose(StructuresPlugin plugin, UUID benchId) {
        var lang = plugin.lang();
        return benchId == null
                ? item(Material.BARRIER, lang.component("menu.close"), List.of(lang.component("menu.close-lore")))
                : item(Material.BARRIER, lang.component("menu.back"), List.of(lang.component("menu.back-lore")));
    }

    public static String tierName(StructuresPlugin plugin, WorkbenchTier tier) {
        return plugin.lang().raw("tier." + tier.name().toLowerCase(Locale.ROOT));
    }
}
