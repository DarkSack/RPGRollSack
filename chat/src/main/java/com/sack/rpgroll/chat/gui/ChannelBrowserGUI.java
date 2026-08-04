package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.channel.ChatChannel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/** Navegador de canales — click abre el editor completo. */
public class ChannelBrowserGUI extends PaginatedGUI {

    private static final int SIZE = 54;
    private static final int CONTENT_SLOTS = 45;
    private static final int NEW_SLOT = 49;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final ChannelManager channelManager;
    private final ChatPromptManager chatPromptManager;
    private List<ChatChannel> channels;

    public ChannelBrowserGUI(Player player, ChannelManager channelManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Canales RPGRoll", NamedTextColor.GOLD), SIZE, CONTENT_SLOTS);
        this.channelManager = channelManager;
        this.chatPromptManager = chatPromptManager;
        this.channels = channelManager.sortedByPriority();
    }

    @Override
    protected int totalItemCount() {
        return channels.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        ChatChannel channel = channels.get(absoluteIndex);

        setItem(contentSlot, new ItemBuilder(resolveIcon(channel))
                .setName(Component.text(channel.displayName(), resolveColor(channel)))
                .setLore(Component.text(channel.id(), NamedTextColor.DARK_GRAY),
                        Component.text("Alcance: " + channel.scope() + " · Prioridad: " + channel.priority(),
                                NamedTextColor.GRAY),
                        Component.text("Click para editar", NamedTextColor.YELLOW))
                .build());
    }

    private NamedTextColor resolveColor(ChatChannel channel) {
        NamedTextColor color = NamedTextColor.NAMES.value(channel.color().toLowerCase(Locale.ROOT));
        return color != null ? color : NamedTextColor.WHITE;
    }

    private Material resolveIcon(ChatChannel channel) {
        try {
            return Material.valueOf(channel.icon().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }

    @Override
    protected void renderExtras() {

        setItem(PREV_SLOT, hasPreviousPage()
                ? new ItemBuilder(Material.ARROW).setName(Component.text("« Anterior", NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        setItem(NEXT_SLOT, hasNextPage()
                ? new ItemBuilder(Material.ARROW).setName(Component.text("Siguiente »", NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear canal nuevo", NamedTextColor.GREEN))
                .build());
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {
        new ChannelEditorGUI(player, channels.get(absoluteIndex), channelManager, chatPromptManager, this::reopen)
                .open();
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        switch (event.getSlot()) {
            case PREV_SLOT -> previousPage();
            case NEXT_SLOT -> nextPage();
            case NEW_SLOT -> promptNewChannel();
            default -> {
            }
        }
    }

    private void promptNewChannel() {
        chatPromptManager.prompt(player, "Escribí el id del nuevo canal:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (channelManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un canal con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            ChatChannel channel = new ChatChannel(id, id, "PAPER", "WHITE", 0,
                    com.sack.rpgroll.chat.channel.ChannelScope.GLOBAL, 0, "", "", 0,
                    "&7[{channel}] &f{player}&7: &f{message}", com.sack.rpgroll.chat.channel.ChatTextFormat.LEGACY,
                    "", true, true, false, true, true, false);

            channelManager.save(channel);
            reopen();
        });
    }

    private void reopen() {
        this.channels = channelManager.sortedByPriority();
        build();
    }

}
