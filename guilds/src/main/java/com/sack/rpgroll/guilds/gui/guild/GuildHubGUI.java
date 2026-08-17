package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.GuildServices;
import com.sack.rpgroll.guilds.guild.Guild;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Panel de navegación principal de una guild — botón por componente, igual patrón que el Dungeon Studio. */
public class GuildHubGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int MEMBERS_SLOT = 10;
    private static final int VAULT_SLOT = 11;
    private static final int TERRITORY_SLOT = 12;
    private static final int UPGRADES_SLOT = 13;
    private static final int DIPLOMACY_SLOT = 14;
    private static final int QUESTS_SLOT = 15;
    private static final int ACHIEVEMENTS_SLOT = 16;
    private static final int CALENDAR_SLOT = 20;
    private static final int CUSTOMIZATION_SLOT = 21;
    private static final int DISBAND_SLOT = 40;
    private static final int CLOSE_SLOT = 44;

    private final Guild guild;
    private final GuildServices services;

    public GuildHubGUI(Player player, Guild guild, GuildServices services) {
        super(player, Component.text(guild.name(), NamedTextColor.GOLD), SIZE);
        this.guild = guild;
        this.services = services;
    }

    private com.sack.rpgroll.common.lang.LangManager lang() {
        return services.langManager();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(4, new ItemBuilder(guild.icon())
                .setName(Component.text(guild.name() + " — " + lang().raw("guild.hub.level_suffix", "level",
                        guild.level()), guild.color()))
                .setLore(Component.text(lang().raw("guild.hub.member_count", "count", guild.memberCount()),
                        NamedTextColor.GRAY),
                        Component.text(guild.motto(), NamedTextColor.GRAY))
                .build());

        setItem(MEMBERS_SLOT, button(Material.PLAYER_HEAD, lang().raw("guild.hub.button.members"), NamedTextColor.YELLOW));
        setItem(VAULT_SLOT, button(Material.CHEST, lang().raw("guild.hub.button.vault"), NamedTextColor.GOLD));
        setItem(TERRITORY_SLOT, button(Material.GRASS_BLOCK, lang().raw("guild.hub.button.territory"),
                NamedTextColor.GREEN));
        setItem(UPGRADES_SLOT, button(Material.NETHER_STAR, lang().raw("guild.hub.button.upgrades"),
                NamedTextColor.LIGHT_PURPLE));
        setItem(DIPLOMACY_SLOT, button(Material.WHITE_BANNER, lang().raw("guild.hub.button.diplomacy"),
                NamedTextColor.AQUA));
        setItem(QUESTS_SLOT, button(Material.MAP, lang().raw("guild.hub.button.quests"), NamedTextColor.YELLOW));
        setItem(ACHIEVEMENTS_SLOT, button(Material.GOLD_INGOT, lang().raw("guild.hub.button.achievements"),
                NamedTextColor.GOLD));
        setItem(CALENDAR_SLOT, button(Material.CLOCK, lang().raw("guild.hub.button.calendar"), NamedTextColor.AQUA));
        setItem(CUSTOMIZATION_SLOT, button(Material.NAME_TAG, lang().raw("guild.hub.button.customization"),
                NamedTextColor.WHITE));

        setItem(DISBAND_SLOT, new ItemBuilder(Material.REDSTONE_BLOCK)
                .setName(Component.text(lang().raw("guild.hub.button.disband_leave"), NamedTextColor.RED))
                .build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang().raw("common.close")));
    }

    private org.bukkit.inventory.ItemStack button(Material material, String name, NamedTextColor color) {
        return new ItemBuilder(material).setName(Component.text(name, color)).build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        switch (event.getSlot()) {
            case MEMBERS_SLOT -> new GuildMembersGUI(player, guild, services.guildManager(),
                    services.chatPromptManager(), this::reopen).open();
            case VAULT_SLOT -> new GuildVaultGUI(player, guild, services.guildManager(),
                    services.chatPromptManager(), this::reopen).open();
            case TERRITORY_SLOT -> new GuildTerritoryGUI(player, guild, services.guildManager(),
                    services.chatPromptManager(), this::reopen).open();
            case UPGRADES_SLOT -> new GuildUpgradeTreeGUI(player, guild, services.guildManager(), this::reopen).open();
            case DIPLOMACY_SLOT -> new GuildDiplomacyGUI(player, guild, services.guildManager(),
                    services.diplomacyManager(), this::reopen).open();
            case QUESTS_SLOT -> new GuildQuestsGUI(player, guild, services.guildManager(), services.questManager(),
                    services.questService(), this::reopen).open();
            case ACHIEVEMENTS_SLOT -> new GuildAchievementsGUI(player, guild, this::reopen).open();
            case CALENDAR_SLOT -> new GuildCalendarGUI(player, guild, services.guildManager(),
                    services.chatPromptManager(), this::reopen).open();
            case CUSTOMIZATION_SLOT -> new GuildCustomizationGUI(player, guild, services.guildManager(),
                    services.chatPromptManager(), this::reopen).open();
            case DISBAND_SLOT -> handleDisbandOrLeave();
            case CLOSE_SLOT -> close();
            default -> {
            }
        }
    }

    private void handleDisbandOrLeave() {

        if (guild.roleOf(player.getUniqueId()) == com.sack.rpgroll.guilds.guild.GuildRole.LEADER
                && guild.memberCount() > 1) {
            lang().send(player, "guild.hub.disband_is_leader");
            return;
        }

        if (guild.memberCount() <= 1) {
            services.guildManager().disband(guild.id());
            lang().send(player, "guild.disband.success");
        } else {
            guild.removeMember(player.getUniqueId());
            services.guildManager().save(guild);
            lang().send(player, "guild.leave.success");
        }

        close();
    }

    private void reopen() {
        new GuildHubGUI(player, guild, services).open();
    }

}
