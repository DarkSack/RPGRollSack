package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.economy.tax.TaxRule;
import com.sack.rpgroll.economy.tax.TaxRuleManager;
import com.sack.rpgroll.economy.tax.TaxType;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class TaxBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final TaxRuleManager taxRuleManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<TaxRule> rules;

    public TaxBrowserGUI(Player player, TaxRuleManager taxRuleManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Impuestos", NamedTextColor.GOLD), SIZE);
        this.taxRuleManager = taxRuleManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.rules = List.copyOf(taxRuleManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < rules.size() && i < 36; i++) {

            TaxRule rule = rules.get(i);

            setItem(i, new ItemBuilder(rule.enabled() ? Material.GOLD_NUGGET : Material.IRON_NUGGET)
                    .setName(Component.text(rule.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text("id: " + rule.id(), NamedTextColor.GRAY),
                            Component.text("tipo: " + rule.type(), NamedTextColor.GRAY),
                            Component.text("tasa: " + rule.ratePercent() + "%", NamedTextColor.GOLD),
                            Component.text(rule.enabled() ? "Activo" : "Desactivado",
                                    rule.enabled() ? NamedTextColor.GREEN : NamedTextColor.RED),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear regla nueva", NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < rules.size() && slot < 36) {
            new TaxEditorGUI(player, rules.get(slot), taxRuleManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id de la nueva regla tributaria:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (taxRuleManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una regla con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            taxRuleManager.save(new TaxRule(id, id, TaxType.SALE, 0, List.of(), true));
            reopen();
        });
    }

    private void reopen() {
        this.rules = List.copyOf(taxRuleManager.getAll());
        open();
    }

}
