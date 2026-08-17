package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private final Runnable onBack;

    public RulesEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.rules.title", "id", session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
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

        setItem(LEVEL_SLOT, textField(Material.EXPERIENCE_BOTTLE, lang.raw("editor.rules.level"),
                String.valueOf(req.level()), lang.raw("editor.rules.level_hint")));
        setItem(RACE_SLOT, textField(Material.PLAYER_HEAD, lang.raw("editor.rules.race"), req.race(),
                lang.raw("editor.rules.text_hint")));
        setItem(CLASS_SLOT, textField(Material.IRON_SWORD, lang.raw("editor.rules.class"), req.playerClass(),
                lang.raw("editor.rules.text_hint")));
        setItem(PROFESSION_SLOT, textField(Material.IRON_PICKAXE, lang.raw("editor.rules.profession"),
                req.profession(), lang.raw("editor.rules.text_hint")));
        setItem(SKILL_SLOT, textField(Material.BOOK, lang.raw("editor.rules.skill"), req.skill(),
                lang.raw("editor.rules.text_hint")));
        setItem(TRAIT_SLOT, textField(Material.NETHER_STAR, lang.raw("editor.rules.trait"), req.trait(),
                lang.raw("editor.rules.text_hint")));
        setItem(PERMISSION_SLOT, textField(Material.PAPER, lang.raw("editor.rules.permission"), req.permission(),
                lang.raw("editor.rules.text_hint")));
        setItem(MONEY_REQ_SLOT, textField(Material.GOLD_INGOT, lang.raw("editor.rules.money_req"),
                String.valueOf(req.money()), lang.raw("editor.rules.money_req_hint")));
        setItem(QUESTS_SLOT, new ItemBuilder(Material.MAP)
                .setName(lang.component("editor.rules.quests"))
                .setLore(Component.text(req.completedQuests().isEmpty() ? lang.raw("editor.rules.quests_none")
                        : String.join(", ", req.completedQuests()), NamedTextColor.GRAY),
                        lang.component("editor.rules.quests_hint"))
                .build());

        ItemDurabilityDef durability = session.durability;
        boolean durabilityOn = durability.enabled();

        setItem(DURABILITY_TOGGLE_SLOT, new ItemBuilder(durabilityOn ? Material.ANVIL : Material.GRAY_DYE)
                .setName(lang.component("editor.rules.durability_label",
                        "state", durabilityOn ? lang.raw("editor.rules.durability_on") : lang.raw("editor.rules.durability_off")))
                .setLore(lang.component("editor.rules.durability_hint",
                        "action", durabilityOn ? lang.raw("editor.rules.durability_deactivate") : lang.raw("editor.rules.durability_activate")))
                .build());

        if (durabilityOn) {
            setItem(DURABILITY_MAX_SLOT, textField(Material.IRON_INGOT, lang.raw("editor.rules.durability_max"),
                    String.valueOf(durability.maxDurability()), lang.raw("editor.rules.durability_max_hint")));
            setItem(REPAIRABLE_SLOT, new ItemBuilder(durability.repairable() ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setName(lang.component("editor.rules.repairable",
                            "value", durability.repairable() ? "SI" : "NO"))
                    .build());
            setItem(DEGRADE_SLOT, textField(Material.FLINT, lang.raw("editor.rules.degrade"),
                    String.valueOf(durability.degradePerUse()), lang.raw("editor.rules.degrade_hint")));
            setItem(AUTO_REPAIR_SLOT, textField(Material.CLOCK, lang.raw("editor.rules.auto_repair"),
                    String.valueOf(durability.autoRepairPerMinute()), lang.raw("editor.rules.auto_repair_hint")));
        } else {
            org.bukkit.inventory.ItemStack disabled = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                    .setName(lang.component("editor.rules.durability_disabled_hint"))
                    .build();
            setItem(DURABILITY_MAX_SLOT, disabled);
            setItem(REPAIRABLE_SLOT, disabled);
            setItem(DEGRADE_SLOT, disabled);
            setItem(AUTO_REPAIR_SLOT, disabled);
        }

        setItem(SELL_SLOT, textField(Material.EMERALD, lang.raw("editor.rules.sell_price"),
                String.valueOf(session.sellPrice), lang.raw("editor.rules.price_hint")));
        setItem(BUY_SLOT, textField(Material.DIAMOND, lang.raw("editor.rules.buy_price"),
                String.valueOf(session.buyPrice), lang.raw("editor.rules.price_hint")));

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    private org.bukkit.inventory.ItemStack textField(Material material, String name, String value, String hint) {
        return new ItemBuilder(material)
                .setName(Component.text(name + ": " + (value == null || value.isBlank() ? lang.raw("editor.rules.undefined") : value),
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
            case RACE_SLOT -> promptText(lang.raw("editor.rules.race"), session.requirements.race(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), v, r.playerClass(), r.profession(),
                            r.skill(), r.trait(), r.permission(), r.money(), r.completedQuests())));
            case CLASS_SLOT -> promptText(lang.raw("editor.rules.class"), session.requirements.playerClass(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), r.race(), v, r.profession(), r.skill(),
                            r.trait(), r.permission(), r.money(), r.completedQuests())));
            case PROFESSION_SLOT -> promptText(lang.raw("editor.rules.profession"), session.requirements.profession(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), r.race(), r.playerClass(), v,
                            r.skill(), r.trait(), r.permission(), r.money(), r.completedQuests())));
            case SKILL_SLOT -> promptText(lang.raw("editor.rules.skill"), session.requirements.skill(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), r.race(), r.playerClass(),
                            r.profession(), v, r.trait(), r.permission(), r.money(), r.completedQuests())));
            case TRAIT_SLOT -> promptText(lang.raw("editor.rules.trait"), session.requirements.trait(),
                    v -> withRequirements(r -> new ItemRequirements(r.level(), r.race(), r.playerClass(),
                            r.profession(), r.skill(), v, r.permission(), r.money(), r.completedQuests())));
            case PERMISSION_SLOT -> promptText(lang.raw("editor.rules.permission"), session.requirements.permission(),
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
                lang.raw("editor.rules.prompt_field", "label", label,
                        "current", current == null ? lang.raw("editor.rules.prompt_field_none") : current),
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
        session.chatPromptManager.prompt(player, lang.raw("editor.rules.prompt_quests"), value -> {

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
