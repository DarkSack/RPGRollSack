package com.sack.rpgroll.npcs.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.npcs.core.NpcEditSession;
import com.sack.rpgroll.npcs.core.NpcManager;
import com.sack.rpgroll.npcs.core.NpcSessionManager;
import com.sack.rpgroll.npcs.core.NpcSpawnManager;
import com.sack.rpgroll.npcs.core.NpcWriter;
import com.sack.rpgroll.npcs.integration.MineSkinClient;
import com.sack.rpgroll.npcs.listener.ChatPromptManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class NpcAdminGUI extends InventoryGUI {

        private static final int SLOT_NAME = 10;
        private static final int SLOT_SKIN = 11;
        private static final int SLOT_POSE = 12;
        private static final int SLOT_POSITION = 13;
        private static final int SLOT_ACTIONS = 14;

        private static final int SLOT_SAVE = 21;
        private static final int SLOT_CANCEL = 23;

        private final NpcEditSession session;

        private final NpcSessionManager sessionManager;
        private final ChatPromptManager chatPromptManager;

        private final NpcManager npcManager;
        private final NpcSpawnManager spawnManager;

        private final MineSkinClient mineSkinClient;

        private final NpcWriter writer;
        private final LangManager langManager;

        public NpcAdminGUI(
                        Player player,
                        NpcEditSession session,
                        NpcSessionManager sessionManager,
                        ChatPromptManager chatPromptManager,
                        NpcManager npcManager,
                        NpcSpawnManager spawnManager,
                        MineSkinClient mineSkinClient,
                        NpcWriter writer,
                        LangManager langManager) {

                super(
                                player,
                                langManager.component("adminGui.title", "id", session.getId()),
                                27);

                this.session = session;
                this.sessionManager = sessionManager;
                this.chatPromptManager = chatPromptManager;

                this.npcManager = npcManager;
                this.spawnManager = spawnManager;

                this.mineSkinClient = mineSkinClient;

                this.writer = writer;
                this.langManager = langManager;
        }

        @Override
        public void build() {

                clear();

                for (int i = 0; i < 9; i++) {
                        setItem(
                                        i,
                                        ItemBuilder.createFiller());
                }

                for (int i = 18; i < 27; i++) {
                        setItem(
                                        i,
                                        ItemBuilder.createFiller());
                }

                setItem(
                                SLOT_NAME,
                                new ItemBuilder(Material.NAME_TAG)

                                                .setName(
                                                                langManager.component("adminGui.name_label"))

                                                .setLore(
                                                                Component.text(
                                                                                session.getDisplayName(),
                                                                                NamedTextColor.WHITE),

                                                                langManager.component("adminGui.click_to_change"))

                                                .build());

                ItemStack skinItem;

                if (session.getSkinValue().isBlank()) {

                        skinItem = new ItemBuilder(Material.PLAYER_HEAD)

                                        .setName(
                                                        langManager.component("adminGui.skin_label"))

                                        .setLore(
                                                        langManager.component("adminGui.skin_none"),

                                                        langManager.component("adminGui.skin_click_configure"))

                                        .build();

                } else {

                        skinItem = ItemBuilder.skull(
                                        session.getSkinValue())

                                        .setName(
                                                        langManager.component("adminGui.skin_label"))

                                        .setLore(
                                                        langManager.component("adminGui.skin_configured"),

                                                        langManager.component("adminGui.click_to_change"))

                                        .build();

                }

                setItem(
                                SLOT_SKIN,
                                skinItem);

                setItem(
                                SLOT_POSE,
                                new ItemBuilder(Material.ARMOR_STAND)

                                                .setName(
                                                                langManager.component("adminGui.pose_label"))

                                                .setLore(
                                                                Component.text(
                                                                                session.getPose(),
                                                                                NamedTextColor.WHITE),

                                                                langManager.component("adminGui.click_to_change"))

                                                .build());

                List<Component> positionLore = new ArrayList<>();

                if (session.hasLocation()) {

                        positionLore.add(
                                        Component.text(
                                                        session.getWorld()
                                                                        + " ("
                                                                        + Math.round(session.getX())
                                                                        + ", "
                                                                        + Math.round(session.getY())
                                                                        + ", "
                                                                        + Math.round(session.getZ())
                                                                        + ")",
                                                        NamedTextColor.WHITE));

                } else {

                        positionLore.add(
                                        langManager.component("adminGui.position_none"));

                }

                positionLore.add(
                                langManager.component("adminGui.position_click"));

                setItem(
                                SLOT_POSITION,
                                new ItemBuilder(Material.COMPASS)

                                                .setName(
                                                                langManager.component("adminGui.position_label"))

                                                .setLore(positionLore)

                                                .build());

                setItem(
                                SLOT_ACTIONS,

                                new ItemBuilder(Material.BOOK)

                                                .setName(
                                                                langManager.component("adminGui.actions_label"))

                                                .setLore(

                                                                langManager.component("adminGui.actions_count",
                                                                                "count", session.getActions().size()),

                                                                langManager.component("adminGui.actions_click_manage")

                                                )

                                                .build());

                setItem(
                                SLOT_SAVE,
                                ItemBuilder.createConfirmButton(
                                                session.isComplete()
                                                                ? langManager.raw("adminGui.save")
                                                                : langManager.raw("adminGui.save_incomplete")));

                setItem(
                                SLOT_CANCEL,
                                ItemBuilder.createCancelButton(
                                                langManager.raw("adminGui.cancel")));

        }

        @Override
        public void handleClick(InventoryClickEvent event) {

                event.setCancelled(true);

                switch (event.getRawSlot()) {

                        case SLOT_NAME -> {

                                close();

                                chatPromptManager.prompt(
                                                player,
                                                langManager.raw("adminGui.prompt_name"),
                                                input -> {

                                                        session.setDisplayName(input);

                                                        reopen();

                                                });

                        }

                        case SLOT_SKIN -> {

                                close();

                                chatPromptManager.prompt(
                                                player,
                                                langManager.raw("adminGui.prompt_skin"),
                                                input -> {

                                                        langManager.send(player, "adminGui.skin_querying");

                                                        mineSkinClient.resolve(

                                                                        input,

                                                                        (value, signature) -> {

                                                                                session.setSkin(
                                                                                                value,
                                                                                                signature);

                                                                                langManager.send(player,
                                                                                                "adminGui.skin_applied");

                                                                                reopen();

                                                                        },

                                                                        () -> {

                                                                                langManager.send(player,
                                                                                                "adminGui.skin_resolve_failed");

                                                                                reopen();

                                                                        }

                                                );

                                                });

                        }

                        case SLOT_POSITION -> {

                                session.setLocation(
                                                player.getLocation());

                                langManager.send(player, "adminGui.location_assigned");

                                build();
                                reopen();

                        }

                        case SLOT_POSE -> {

                                close();

                                new NpcPoseSelectGUI(
                                                player,
                                                session,
                                                this,
                                                langManager).open();

                        }

                        case SLOT_ACTIONS -> {

                                close();

                                new NpcActionsGUI(
                                                player,
                                                session,
                                                this,
                                                chatPromptManager,
                                                langManager).open();

                        }

                        case SLOT_SAVE -> {

                                if (!session.isComplete()) {

                                        langManager.send(player, "adminGui.save_missing_fields");

                                        return;
                                }

                                boolean saved = writer.save(session);

                                if (saved) {

                                        npcManager.reload();

                                        spawnManager.despawnAllForEveryone();

                                        npcManager.getAll()
                                                        .forEach(
                                                                        spawnManager::register);

                                        org.bukkit.Bukkit
                                                        .getOnlinePlayers()
                                                        .forEach(
                                                                        online -> spawnManager.updateVisibility(
                                                                                        online,
                                                                                        npcManager.getAll()));

                                        langManager.send(player, "adminGui.save_success",
                                                        "id", session.getId());

                                } else {

                                        langManager.send(player, "adminGui.save_error");

                                }

                                sessionManager.end(
                                                player.getUniqueId());

                                close();

                        }

                        case SLOT_CANCEL -> {

                                sessionManager.end(
                                                player.getUniqueId());

                                close();

                                langManager.send(player, "adminGui.edit_cancelled");

                        }

                }

        }

        public void reopen() {

                new NpcAdminGUI(

                                player,
                                session,
                                sessionManager,
                                chatPromptManager,
                                npcManager,
                                spawnManager,
                                mineSkinClient,
                                writer,
                                langManager

                ).open();

        }

}