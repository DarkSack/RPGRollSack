package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.config.WorkbenchSettings.Cost;
import com.sack.rpgroll.structures.config.WorkbenchSettings.TierSettings;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.UpgradeRules;
import com.sack.rpgroll.structures.workbench.UpgradeRules.UpgradeCheck;
import com.sack.rpgroll.structures.workbench.WorkbenchService;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/** Requisitos del siguiente nivel (verde si se tiene, rojo si falta) y el botón «Mejorar». 4 filas. */
public final class UpgradeMenu extends InventoryGUI {
    public static final int NIVEL = 4;
    public static final int REQUISITOS_DESDE = 10;
    public static final int REQUISITOS_MAX = 7;
    public static final int MEJORAR = 22;
    public static final int VOLVER = 31;

    public static final String KEY_HAVE = "upgrade.have";
    public static final String KEY_MISSING = "upgrade.missing";

    /** Una línea de requisitos: {@code missing} es lo que le falta al jugador (0 = lo tiene). */
    public record Line(String itemId, int amount, int missing, String key) {}

    /** Lo que falta de cada coste según lo que el jugador lleva (solo los que faltan). */
    public static Map<String, Integer> shortfall(List<Cost> costs, Map<String, Integer> playerHas) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Cost c : costs) {
            int s = c.amount() - playerHas.getOrDefault(c.itemId(), 0);
            if (s > 0) {
                out.merge(c.itemId(), s, Integer::sum);
            }
        }
        return out;
    }

    /** Una línea por coste: {@code upgrade.missing} si el ítem falta, {@code upgrade.have} si no. */
    public static List<Line> lines(List<Cost> costs, Map<String, Integer> missing) {
        List<Line> out = new ArrayList<>();
        for (Cost c : costs) {
            int m = missing.getOrDefault(c.itemId(), 0);
            out.add(new Line(c.itemId(), c.amount(), m, m > 0 ? KEY_MISSING : KEY_HAVE));
        }
        return out;
    }

    /**
     * Clave de lang del mensaje para un resultado, o vacío si no hay que decir nada. {@code MaxTier} solo se
     * dice si la mesa ya es ARQUITECTO: {@code WorkbenchService.upgrade} también lo devuelve (sin efecto ni
     * cobro) cuando ya hay una mejora o una rotura en curso, y eso se ignora en silencio.
     */
    public static Optional<String> resultKey(UpgradeCheck check, WorkbenchTier current) {
        if (check instanceof UpgradeRules.Ok) {
            return Optional.of("upgrade.done");
        } else if (check instanceof UpgradeRules.MaxTier) {
            return current.next().isEmpty() ? Optional.of("upgrade.max-tier") : Optional.empty();
        } else if (check instanceof UpgradeRules.MissingItems) {
            return Optional.of("upgrade.result-missing");
        } else if (check instanceof UpgradeRules.NotEnoughMoney) {
            return Optional.of("upgrade.no-money");
        }
        return Optional.of("upgrade.no-permission");
    }

    private final StructuresPlugin plugin;
    private final UUID benchId;
    private final WorkbenchTier tier;

    public UpgradeMenu(Player player, StructuresPlugin plugin, WorkbenchRecord bench) {
        super(player, StructuresMenus.skin().title(StructuresMenus.background(StructuresMenus.MEJORAS, bench.tier()),
                plugin.menuTitle("upgrade")), 36);
        this.plugin = plugin;
        this.benchId = bench.id();
        this.tier = bench.tier();
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.MEJORAS.dress(slot, item));
    }

    @Override
    public void build() {
        StructuresMenus.fill(inventory);
        var lang = plugin.lang();
        put(NIVEL, StructuresMenus.tierIcon(plugin, tier));
        Optional<WorkbenchTier> next = tier.next();
        if (next.isPresent()) {
            TierSettings ts = plugin.workbench().tiers().get(next.get());
            Map<String, Integer> has = WorkbenchService.count(player.getInventory(), ts.upgradeCost());
            List<Line> lines = lines(ts.upgradeCost(), shortfall(ts.upgradeCost(), has));
            for (int i = 0; i < Math.min(lines.size(), REQUISITOS_MAX); i++) {
                put(REQUISITOS_DESDE + i, requirement(lines.get(i)));
            }
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("upgrade.to", "tier", StructuresMenus.tierName(plugin, next.get())));
            if (ts.upgradeMoney() > 0) {
                lore.add(lang.component("upgrade.money", "amount", String.format(Locale.ROOT, "%.2f", ts.upgradeMoney())));
            }
            put(MEJORAR, StructuresMenus.item(Material.ANVIL, lang.component("upgrade.button"), lore));
        } else {
            put(MEJORAR, StructuresMenus.item(Material.BARRIER, lang.component("upgrade.max-tier"),
                    List.of(lang.component("upgrade.max-lore"))));
        }
        put(VOLVER, StructuresMenus.item(Material.ARROW, lang.component("menu.back"),
                List.of(lang.component("menu.back-lore"))));
    }

    private ItemStack requirement(Line line) {
        Material m = Material.matchMaterial(line.itemId());
        Component name = m != null ? Component.translatable(m.translationKey()) : Component.text(line.itemId());
        Component text = plugin.lang().component(line.key(), "item", name, "amount", String.valueOf(line.amount()),
                "missing", String.valueOf(line.missing()));
        return StructuresMenus.item(m != null ? m : Material.BARRIER, text, List.of());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == VOLVER) {
            reopenCenter();
        } else if (slot == MEJORAR && tier.next().isPresent()) {
            new ConfirmMenu(player, plugin, tier, StructuresMenus.item(Material.ANVIL,
                    plugin.lang().component("upgrade.confirm"), List.of()), this::doUpgrade, this::reopenSelf).open();
        }
    }

    private void doUpgrade() {
        WorkbenchTier before = tier;
        plugin.service().upgrade(player, benchId, before).whenComplete((check, err) -> {
            if (err != null) {
                plugin.getLogger().log(Level.WARNING, "Mejora fallida de la mesa " + benchId, err);
                plugin.lang().send(player, "upgrade.error");
            } else {
                // El nivel de ahora, no el de cuando se abrió el menú: otro gestor pudo mejorarla mientras tanto.
                WorkbenchTier current = plugin.service().byId(benchId).map(WorkbenchRecord::tier).orElse(before);
                resultKey(check, current).ifPresent(k -> plugin.lang().send(player, k));
            }
            reopenSelf();
        });
    }

    private void reopenSelf() {
        plugin.service().byId(benchId).ifPresentOrElse(b -> new UpgradeMenu(player, plugin, b).open(), this::close);
    }

    private void reopenCenter() {
        plugin.service().byId(benchId).ifPresentOrElse(b -> new CenterMenu(player, plugin, b).open(), this::close);
    }
}
