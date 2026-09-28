package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.ownership.AnimalMarket;
import com.sack.rpgroll.ranching.core.ownership.OwnershipService;
import com.sack.rpgroll.ranching.core.species.Species;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "Mis animales": todos los del jugador, estén donde estén. Clic izquierdo los llama, derecho los
 * manda al corral, con mayúsculas se abre su ficha o se ponen/quitan de la venta. Abajo, las
 * acciones para todos a la vez, fijar el corral aquí y el mercado.
 */
public class MyAnimalsGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;
    private static final int PREV = 45;
    private static final int CALL_ALL = 47;
    private static final int PEN_ALL = 48;
    private static final int PEN = 49;
    private static final int MARKET = 50;
    private static final int NEXT = 53;

    private final OwnershipService ownership;
    private final ChatPromptManager prompts;
    private final LangManager lang;
    private List<Animal> animals;
    private int page;

    public MyAnimalsGUI(Player player, OwnershipService ownership, ChatPromptManager prompts) {
        super(player, ownership.lang().component("gui.my_animals.title"), SIZE);
        this.ownership = ownership;
        this.prompts = prompts;
        this.lang = ownership.lang();
        this.animals = ownership.animals().ownedBy(player.getUniqueId());
    }

    @Override
    public void build() {

        clear();
        animals = ownership.animals().ownedBy(player.getUniqueId());

        int pages = Math.max(1, (animals.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        for (int i = 0; i < PER_PAGE && page * PER_PAGE + i < animals.size(); i++) {
            setItem(i, icon(animals.get(page * PER_PAGE + i)));
        }

        if (animals.isEmpty()) {
            setItem(22, new ItemBuilder(Material.HAY_BLOCK).setName(lang.component("gui.my_animals.empty"))
                    .setLore(ItemBuilder.toLoreLines(lang.raw("gui.my_animals.empty_lore"))).build());
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

        setItem(CALL_ALL, new ItemBuilder(Material.GOAT_HORN).setName(lang.component("gui.my_animals.call_all")).build());
        setItem(PEN_ALL, new ItemBuilder(Material.OAK_FENCE_GATE).setName(lang.component("gui.my_animals.pen_all")).build());

        String pen = ownership.pens().get(player.getUniqueId())
                .map(l -> l.getWorld().getName() + " " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ())
                .orElse(lang.raw("gui.my_animals.pen_none"));
        setItem(PEN, new ItemBuilder(Material.OAK_FENCE).setName(lang.component("gui.my_animals.pen"))
                .setLore(ItemBuilder.toLoreLines(lang.raw("gui.my_animals.pen_lore", "pen", pen))).build());

        if (ownership.settings().marketEnabled()) {
            setItem(MARKET, new ItemBuilder(Material.EMERALD).setName(lang.component("gui.my_animals.market")).build());
        }
    }

    private ItemStack icon(Animal animal) {

        Species species = ownership.species().get(animal.speciesId()).orElse(null);
        Material material = species != null ? SpeciesBrowserGUI.parseMaterial(species.icon(), Material.COW_SPAWN_EGG)
                : Material.BARRIER;

        List<Component> lore = new ArrayList<>();
        lore.add(lang.component("gui.animal.sex_stage", "sex", animal.sex(), "stage", animal.stage()));
        lore.add(lang.component("gui.animal.quality", "quality", animal.quality()));
        lore.add(lang.component("gui.animal.health_happiness", "health", String.format(Locale.ROOT, "%.0f", animal.health()),
                "happiness", String.format(Locale.ROOT, "%.0f", animal.happiness())));
        lore.add(lang.component("gui.my_animals.where", "where", ownership.whereIs(animal, player)));

        if (animal.isForSale()) {
            lore.add(lang.component("gui.my_animals.for_sale", "price", AnimalMarket.format(animal.salePrice())));
        }

        lore.add(Component.empty());
        lore.addAll(ItemBuilder.toLoreLines(lang.raw("gui.my_animals.actions")));

        return new ItemBuilder(material).setName(lang.component("gui.my_animals.name", "animal", ownership.describe(animal)))
                .setLore(lore).build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= SIZE) {
            return;
        }

        if (slot < PER_PAGE) {
            int index = page * PER_PAGE + slot;
            if (index < animals.size()) {
                clickAnimal(animals.get(index), event.getClick());
            }
            return;
        }

        switch (slot) {
            case PREV -> {
                page--;
                build();
            }
            case NEXT -> {
                page++;
                build();
            }
            case CALL_ALL -> {
                close();
                ownership.callAll(player);
            }
            case PEN_ALL -> {
                close();
                ownership.sendAllToPen(player);
            }
            case PEN -> {
                ownership.pens().set(player.getUniqueId(), player.getLocation());
                lang.send(player, "owner.pen_set");
                build();
            }
            case MARKET -> {
                if (ownership.settings().marketEnabled()) {
                    new AnimalMarketGUI(player, ownership, prompts).open();
                }
            }
            default -> {
            }
        }
    }

    private void clickAnimal(Animal animal, ClickType click) {

        switch (click) {
            case LEFT -> {
                close();
                ownership.call(player, animal);
            }
            case RIGHT -> {
                close();
                ownership.sendToPen(player, animal);
            }
            case SHIFT_LEFT -> new AnimalDetailGUI(player, animal, ownership.species(), ownership.breeds(), prompts,
                    this::open).open();
            case SHIFT_RIGHT -> {
                if (!ownership.settings().marketEnabled()) {
                    return;
                }
                if (animal.isForSale()) {
                    ownership.list(player, animal, 0);
                    build();
                    return;
                }
                close();
                prompts.prompt(player, lang.raw("market.ask_price", "animal", ownership.describe(animal)), answer -> {
                    try {
                        ownership.list(player, animal, Double.parseDouble(answer.trim().replace(",", ".")));
                    } catch (NumberFormatException e) {
                        lang.send(player, "market.result.bad_price", "min", AnimalMarket.format(ownership.settings().minPrice()),
                                "maxprice", AnimalMarket.format(ownership.settings().maxPrice()));
                    }
                });
            }
            default -> {
            }
        }
    }

}
