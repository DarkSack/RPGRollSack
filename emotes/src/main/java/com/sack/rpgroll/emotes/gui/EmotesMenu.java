package com.sack.rpgroll.emotes.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.emotes.catalog.Emote;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.command.EmoteCommand;
import com.sack.rpgroll.emotes.player.EmotePlayer;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import com.sack.rpgroll.emotes.service.EmoteService;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Menú {@code /emotes}: pestañas de categoría, 28 emotes por página, favoritos y «Parar emote».
 * Clic izquierdo lanza y cierra; clic derecho pone o quita de favoritos. Sin tareas repetidas:
 * solo se redibuja al hacer clic.
 */
public class EmotesMenu extends InventoryGUI {

    /** Fondo dibujado (sackito:menus/emotes_menu). */
    public static final String BACKGROUND = "emotes_menu";

    private final EmoteService service;
    private final EmoteCatalog catalog;
    private final EmotePlayerStore store;
    private final EmoteCommand command;
    private final LangManager lang;

    /** null = «Todos». */
    private String category;
    private int page;
    private List<String> tabs = List.of();
    private List<Emote> shown = List.of();

    public EmotesMenu(Player player, EmoteService service, EmoteCatalog catalog, EmotePlayerStore store,
                      EmoteCommand command, LangManager lang) {
        super(player, EmotesMenus.skin().title(BACKGROUND, lang.component("menu.title")), MenuLayout.SIZE);
        this.service = service;
        this.catalog = catalog;
        this.store = store;
        this.command = command;
        this.lang = lang;
    }

    @Override
    public void build() {
        clear();
        for (int slot = 0; slot < MenuLayout.SIZE; slot++) {
            setItem(slot, EmotesMenus.filler());
        }
        EmotePlayer data = store.get(player.getUniqueId());

        // Contenido: los ocultos que no tiene quedan fuera; los bloqueados se ven en gris.
        List<Emote> visible = catalog.visibleTo(e -> service.owns(player, data, e));

        // Pestañas: «Todos» y las categorías que quepan en la fila 0 (las que sobren no se muestran).
        // Salen de los visibles: una categoría de solo ocultos (Magia) no deja una pestaña vacía.
        List<String> categories = EmoteCatalog.categories(visible);
        tabs = categories.subList(0, Math.min(categories.size(), MenuLayout.CATEGORY_SLOTS.size() - 1));
        if (category != null && !tabs.contains(category)) {
            category = null;
        }
        setItem(MenuLayout.CATEGORY_SLOTS.get(0),
                tab(Material.NETHER_STAR, lang.component("menu.all"), category == null));
        for (int i = 0; i < tabs.size(); i++) {
            setItem(MenuLayout.CATEGORY_SLOTS.get(i + 1),
                    tab(Material.BOOK, Component.text(capitalize(tabs.get(i))), tabs.get(i).equals(category)));
        }

        shown = visible.stream()
                .filter(e -> category == null || e.category().equals(category)).toList();
        int pages = MenuLayout.pages(shown.size(), MenuLayout.PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        List<Emote> onPage = MenuLayout.page(shown, page, MenuLayout.PER_PAGE);
        for (int i = 0; i < onPage.size(); i++) {
            setItem(MenuLayout.CONTENT_SLOTS.get(i), emoteItem(onPage.get(i), data));
        }

        // Favoritos: su emote o un hueco vacío.
        List<String> favs = data.favorites();
        for (int i = 0; i < MenuLayout.FAVORITE_SLOTS.size() && i < favs.size(); i++) {
            Optional<Emote> fav = catalog.get(favs.get(i));
            setItem(MenuLayout.FAVORITE_SLOTS.get(i), fav.isPresent()
                    ? favoriteItem(fav.get(), i + 1)
                    : EmotesMenus.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                            lang.component("menu.favorite_empty", "slot", i + 1), List.of()));
        }

        List<Component> pageLore = List.of(lang.component("menu.page", "page", page + 1, "pages", pages));
        setItem(MenuLayout.PREV, EmotesMenus.skin().button(
                EmotesMenus.item(Material.ARROW, lang.component("menu.prev"), pageLore)));
        setItem(MenuLayout.NEXT, EmotesMenus.skin().button(
                EmotesMenus.item(Material.ARROW, lang.component("menu.next"), pageLore)));
        setItem(MenuLayout.STOP, EmotesMenus.skin().button(
                EmotesMenus.item(Material.BARRIER, lang.component("menu.stop"), List.of())));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        EmotePlayer data = store.get(player.getUniqueId());

        int tab = MenuLayout.CATEGORY_SLOTS.indexOf(slot);
        if (tab >= 0) {
            if (tab <= tabs.size()) {
                category = tab == 0 ? null : tabs.get(tab - 1);
                page = 0;
                build();
            }
            return;
        }
        if (slot == MenuLayout.PREV || slot == MenuLayout.NEXT) {
            page += slot == MenuLayout.NEXT ? 1 : -1;
            build();
            return;
        }
        if (slot == MenuLayout.STOP) {
            service.stop(player);
            close();
            return;
        }

        int favIndex = MenuLayout.FAVORITE_SLOTS.indexOf(slot);
        if (favIndex >= 0) {
            String id = data.favorite(favIndex + 1);
            if (id == null || catalog.get(id).isEmpty()) {
                return;
            }
            switch (MenuLayout.action(click)) {
                case TOGGLE_FAVORITE -> {
                    data.toggleFavorite(id);
                    build();
                }
                case LAUNCH -> launch(id);
                case NONE -> { }
            }
            return;
        }

        int index = MenuLayout.CONTENT_SLOTS.indexOf(slot);
        List<Emote> onPage = MenuLayout.page(shown, page, MenuLayout.PER_PAGE);
        if (index < 0 || index >= onPage.size()) {
            return;
        }
        Emote emote = onPage.get(index);
        if (!service.owns(player, data, emote)) {
            return;
        }
        switch (MenuLayout.action(click)) {
            case TOGGLE_FAVORITE -> {
                if (!data.toggleFavorite(emote.id())) {
                    player.sendActionBar(lang.component("menu.favorites_full"));
                }
                build();
            }
            case LAUNCH -> launch(emote.id());
            case NONE -> { }
        }
    }

