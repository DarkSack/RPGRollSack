package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.ownership.AnimalMarket;
import com.sack.rpgroll.ranching.core.ownership.OwnershipService;
import com.sack.rpgroll.ranching.core.ownership.OwnershipSettings;
import com.sack.rpgroll.ranching.core.species.Species;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * El mercado de animales, en tres pestañas: lo que venden otros jugadores, lo que vende el servidor
 * ({@code market.server-shop}) y vender los tuyos al servidor. Todo pasa por una confirmación.
 */
public class AnimalMarketGUI extends InventoryGUI {

    private enum Tab {
        PLAYERS, SERVER, SELL
    }

    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;
    private static final int PREV = 45;
    private static final int TAB_PLAYERS = 47;
    private static final int TAB_SERVER = 48;
    private static final int TAB_SELL = 49;
    private static final int MY_ANIMALS = 51;
    private static final int NEXT = 53;

    private final OwnershipService ownership;
    private final ChatPromptManager prompts;
    private final LangManager lang;
    private Tab tab = Tab.PLAYERS;
    private int page;
    private List<Runnable> actions = List.of();

    public AnimalMarketGUI(Player player, OwnershipService ownership, ChatPromptManager prompts) {
        super(player, ownership.lang().component("gui.market.title"), SIZE);
        this.ownership = ownership;
        this.prompts = prompts;
        this.lang = ownership.lang();
    }

    @Override
    public void build() {

        clear();
        List<ItemStack> icons = new ArrayList<>();
        List<Runnable> clicks = new ArrayList<>();

        switch (tab) {
            case PLAYERS -> {
                for (Animal animal : ownership.animals().forSale()) {
                    icons.add(listingIcon(animal));
                    clicks.add(() -> confirm(listingIcon(animal), () -> ownership.buy(player, animal)));
                }
            }
            case SERVER -> {
                for (OwnershipSettings.ServerOffer offer : ownership.settings().serverShop()) {
                    icons.add(offerIcon(offer));
                    clicks.add(() -> confirm(offerIcon(offer), () -> ownership.buyFromServer(player, offer)));
                }
            }
            case SELL -> {
                for (Animal animal : ownership.animals().ownedBy(player.getUniqueId())) {
                    if (ownership.market().serverPrice(animal) > 0) {
                        icons.add(sellIcon(animal));
                        clicks.add(() -> confirm(sellIcon(animal), () -> ownership.sellToServer(player, animal)));
                    }
                }
            }
        }

        int pages = Math.max(1, (icons.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        List<Runnable> pageActions = new ArrayList<>();

        for (int i = 0; i < PER_PAGE && page * PER_PAGE + i < icons.size(); i++) {
            setItem(i, icons.get(page * PER_PAGE + i));
            pageActions.add(clicks.get(page * PER_PAGE + i));
        }
        actions = pageActions;

        if (icons.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER).setName(lang.component("gui.market.empty_"
                    + tab.name().toLowerCase(Locale.ROOT))).build());
        }

        for (int slot = PER_PAGE; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        if (page > 0) {
            setItem(PREV, new ItemBuilder(Material.ARROW).setName(lang.component("gui.common.prev_page")).build());
        }
        if (page < pages - 1) {
            setItem(NEXT, new ItemBuilder(Material.ARROW).setName(lang.component("gui.common.next_page")).build());
        }

        setItem(TAB_PLAYERS, tabIcon(Tab.PLAYERS, Material.PLAYER_HEAD, "gui.market.tab_players"));
        setItem(TAB_SERVER, tabIcon(Tab.SERVER, Material.CHEST, "gui.market.tab_server"));
        setItem(TAB_SELL, tabIcon(Tab.SELL, Material.GOLD_INGOT, "gui.market.tab_sell"));
        setItem(MY_ANIMALS, new ItemBuilder(Material.LEAD).setName(lang.component("gui.market.my_animals")).build());
    }

    private ItemStack tabIcon(Tab which, Material material, String key) {
        Component name = lang.component(key);
        return new ItemBuilder(tab == which ? Material.LIME_STAINED_GLASS_PANE : material)
                .setName(tab == which ? lang.component("gui.market.tab_selected", "tab", lang.raw(key)) : name).build();
    }

    private Material iconOf(String speciesId) {
        return ownership.species().get(speciesId).map(Species::icon)
                .map(icon -> SpeciesBrowserGUI.parseMaterial(icon, Material.COW_SPAWN_EGG)).orElse(Material.BARRIER);
    }

    private List<Component> stats(Animal animal) {
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("gui.animal.sex_stage", "sex", animal.sex(), "stage", animal.stage()));
        lore.add(lang.component("gui.animal.quality", "quality", animal.quality()));
        lore.add(lang.component("gui.animal.generation", "gen", animal.generation()));
        lore.add(lang.component("gui.animal.health_happiness", "health", String.format(Locale.ROOT, "%.0f", animal.health()),
                "happiness", String.format(Locale.ROOT, "%.0f", animal.happiness())));
        return lore;
    }

    private ItemStack listingIcon(Animal animal) {
        List<Component> lore = stats(animal);
        lore.add(lang.component("gui.market.seller", "owner", ownership.ownerName(animal)));
        lore.add(lang.component("gui.my_animals.where", "where", ownership.whereIs(animal, player)));
        lore.add(lang.component("gui.market.price", "price", AnimalMarket.format(animal.salePrice())));
        lore.add(lang.component(animal.isOwnedBy(player.getUniqueId()) ? "gui.market.yours" : "gui.market.click_buy"));
        return new ItemBuilder(iconOf(animal.speciesId()))
                .setName(lang.component("gui.my_animals.name", "animal", ownership.describe(animal))).setLore(lore).build();
    }

    private ItemStack offerIcon(OwnershipSettings.ServerOffer offer) {
        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("gui.market.offer_sex", "sex", offer.sex() == null ? lang.raw("gui.market.random_sex")
                : offer.sex()));
        lore.add(lang.component("gui.market.price", "price", AnimalMarket.format(offer.price())));
        lore.add(lang.component("gui.market.click_buy"));
        return new ItemBuilder(iconOf(offer.speciesId()))
                .setName(lang.component("gui.my_animals.name", "animal", ownership.offerName(offer))).setLore(lore).build();
    }

    private ItemStack sellIcon(Animal animal) {
        List<Component> lore = stats(animal);
        lore.add(lang.component("gui.market.server_pays", "price", AnimalMarket.format(ownership.market().serverPrice(animal))));
        lore.add(lang.component("gui.market.click_sell"));
        return new ItemBuilder(iconOf(animal.speciesId()))
                .setName(lang.component("gui.my_animals.name", "animal", ownership.describe(animal))).setLore(lore).build();
    }

    private void confirm(ItemStack subject, Runnable action) {
        new ConfirmGUI(player, lang, subject, () -> {
            action.run();
            open();
        }, this::open).open();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot >= 0 && slot < PER_PAGE) {
            if (slot < actions.size()) {
                actions.get(slot).run();
            }
            return;
        }

        switch (slot) {
            case PREV -> page--;
            case NEXT -> page++;
            case TAB_PLAYERS -> switchTo(Tab.PLAYERS);
            case TAB_SERVER -> switchTo(Tab.SERVER);
            case TAB_SELL -> switchTo(Tab.SELL);
            case MY_ANIMALS -> {
                new MyAnimalsGUI(player, ownership, prompts).open();
                return;
            }
            default -> {
                return;
            }
        }

        build();
    }

    private void switchTo(Tab next) {
        tab = next;
        page = 0;
    }

}
