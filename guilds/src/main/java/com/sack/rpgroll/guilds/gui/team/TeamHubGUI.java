package com.sack.rpgroll.guilds.gui.team;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamBuff;
import com.sack.rpgroll.guilds.team.TeamManager;
import com.sack.rpgroll.guilds.team.TeamRole;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.UUID;

/** Panel único de gestión de equipo: miembros/roles, configuración, buffs, y compartir XP/loot/oro. */
public class TeamHubGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int MEMBERS_START = 0;
    private static final int MEMBERS_END = 8;

    private static final int INVITE_SLOT = 10;
    private static final int NAME_SLOT = 11;
    private static final int COLOR_SLOT = 12;
    private static final int MAX_PLAYERS_SLOT = 13;
    private static final int MIN_LEVEL_SLOT = 14;

    private static final int SHARE_XP_SLOT = 19;
    private static final int SHARE_LOOT_SLOT = 20;
    private static final int SHARE_GOLD_SLOT = 21;

    private static final int BUFF_XP_SLOT = 28;
    private static final int BUFF_DAMAGE_SLOT = 29;
    private static final int BUFF_REGEN_SLOT = 30;
    private static final int BUFF_SPEED_SLOT = 31;

    private static final int LEAVE_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    private final TeamManager teamManager;
    private final ChatPromptManager chatPromptManager;
    private final List<UUID> memberOrder;

    public TeamHubGUI(Player player, TeamManager teamManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text(chatPromptManager.lang().raw("team.hub.title"), NamedTextColor.AQUA), SIZE);
        this.teamManager = teamManager;
        this.chatPromptManager = chatPromptManager;
        this.memberOrder = team() != null ? List.copyOf(team().members()) : List.of();
    }

    private com.sack.rpgroll.common.lang.LangManager lang() {
        return chatPromptManager.lang();
    }

    private Team team() {
        return teamManager.getTeam(player.getUniqueId()).orElse(null);
    }

    private TeamRole myRole() {
        Team team = team();
        return team != null ? team.roleOf(player.getUniqueId()) : TeamRole.GUEST;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        Team team = team();

        if (team == null) {
            setItem(22, new ItemBuilder(Material.BARRIER)
                    .setName(Component.text(lang().raw("team.hub.not_in_team"), NamedTextColor.RED))
                    .build());
            setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang().raw("common.close")));
            return;
        }

        for (int i = 0; i < memberOrder.size() && i <= MEMBERS_END - MEMBERS_START; i++) {

            UUID memberId = memberOrder.get(i);
            var offline = Bukkit.getOfflinePlayer(memberId);
            TeamRole role = team.roleOf(memberId);

            setItem(MEMBERS_START + i, ItemBuilder.skull(null)
                    .setName(Component.text((offline.getName() != null ? offline.getName() : memberId.toString())
                            + " — " + role, role == TeamRole.LEADER ? NamedTextColor.GOLD : NamedTextColor.YELLOW))
                    .setLore(Component.text(lang().raw("team.hub.lore.change_role"), NamedTextColor.GRAY),
                            Component.text(lang().raw("team.hub.lore.kick"), NamedTextColor.GRAY))
                    .build());
        }

        setItem(INVITE_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(Component.text(lang().raw("team.hub.button.invite"), NamedTextColor.GREEN))
                .build());

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text(lang().raw("team.hub.name_label", "name", team.name()), NamedTextColor.YELLOW))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.WHITE_DYE)
                .setName(Component.text(lang().raw("team.hub.color_label", "color", team.color()), NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("team.hub.lore.click_to_change"), NamedTextColor.GRAY))
                .build());

        setItem(MAX_PLAYERS_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(Component.text(lang().raw("team.hub.max_players_label", "amount", team.maxPlayers()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("team.hub.lore.plus_minus_one"), NamedTextColor.GRAY))
                .build());

        setItem(MIN_LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text(lang().raw("team.hub.min_level_label", "level", team.minLevel()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("team.hub.lore.plus_minus_one"), NamedTextColor.GRAY))
                .build());

        setItem(SHARE_XP_SLOT, toggleItem(lang().raw("team.hub.share_xp"), team.shareXp()));
        setItem(SHARE_LOOT_SLOT, toggleItem(lang().raw("team.hub.share_loot"), team.shareLoot()));
        setItem(SHARE_GOLD_SLOT, toggleItem(lang().raw("team.hub.share_gold"), team.shareGold()));

        setItem(BUFF_XP_SLOT, buffItem(team, TeamBuff.XP_BOOST));
        setItem(BUFF_DAMAGE_SLOT, buffItem(team, TeamBuff.DAMAGE_BOOST));
        setItem(BUFF_REGEN_SLOT, buffItem(team, TeamBuff.REGEN_BOOST));
        setItem(BUFF_SPEED_SLOT, buffItem(team, TeamBuff.SPEED_BOOST));

        setItem(LEAVE_SLOT, new ItemBuilder(Material.REDSTONE_BLOCK)
                .setName(Component.text(lang().raw(team.size() <= 1 ? "team.hub.button.disband" : "team.hub.button.leave"),
                        NamedTextColor.RED))
                .build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang().raw("common.close")));
    }

    private org.bukkit.inventory.ItemStack toggleItem(String label, boolean enabled) {
        return new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(Component.text(label + ": " + (enabled ? lang().raw("common.yes") : lang().raw("common.no")),
                        enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .setLore(Component.text(lang().raw("team.hub.lore.click_to_toggle"), NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        Team team = team();

        if (team == null) {
            if (event.getSlot() == CLOSE_SLOT) {
                close();
            }
            return;
        }

        int slot = event.getSlot();

        if (slot >= MEMBERS_START && slot <= MEMBERS_END && slot - MEMBERS_START < memberOrder.size()) {
            handleMemberClick(event, team, memberOrder.get(slot - MEMBERS_START));
            return;
        }

        if (slot == INVITE_SLOT) {
            if (!myRole().canInvite()) {
                lang().send(player, "team.hub.no_permission_invite");
                return;
            }
            chatPromptManager.prompt(player, "team.hub.prompt_invite", value -> {
                Player target = Bukkit.getPlayerExact(value.trim());
                if (target == null) {
                    lang().send(player, "common.player_not_found");
                    return;
                }
                var result = teamManager.invite(player, target);
                lang().send(player, "common.result", "result", result);
                if (result == TeamManager.InviteResult.OK) {
                    lang().send(target, "team.hub.invite_received", "player", player.getName());
                }
                reopen();
            });
            return;
        }

        if (!myRole().canEditConfig() && slot != LEAVE_SLOT
                && slot != SHARE_XP_SLOT && slot != SHARE_LOOT_SLOT && slot != SHARE_GOLD_SLOT
                && slot != BUFF_XP_SLOT && slot != BUFF_DAMAGE_SLOT && slot != BUFF_REGEN_SLOT
                && slot != BUFF_SPEED_SLOT) {
            if (slot == NAME_SLOT || slot == COLOR_SLOT || slot == MAX_PLAYERS_SLOT || slot == MIN_LEVEL_SLOT) {
                lang().send(player, "team.hub.only_leader_config");
                return;
            }
        }

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "team.hub.prompt_name", value -> {
                team.setName(value.trim());
                reopen();
            });
            return;
        }

        if (slot == COLOR_SLOT) {
            NamedTextColor[] palette = { NamedTextColor.AQUA, NamedTextColor.GREEN, NamedTextColor.RED,
                    NamedTextColor.GOLD, NamedTextColor.LIGHT_PURPLE, NamedTextColor.BLUE };
            int currentIndex = java.util.Arrays.asList(palette).indexOf(team.color());
            team.setColor(palette[(Math.max(0, currentIndex) + 1) % palette.length]);
            build();
            return;
        }

        if (slot == MAX_PLAYERS_SLOT) {
            team.setMaxPlayers(team.maxPlayers() + (event.isRightClick() ? -1 : 1));
            build();
            return;
        }

        if (slot == MIN_LEVEL_SLOT) {
            team.setMinLevel(team.minLevel() + (event.isRightClick() ? -1 : 1));
            build();
            return;
        }

        if (slot == SHARE_XP_SLOT) {
            team.setShareXp(!team.shareXp());
            build();
            return;
        }

        if (slot == SHARE_LOOT_SLOT) {
            team.setShareLoot(!team.shareLoot());
            build();
            return;
        }

        if (slot == SHARE_GOLD_SLOT) {
            team.setShareGold(!team.shareGold());
            build();
            return;
        }

        boolean isBuffSlot = slot == BUFF_XP_SLOT || slot == BUFF_DAMAGE_SLOT || slot == BUFF_REGEN_SLOT
                || slot == BUFF_SPEED_SLOT;

        if (isBuffSlot && !myRole().canManageBuffs()) {
            lang().send(player, "team.hub.no_permission_buffs");
            return;
        }

        if (slot == BUFF_XP_SLOT) {
            team.toggleBuff(TeamBuff.XP_BOOST);
            build();
            return;
        }

        if (slot == BUFF_DAMAGE_SLOT) {
            team.toggleBuff(TeamBuff.DAMAGE_BOOST);
            build();
            return;
        }

        if (slot == BUFF_REGEN_SLOT) {
            team.toggleBuff(TeamBuff.REGEN_BOOST);
            build();
            return;
        }

        if (slot == BUFF_SPEED_SLOT) {
            team.toggleBuff(TeamBuff.SPEED_BOOST);
            build();
            return;
        }

        if (slot == LEAVE_SLOT) {
            boolean disbanded = teamManager.leave(player.getUniqueId());
            lang().send(player, disbanded ? "team.leave.disbanded" : "team.leave.left");
            close();
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void handleMemberClick(InventoryClickEvent event, Team team, UUID memberId) {

        if (memberId.equals(player.getUniqueId())) {
            return;
        }

        if (event.isShiftClick()) {
            var result = teamManager.kick(player, memberId);
            lang().send(player, "common.result", "result", result);
            reopen();
            return;
        }

        TeamRole current = team.roleOf(memberId);
        TeamRole[] values = TeamRole.values();
        TeamRole next = values[(current.ordinal() + 1) % values.length];

        if (next == TeamRole.LEADER) {
            next = values[(next.ordinal() + 1) % values.length];
        }

        teamManager.setRole(player, memberId, next);
        build();
    }

    private void reopen() {
        new TeamHubGUI(player, teamManager, chatPromptManager).open();
    }

    private org.bukkit.inventory.ItemStack buffItem(Team team, TeamBuff buff) {

        boolean enabled = team.hasBuff(buff);

        return new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(Component.text(buff.displayName(lang()) + " +" + (int) (buff.percent() * 100) + "%",
                        enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .setLore(Component.text(lang().raw(enabled ? "team.hub.lore.click_deactivate"
                        : "team.hub.lore.click_activate"), NamedTextColor.GRAY))
                .build();
    }

}
