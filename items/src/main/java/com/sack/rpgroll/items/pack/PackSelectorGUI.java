package com.sack.rpgroll.items.pack;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.gui.ChatPromptManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Se muestra antes de crear un ítem nuevo: si no hay ningún pack todavía,
 * pide crear uno; si ya hay, los lista para elegir uno existente o crear
 * otro. El resultado (nombre del pack elegido/creado) se entrega vía
 * {@code onSelected} — este GUI no sabe nada de {@link com.sack.rpgroll.items.core.ItemDefinition},
 * solo resuelve "en qué pack va este ítem nuevo".
 */
public class PackSelectorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int CANCEL_SLOT = 44;
    private static final int MAX_PACKS = 36;

    private final PackManager packManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Consumer<String> onSelected;

    private List<String> packs;

    public PackSelectorGUI(Player player, PackManager packManager, ChatPromptManager chatPromptManager,
            Consumer<String> onSelected) {

        super(player, chatPromptManager.lang().component("pack_selector.title"), SIZE);

        this.packManager = packManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onSelected = onSelected;
        this.packs = packManager.list();
    }

    @Override
    public void build() {

        clear();

        if (packs.isEmpty()) {
            lang.send(player, "pack_selector.empty_hint");
        }

        for (int i = 0; i < packs.size() && i < MAX_PACKS; i++) {

            String pack = packs.get(i);

            setItem(i, new ItemBuilder(Material.CHEST)
                    .setName(Component.text(pack, NamedTextColor.YELLOW))
                    .setLore(lang.component("pack_selector.click_to_use"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("pack_selector.create_new"))
                .setLore(lang.component("pack_selector.create_new_lore"))
                .build());

        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton(lang.raw("pack_selector.cancel_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < packs.size() && slot < MAX_PACKS) {
            close();
            onSelected.accept(packs.get(slot));
            return;
        }

        if (slot == NEW_SLOT) {
            promptNewPack();
            return;
        }

        if (slot == CANCEL_SLOT) {
            close();
        }
    }

    private void promptNewPack() {
        chatPromptManager.prompt(player, lang.raw("pack_selector.prompt_new"), value -> {

            String name = value.trim().toLowerCase().replace(' ', '_');

            if (!packManager.isValidName(name)) {
                lang.send(player, "pack_selector.invalid_name");
                reopen();
                return;
            }

            if (packManager.exists(name)) {
                lang.send(player, "pack_selector.already_exists");
                reopen();
                return;
            }

            packManager.create(name);
            close();
            onSelected.accept(name);
        });
    }

    private void reopen() {
        this.packs = packManager.list();
        open();
    }

}