    /** Cierra y lanza por el mismo camino que {@code /emote} (mensajes de error por la barra de acción). */
    private void launch(String id) {
        close();
        command.run(player, id);
    }

    private ItemStack tab(Material material, Component name, boolean selected) {
        ItemStack item = EmotesMenus.item(material, name, List.of());
        if (selected) {
            item.editMeta(m -> m.setEnchantmentGlintOverride(true));
        }
        return item;
    }

    private ItemStack emoteItem(Emote e, EmotePlayer data) {
        List<Component> lore = new ArrayList<>();
        if (!service.owns(player, data, e)) {
            lore.add(lang.component("menu.locked"));
            if (e.howToUnlock() != null && !e.howToUnlock().isBlank()) {
                lore.add(lang.component("menu.how_to_unlock", "how", e.howToUnlock()));
            }
            return EmotesMenus.item(Material.GRAY_DYE, Component.text(plainName(e), NamedTextColor.GRAY), lore);
        }
        boolean favorite = data.favorites().contains(e.id());
        lore.add(lang.component("menu.rarity", "rarity", rarityName(lang, e.rarity())));
        lore.add(lang.component("menu.hint_play"));
        lore.add(lang.component(favorite ? "menu.hint_unfavorite" : "menu.hint_favorite"));
        ItemStack item = EmotesMenus.item(icon(e), name(e), lore);
        if (favorite) {
            item.editMeta(m -> m.setEnchantmentGlintOverride(true));
        }
        return item;
    }

    private ItemStack favoriteItem(Emote e, int slot) {
        return EmotesMenus.item(icon(e), lang.component("menu.favorite", "slot", slot, "name", plainName(e)),
                List.of(lang.component("menu.hint_play"), lang.component("menu.hint_unfavorite")));
    }

    /** El icono si es un material de ítem; si no (p. ej. cabeza por textura), papel. */
    private static Material icon(Emote e) {
        Material m = Material.matchMaterial(e.icon() == null ? "" : e.icon());
        return m == null || m.isAir() || !m.isItem() ? Material.PAPER : m;
    }

    private Component name(Emote e) {
        return e.name().startsWith("lang:") ? lang.component(e.name().substring(5)) : ComponentUtils.parse(e.name());
    }

    private String plainName(Emote e) {
        return PlainTextComponentSerializer.plainText().serialize(name(e));
    }

    /** La rareza en el idioma activo ({@code rarity.<id>}); sin traducción, el id con mayúscula. */
    static String rarityName(LangManager lang, String rarity) {
        String key = "rarity." + rarity;
        String name = lang.raw(key);
        return name.equals(key) ? capitalize(rarity) : name;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
