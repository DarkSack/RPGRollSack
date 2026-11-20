package com.sack.rpgroll.mobs.bestiary;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.mobs.gui.MobMenus;
import com.sack.rpgroll.mobs.gui.PaginatedGUI;
import com.sack.rpgroll.mobs.rarity.MobRarities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** /bestiario: una casilla por especie; las que aún no has matado salen como «???». */
public final class BestiaryGUI extends PaginatedGUI {

    public static final String BACKGROUND = "bestiario";

    private static final int SIZE = 54;
    private static final int CONTENT = 45;
    private static final int PREVIOUS = 45;
    private static final int SUMMARY = 49;
    private static final int NEXT = 53;

    private final Bestiary bestiary;
    private final LangManager lang;
    private final List<Bestiary.Species> species;

    public BestiaryGUI(Player player, Bestiary bestiary, LangManager lang) {
        super(player, MobMenus.skin().title(BACKGROUND, lang.component("bestiary.title")), SIZE, CONTENT);
        this.bestiary = bestiary;
        this.lang = lang;
        this.species = bestiary.species();
    }

    private static Component plain(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = ItemStack.of(material);
        var meta = item.getItemMeta();
        meta.displayName(plain(name));
        meta.lore(lore.stream().map(BestiaryGUI::plain).toList());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected int totalItemCount() {
        return species.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {
        Bestiary.Species entry = species.get(absoluteIndex);
        int[] counts = bestiary.counts(player, entry.key());
        int total = Bestiary.total(counts);

        if (total == 0) {
            setItem(contentSlot, item(Material.GRAY_DYE, lang.component("bestiary.unknown_name"),
                    List.of(lang.component(entry.hostile() ? "bestiary.unknown_hostile" : "bestiary.unknown_other"))));
            return;
        }

        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("bestiary.kills", "amount", total));
        List<MobRarities.Tier> tiers = bestiary.rarities().tiers();
        for (int i = 0; i < tiers.size(); i++) {
            int n = i < counts.length ? counts[i] : 0;
            lore.add(lang.component(n > 0 ? "bestiary.tier_line" : "bestiary.tier_line_none", "rarity", tiers.get(i).name(),
                    "amount", n));
        }
        setItem(contentSlot, item(entry.icon(), entry.name().colorIfAbsent(NamedTextColor.WHITE), lore));
    }

    @Override
    protected void renderExtras() {
        int discovered = 0;
        int kills = 0;
        int best = 0;
        int last = bestiary.rarities().tiers().size() - 1;
        for (Bestiary.Species entry : species) {
            int[] counts = bestiary.counts(player, entry.key());
            int total = Bestiary.total(counts);
            if (total > 0) {
                discovered++;
            }
            kills += total;
            if (last > 0 && counts.length > last) {
                best += counts[last];
            }
        }
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("bestiary.summary_discovered", "amount", discovered, "total", species.size()));
        lore.add(lang.component("bestiary.summary_kills", "amount", kills));
        if (last > 0) {
            lore.add(lang.component("bestiary.summary_best", "rarity", bestiary.rarities().tiers().get(last).name(),
                    "amount", best));
        }
        lore.add(lang.component("bestiary.summary_hint"));
        setItem(SUMMARY, item(Material.WRITABLE_BOOK, lang.component("bestiary.summary_title"), lore));
        if (hasPreviousPage()) {
            setItem(PREVIOUS, item(Material.ARROW, lang.component("bestiary.previous"), List.of()));
        }
        if (hasNextPage()) {
            setItem(NEXT, item(Material.ARROW, lang.component("bestiary.next", "page", page + 2, "pages", maxPage() + 1),
                    List.of()));
        }
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {
        if (event.getSlot() == PREVIOUS) {
            previousPage();
        } else if (event.getSlot() == NEXT) {
            nextPage();
        }
    }
}
