package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.SocketDefinition;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.List;

/** Editor de ranuras de socket (para insertar gemas en tiempo de juego). */
public class SocketsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final EditorSession session;
    private final LangManager lang;
    private final Runnable onBack;

    public SocketsEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.sockets.title", "id", session.original.id()), SIZE);
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

        for (int slot = 36; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        for (int i = 0; i < session.sockets.size() && i < 36; i++) {

            SocketDefinition socket = session.sockets.get(i);

            setItem(i, new ItemBuilder(Material.AMETHYST_SHARD)
                    .setName(Component.text(socket.id(), NamedTextColor.AQUA))
                    .setLore(lang.component("editor.sockets.accepted_types",
                                    "types", socket.acceptedTypes().isEmpty()
                                            ? lang.raw("editor.sockets.any_type")
                                            : String.join(", ", socket.acceptedTypes())),
                            lang.component("editor.common.shift_click_remove"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.sockets.add"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.sockets.size() && slot < 36) {
            if (event.isShiftClick()) {
                session.sockets.remove(slot);
                build();
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptAdd();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAdd() {
        session.chatPromptManager.prompt(player, lang.raw("editor.sockets.prompt_add"), value -> {

            String[] parts = value.trim().split("\\s+", 2);

            if (parts.length < 1 || parts[0].isBlank()) {
                lang.send(player, "editor.common.invalid_format");
                return;
            }

            List<String> types = parts.length < 2 || parts[1].equalsIgnoreCase("cualquiera")
                    ? List.of()
                    : Arrays.stream(parts[1].split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

            session.sockets.add(new SocketDefinition(parts[0], types));
            build();
        });
    }

}
