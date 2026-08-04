package com.sack.rpgroll.items.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.rarity.Rarity;
import com.sack.rpgroll.items.rarity.RarityManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.Consumer;

/** Lista visual de todas las rarezas registradas, cada una coloreada con su propio color. */
public class RarityPickerGUI extends PaginatedGUI {

    private static final int SIZE = 54;
    private static final int CONTENT_SLOTS = 45;
    private static final int BACK_SLOT = 49;

    private final RarityManager rarityManager;
    private final Consumer<String> onSelect;
    private final Runnable onBack;
    private final List<Rarity> rarities;

    public RarityPickerGUI(Player player, RarityManager rarityManager, Consumer<String> onSelect, Runnable onBack) {

        super(player, Component.text("Elegí una rareza", NamedTextColor.GOLD), SIZE, CONTENT_SLOTS);

        this.rarityManager = rarityManager;
        this.onSelect = onSelect;
        this.onBack = onBack;
        this.rarities = rarityManager.getAll().stream().sorted((a, b) -> a.id().compareTo(b.id())).toList();
    }

    @Override
    protected int totalItemCount() {
        return rarities.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        Rarity rarity = rarities.get(absoluteIndex);

        setItem(contentSlot, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text(rarity.displayName(), rarity.color()).decoration(TextDecoration.BOLD, rarity.glow()))
                .setLore(Component.text("id: " + rarity.id(), NamedTextColor.GRAY),
                        Component.text(rarity.glow() ? "Brillo: sí" : "Brillo: no", NamedTextColor.GRAY))
                .build());
    }

    @Override
    protected void renderExtras() {
        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {
        onSelect.accept(rarities.get(absoluteIndex).id());
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {
        if (event.getSlot() == BACK_SLOT) {
            onBack.run();
        }
    }

}
