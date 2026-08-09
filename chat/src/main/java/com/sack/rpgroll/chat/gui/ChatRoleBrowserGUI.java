package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.role.ChatRole;
import com.sack.rpgroll.chat.role.ChatRoleManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class ChatRoleBrowserGUI extends PaginatedGUI {

    private static final int SIZE = 45;
    private static final int CONTENT_SLOTS = 36;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ChatRoleManager roleManager;
    private final ChatPromptManager chatPromptManager;
    private List<ChatRole> roles;

    public ChatRoleBrowserGUI(Player player, ChatRoleManager roleManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Roles de Chat RPGRoll", NamedTextColor.GOLD), SIZE, CONTENT_SLOTS);
        this.roleManager = roleManager;
        this.chatPromptManager = chatPromptManager;
        this.roles = List.copyOf(roleManager.getAll());
    }

    @Override
    protected int totalItemCount() {
        return roles.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        ChatRole role = roles.get(absoluteIndex);

        setItem(contentSlot, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text(role.id(), NamedTextColor.YELLOW))
                .setLore(Component.text("Prioridad: " + role.priority(), NamedTextColor.GRAY),
                        Component.text("Permiso: " + (role.permission() == null || role.permission().isBlank()
                                ? "(ninguno)" : role.permission()), NamedTextColor.GRAY),
                        Component.text("Click para editar", NamedTextColor.YELLOW))
                .build());
    }

    @Override
    protected void renderExtras() {

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear rol nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {
        new ChatRoleEditorGUI(player, roles.get(absoluteIndex), roleManager, chatPromptManager, this::reopen).open();
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        switch (event.getSlot()) {
            case NEW_SLOT -> promptNew();
            case BACK_SLOT -> close();
            default -> {
            }
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id del nuevo rol:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (roleManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un rol con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            ChatRole role = new ChatRole(id, "", "", "WHITE", "", 0, "");
            roleManager.save(role);
            reopen();
        });
    }

    private void reopen() {
        this.roles = List.copyOf(roleManager.getAll());
        open();
    }

}
