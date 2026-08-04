package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemDurabilityDef;
import com.sack.rpgroll.items.core.ItemRequirements;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Editor combinado de requisitos, durabilidad propia y economía (precio de compra/venta). */
public class RulesEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int LEVEL_SLOT = 10;
    private static final int RACE_SLOT = 11;
    private static final int CLASS_SLOT = 12;
    private static final int PROFESSION_SLOT = 13;
    private static final int SKILL_SLOT = 14;
    private static final int TRAIT_SLOT = 15;
    private static final int PERMISSION_SLOT = 16;
    private static final int MONEY_REQ_SLOT = 17;
    private static final int QUESTS_SLOT = 18;

    private static final int DURABILITY_TOGGLE_SLOT = 28;
    private static final int DURABILITY_MAX_SLOT = 29;
    private static final int REPAIRABLE_SLOT = 30;
    private static final int DEGRADE_SLOT = 31;
    private static final int AUTO_REPAIR_SLOT = 32;

    private static final int SELL_SLOT = 37;
    private static final int BUY_SLOT = 38;

    private static final int BACK_SLOT = 49;

    private final EditorSession session;
    private final Runnable onBack;

    public RulesEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, Component.text("Reglas: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY))
                    .build());
        }

        ItemRequirements req = session.requirements;

        setItem(LEVEL_SLOT, textField(Material.EXPERIENCE_BOTTLE, "Nivel requerido", String.valueOf(req.level()),
                "Click: +1 · Shift: +10 · Derecho: -1/-10"));
        setItem(RACE_SLOT, textField(Material.PLAYER_HEAD, "Raza", req.race(), "Click para escribir · Shift: borrar"));
        setItem(CLASS_SLOT, textField(Material.IRON_SWORD, "Clase", req.playerClass(),
                "Click para escribir · Shift: borrar"));
        setItem(PROFESSION_SLOT, textField(Material.IRON_PICKAXE, "Profesión", req.profession(),
                "Click para escribir · Shift: borrar"));
        setItem(SKILL_SLOT, textField(Material.BOOK, "Skill", req.skill(), "Click para escribir · Shift: borrar"));
        setItem(TRAIT_SLOT, textField(Material.NETHER_STAR, "Trait", req.trait(),
                "Click para escribir · Shift: borrar"));
        setItem(PERMISSION_SLOT, textField(Material.PAPER, "Permiso", req.permission(),
                "Click para escribir · Shift: borrar"));
        setItem(MONEY_REQ_SLOT, textField(Material.GOLD_INGOT, "Dinero requerido", String.valueOf(req.money()),
                "Click: +10 · Shift: +100 · Derecho: -10/-100"));
        setItem(QUESTS_SLOT, new ItemBuilder(Material.MAP)
                .setName(Component.text("Quests completadas", NamedTextColor.YELLOW))
                .setLore(Component.text(req.completedQuests().isEmpty() ? "(ninguna)"
                        : String.join(", ", req.completedQuests()), NamedTextColor.GRAY),
                        Component.text("Click para reescribir (separadas por coma)", NamedTextColor.DARK_GRAY))
                .build());

        ItemDurabilityDef durability = session.durability;
        boolean durabilityOn = durability.enabled();

        setItem(DURABILITY_TOGGLE_SLOT, new ItemBuilder(durabilityOn ? Material.ANVIL : Material.GRAY_DYE)
                .setName(Component.text("Durabilidad: " + (durabilityOn ? "activada" : "desactivada"),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click para " + (durabilityOn ? "desactivar" : "activar"),
                        NamedTextColor.GRAY))
                .build());

        if (durabilityOn) {
            setItem(DURABILITY_MAX_SLOT, textField(Material.IRON_INGOT, "Durabilidad máxima",
                    String.valueOf(durability.maxDurability()), "Click: +10 · Shift: +100 · Derecho: -10/-100"));
            setItem(REPAIRABLE_SLOT, new ItemBuilder(durability.repairable() ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setName(Component.text("Reparable: " + (durability.repairable() ? "SI" : "NO"),
                            NamedTextColor.AQUA))
                    .build());
            setItem(DEGRADE_SLOT, textField(Material.FLINT, "Degradación por uso",
                    String.valueOf(durability.degradePerUse()), "Click: +1 · Shift: +5 · Derecho: -1/-5"));
            setItem(AUTO_REPAIR_SLOT, textField(Material.CLOCK, "Auto-reparación / minuto",
                    String.valueOf(durability.autoRepairPerMinute()), "Click: +1 · Shift: +5 · Derecho: -1/-5"));
        } else {
            org.bukkit.inventory.ItemStack disabled = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                    .setName(Component.text("Activá la durabilidad primero", NamedTextColor.DARK_GRAY))
                    .build();
            setItem(DURABILITY_MAX_SLOT, disabled);
            setItem(REPAIRABLE_SLOT, disabled);
            setItem(DEGRADE_SLOT, disabled);
            setItem(AUTO_REPAIR_SLOT, disabled);
        }

        setItem(SELL_SLOT, textField(Material.EMERALD, "Precio de venta", String.valueOf(session.sellPrice),
                "Click: +10 · Shift: +100 · Derecho: -10/-100"));
        setItem(BUY_SLOT, textField(Material.DIAMOND, "Precio de compra", String.valueOf(session.buyPrice),
                "Click: +10 · Shift: +100 · Derecho: -10/-100"));

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private org.bukkit.inventory.ItemStack textField(Material material, String name, String value, String hint) {
        return new ItemBuilder(material)
                .setName(Component.text(name + ": " + (value == null || value.isBlank() ? "(sin definir)" : value),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(hint, NamedTextColor.GRAY))
                .build();
    }

    private double delta(ClickType click, double small, double large) {
        return switch (click) {
            case LEFT -> small;
            case SHIFT_LEFT -> large;
            case RIGHT -> -small;
            case SHIFT_RIGHT -> -large;
            default -> 0;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        switch (event.getSlot()) {
            case LEVEL_SLOT -> adjustLevel(event.getClick());
            case RACE_SLOT -> promptText("Raza", session.requirements.race(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), v, r.playerClass(), r.profession(),
                            r.skill(), r.trait(), r.permission(), r.money(), r.completedQuests())));
            case CLASS_SLOT -> promptText("Clase", session.requirements.playerClass(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), r.race(), v, r.profession(), r.skill(),
                            r.trait(), r.permission(), r.money(), r.completedQuests())));
            case PROFESSION_SLOT -> promptText("Profesión", session.requirements.profession(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), r.race(), r.playerClass(), v,
                            r.skill(), r.trait(), r.permission(), r.money(), r.completedQuests())));
            case SKILL_SLOT -> promptText("Skill", session.requirements.skill(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), r.race(), r.playerClass(),
                            r.profession(), v, r.trait(), r.permission(), r.money(), r.completedQuests())));
            case TRAIT_SLOT -> promptText("Trait", session.requirements.trait(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), r.race(), r.playerClass(),
                            r.profession(), r.skill(), v, r.permission(), r.money(), r.completedQuests())));
            case PERMISSION_SLOT -> promptText("Permiso", session.requirements.permission(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), r.race(), r.playerClass(),
                            r.profession(), r.skill(), r.trait(), v, r.money(), r.completedQuests())));
            case MONEY_REQ_SLOT -> adjustMoneyRequirement(event.getClick());
            case QUESTS_SLOT -> promptQuests();
            case DURABILITY_TOGGLE_SLOT -> toggleDurability();
            case DURABILITY_MAX_SLOT -> adjustDurabilityMax(event.getClick());
            case REPAIRABLE_SLOT -> toggleRepairable();
            case DEGRADE_SLOT -> adjustDegrade(event.getClick());
            case AUTO_REPAIR_SLOT -> adjustAutoRepair(event.getClick());
            case SELL_SLOT -> adjustSellPrice(event.getClick());
            case BUY_SLOT -> adjustBuyPrice(event.getClick());
            case BACK_SLOT -> onBack.run();
            default -> {
            }
        }
    }

    private void withRequirements(java.util.function.UnaryOperator<ItemRequirements> transform) {
        session.requirements = transform.apply(session.requirements);
        build();
    }

    private void promptText(String label, String current, java.util.function.Consumer<String> onValue) {
        session.chatPromptManager.prompt(player,
                "Escribí el valor de " + label + " (actual: " + (current == null ? "ninguno" : current)
                        + "), o 'borrar':",
                value -> {
                    onValue.accept(value.trim().equalsIgnoreCase("borrar") ? null : value.trim());
                });
    }

    private void adjustLevel(ClickType click) {
        int delta = (int) delta(click, 1, 10);
        if (delta == 0) {
            return;
        }
        ItemRequirements r = session.requirements;
        session.requirements = new ItemRequirements(Math.max(0, r.level() + delta), r.race(), r.playerClass(),
                r.profession(), r.skill(), r.trait(), r.permission(), r.money(), r.completedQuests());
        build();
    }

    private void adjustMoneyRequirement(ClickType click) {
        double delta = delta(click, 10, 100);
        if (delta == 0) {
            return;
        }
        ItemRequirements r = session.requirements;
        session.requirements = new ItemRequirements(r.level(), r.race(), r.playerClass(), r.profession(), r.skill(),
                r.trait(), r.permission(), Math.max(0, r.money() + delta), r.completedQuests());
        build();
    }

    private void promptQuests() {
        session.chatPromptManager.prompt(player,
                "Escribí los ids de quest separados por coma (o 'borrar' para vaciar):", value -> {

                    List<String> quests = value.trim().equalsIgnoreCase("borrar")
                            ? List.of()
                            : List.of(value.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();

                    ItemRequirements r = session.requirements;
                    session.requirements = new ItemRequirements(r.level(), r.race(), r.playerClass(), r.profession(),
                            r.skill(), r.trait(), r.permission(), r.money(), quests);
                    build();
                });
    }

    private void toggleDurability() {

        ItemDurabilityDef d = session.durability;

        session.durability = d.enabled()
                ? ItemDurabilityDef.disabled()
                : new ItemDurabilityDef(Math.max(1, d.maxDurability()), true, 1, 0);

        build();
    }

    private void adjustDurabilityMax(ClickType click) {

        int delta = (int) delta(click, 10, 100);
        if (delta == 0 || !session.durability.enabled()) {
            return;
        }

        ItemDurabilityDef d = session.durability;
        session.durability = new ItemDurabilityDef(Math.max(1, d.maxDurability() + delta), d.repairable(),
                d.degradePerUse(), d.autoRepairPerMinute());
        build();
    }

    private void toggleRepairable() {

        if (!session.durability.enabled()) {
            return;
        }

        ItemDurabilityDef d = session.durability;
        session.durability = new ItemDurabilityDef(d.maxDurability(), !d.repairable(), d.degradePerUse(),
                d.autoRepairPerMinute());
        build();
    }

    private void adjustDegrade(ClickType click) {

        int delta = (int) delta(click, 1, 5);
        if (delta == 0 || !session.durability.enabled()) {
            return;
        }

        ItemDurabilityDef d = session.durability;
        session.durability = new ItemDurabilityDef(d.maxDurability(), d.repairable(),
                Math.max(0, d.degradePerUse() + delta), d.autoRepairPerMinute());
        build();
    }

    private void adjustAutoRepair(ClickType click) {

        int delta = (int) delta(click, 1, 5);
        if (delta == 0 || !session.durability.enabled()) {
            return;
        }

        ItemDurabilityDef d = session.durability;
        session.durability = new ItemDurabilityDef(d.maxDurability(), d.repairable(), d.degradePerUse(),
                Math.max(0, d.autoRepairPerMinute() + delta));
        build();
    }

    private void adjustSellPrice(ClickType click) {
        double delta = delta(click, 10, 100);
        if (delta != 0) {
            session.sellPrice = Math.max(0, session.sellPrice + delta);
            build();
        }
    }

    private void adjustBuyPrice(ClickType click) {
        double delta = delta(click, 10, 100);
        if (delta != 0) {
            session.buyPrice = Math.max(0, session.buyPrice + delta);
            build();
        }
    }

}
