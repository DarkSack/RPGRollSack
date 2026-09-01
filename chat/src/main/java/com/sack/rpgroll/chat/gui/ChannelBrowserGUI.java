package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.common.lang.LangManager;

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
        super(player, chatPromptManager.lang().component("channel.browser_title"), SIZE, CONTENT_SLOTS);
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

        LangManager lang = chatPromptManager.lang();

        setItem(contentSlot, new ItemBuilder(resolveIcon(channel))
                .setName(ComponentUtils.parseWithDefault(channel.displayName(), resolveColor(channel)))
                .setLore(Component.text(channel.id(), NamedTextColor.DARK_GRAY),
                        lang.component("channel.browser_lore_scope", "scope", channel.scope(), "priority",
                                channel.priority()).colorIfAbsent(NamedTextColor.GRAY),
                        lang.component("gui.click_edit").colorIfAbsent(NamedTextColor.YELLOW))
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

        LangManager lang = chatPromptManager.lang();

        setItem(PREV_SLOT, hasPreviousPage()
                ? new ItemBuilder(Material.ARROW)
                        .setName(lang.component("channel.prev_page").colorIfAbsent(NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        setItem(NEXT_SLOT, hasNextPage()
                ? new ItemBuilder(Material.ARROW)
                        .setName(lang.component("channel.next_page").colorIfAbsent(NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("channel.create_new").colorIfAbsent(NamedTextColor.GREEN))
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("channel.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (channelManager.exists(id)) {
                chatPromptManager.lang().send(player, "channel.already_exists");
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
        open();
    }

}
