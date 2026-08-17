package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.GuildRole;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.UUID;

public class GuildMembersGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int INVITE_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final Guild guild;
    private final GuildManager guildManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final List<UUID> memberOrder;

    public GuildMembersGUI(Player player, Guild guild, GuildManager guildManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("guild.members.title", "name", guild.name()),
                NamedTextColor.GOLD), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.memberOrder = List.copyOf(guild.members().keySet());
    }

    private com.sack.rpgroll.common.lang.LangManager lang() {
        return chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < memberOrder.size() && i < 45; i++) {

            UUID memberId = memberOrder.get(i);
            var offline = Bukkit.getOfflinePlayer(memberId);
            GuildRole role = guild.roleOf(memberId);

            setItem(i, ItemBuilder.skull(null)
                    .setName(Component.text((offline.getName() != null ? offline.getName() : memberId.toString())
                            + " — " + role, role == GuildRole.LEADER ? NamedTextColor.GOLD : NamedTextColor.YELLOW))
                    .setLore(Component.text(lang().raw("guild.members.lore.next_role"), NamedTextColor.GRAY),
                            Component.text(lang().raw("guild.members.lore.kick"), NamedTextColor.GRAY))
                    .build());
        }

        setItem(INVITE_SLOT, new ItemBuilder(org.bukkit.Material.PLAYER_HEAD)
                .setName(Component.text(lang().raw("guild.members.button.invite"), NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        GuildRole myRole = guild.roleOf(player.getUniqueId());

        if (slot < memberOrder.size() && slot < 45) {

            UUID targetId = memberOrder.get(slot);

            if (targetId.equals(player.getUniqueId())) {
                return;
            }

            GuildRole targetRole = guild.roleOf(targetId);

            if (event.isShiftClick()) {
                if (!myRole.canKick() || !myRole.outranks(targetRole)) {
                    lang().send(player, "guild.members.no_permission_kick");
                    return;
                }
                guild.removeMember(targetId);
                guildManager.save(guild);
                reopen();
                return;
            }

            if (!myRole.canManageSettings() && myRole != GuildRole.LEADER) {
                lang().send(player, "guild.members.no_permission_role");
                return;
            }

            if (targetRole == GuildRole.LEADER) {
                return;
            }

            GuildRole[] values = { GuildRole.OFFICER, GuildRole.MEMBER, GuildRole.RECRUIT };
            int currentIndex = java.util.Arrays.asList(values).indexOf(targetRole);
            GuildRole next = values[(Math.max(0, currentIndex) + 1) % values.length];

            guild.setRole(targetId, next);
            guildManager.save(guild);
            build();
            return;
        }

        if (slot == INVITE_SLOT) {

            if (!myRole.canInvite()) {
                lang().send(player, "guild.members.no_permission_invite");
                return;
            }

            chatPromptManager.prompt(player, "guild.members.prompt_invite", value -> {
                Player target = Bukkit.getPlayerExact(value.trim());
                if (target == null) {
                    lang().send(player, "common.player_not_found");
                    return;
                }
                if (guildManager.findByMember(target.getUniqueId()).isPresent()) {
                    lang().send(player, "guild.members.target_already_in_guild");
                    return;
                }
                guildManager.invite(player.getUniqueId(), guild.id(), target.getUniqueId());
                lang().send(target, "guild.members.invite_received", "player", player.getName(),
                        "guild", guild.name(), "id", guild.id());
                lang().send(player, "guild.members.invite_sent");
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        new GuildMembersGUI(player, guild, guildManager, chatPromptManager, onBack).open();
    }

}
