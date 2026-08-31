package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.guilds.gui.PaginatedGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.GuildServices;
import com.sack.rpgroll.guilds.guild.Guild;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class GuildBrowserGUI extends PaginatedGUI {

    private static final int SIZE = 54;
    private static final int CONTENT_SLOTS = 45;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 52;
    private static final int CLOSE_SLOT = 53;

    private final GuildServices services;
    private final List<Guild> guilds;

    public GuildBrowserGUI(Player player, GuildServices services) {
        super(player, Component.text(services.langManager().raw("guild.browser.title"), NamedTextColor.GOLD), SIZE,
                CONTENT_SLOTS);
        this.services = services;
        this.guilds = services.guildManager().getAll().stream()
                .sorted((a, b) -> Integer.compare(b.level(), a.level()))
                .toList();
    }

    private com.sack.rpgroll.common.lang.LangManager lang() {
        return services.langManager();
    }

    @Override
    protected int totalItemCount() {
        return guilds.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        Guild guild = guilds.get(absoluteIndex);

        setItem(contentSlot, new ItemBuilder(guild.icon())
                .setName(Component.text(guild.name(), guild.color()))
                .setLore(ComponentUtils.parseWithDefault(lang().raw("guild.browser.level_members", "level", guild.level(),
                        "count", guild.memberCount()), NamedTextColor.GRAY),
                        Component.text(guild.motto(), NamedTextColor.DARK_GRAY),
                        ComponentUtils.parseWithDefault(lang().raw("guild.browser.click_open"), NamedTextColor.YELLOW))
                .build());
    }

    @Override
    protected void renderExtras() {

        setItem(PREV_SLOT, hasPreviousPage()
                ? new ItemBuilder(Material.ARROW).setName(ComponentUtils.parseWithDefault(lang().raw("common.previous_page"), NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        setItem(NEXT_SLOT, hasNextPage()
                ? new ItemBuilder(Material.ARROW).setName(ComponentUtils.parseWithDefault(lang().raw("common.next_page"), NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang().raw("common.close")));
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {

        Guild guild = guilds.get(absoluteIndex);

        if (!guild.isMember(player.getUniqueId())) {
            player.sendMessage(Component.text("=== " + guild.name() + " ===", guild.color()));
            lang().send(player, "guild.browser.level_members", "level", guild.level(), "count", guild.memberCount());
            player.sendMessage(Component.text(guild.description(), NamedTextColor.GRAY));
            return;
        }

        new GuildHubGUI(player, guild, services).open();
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        switch (event.getSlot()) {
            case PREV_SLOT -> previousPage();
            case NEXT_SLOT -> nextPage();
            case CLOSE_SLOT -> close();
            default -> {
            }
        }
    }

}
