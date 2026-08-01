package com.sack.rpgroll.npcs.gui;

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
import net.kyori.adventure.text.format.TextDecoration;

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

        public NpcAdminGUI(
                        Player player,
                        NpcEditSession session,
                        NpcSessionManager sessionManager,
                        ChatPromptManager chatPromptManager,
                        NpcManager npcManager,
                        NpcSpawnManager spawnManager,
                        MineSkinClient mineSkinClient,
                        NpcWriter writer) {

                super(
                                player,
                                Component.text(
                                                "Editor de NPC: "
                                                                + session.getId(),
                                                NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                                27);

                this.session = session;
                this.sessionManager = sessionManager;
                this.chatPromptManager = chatPromptManager;

                this.npcManager = npcManager;
                this.spawnManager = spawnManager;

                this.mineSkinClient = mineSkinClient;

                this.writer = writer;
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
                                                                Component.text(
                                                                                "Nombre",
                                                                                NamedTextColor.GOLD)
                                                                                .decorate(TextDecoration.BOLD))

                                                .setLore(
                                                                Component.text(
                                                                                session.getDisplayName(),
                                                                                NamedTextColor.WHITE),

                                                                Component.text(
                                                                                "Click para cambiar",
                                                                                NamedTextColor.GRAY))

                                                .build());

                ItemStack skinItem;

                if (session.getSkinValue().isBlank()) {

                        skinItem = new ItemBuilder(Material.PLAYER_HEAD)

                                        .setName(
                                                        Component.text(
                                                                        "Skin",
                                                                        NamedTextColor.GOLD)
                                                                        .decorate(TextDecoration.BOLD))

                                        .setLore(
                                                        Component.text(
                                                                        "Sin skin custom",
                                                                        NamedTextColor.GRAY),

                                                        Component.text(
                                                                        "Click para configurar",
                                                                        NamedTextColor.GRAY))

                                        .build();

                } else {

                        skinItem = ItemBuilder.skull(
                                        session.getSkinValue())

                                        .setName(
                                                        Component.text(
                                                                        "Skin",
                                                                        NamedTextColor.GOLD)
                                                                        .decorate(TextDecoration.BOLD))

                                        .setLore(
                                                        Component.text(
                                                                        "Skin configurada ✔",
                                                                        NamedTextColor.GREEN),

                                                        Component.text(
                                                                        "Click para cambiar",
                                                                        NamedTextColor.GRAY))

                                        .build();

                }

                setItem(
                                SLOT_SKIN,
                                skinItem);

                setItem(
                                SLOT_POSE,
                                new ItemBuilder(Material.ARMOR_STAND)

                                                .setName(
                                                                Component.text(
                                                                                "Pose",
                                                                                NamedTextColor.GOLD)
                                                                                .decorate(TextDecoration.BOLD))

                                                .setLore(
                                                                Component.text(
                                                                                session.getPose(),
                                                                                NamedTextColor.WHITE),

                                                                Component.text(
                                                                                "Click para cambiar",
                                                                                NamedTextColor.GRAY))

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
                                        Component.text(
                                                        "Sin ubicación asignada",
                                                        NamedTextColor.RED));

                }

                positionLore.add(
                                Component.text(
                                                "Click para usar tu ubicación actual",
                                                NamedTextColor.GRAY));

                setItem(
                                SLOT_POSITION,
                                new ItemBuilder(Material.COMPASS)

                                                .setName(
                                                                Component.text(
                                                                                "Posición",
                                                                                NamedTextColor.GOLD)
                                                                                .decorate(TextDecoration.BOLD))

                                                .setLore(positionLore)

                                                .build());

                setItem(
                                SLOT_ACTIONS,

                                new ItemBuilder(Material.BOOK)

                                                .setName(
                                                                Component.text(
                                                                                "Acciones",
                                                                                NamedTextColor.GOLD)
                                                                                .decorate(TextDecoration.BOLD))

                                                .setLore(

                                                                Component.text(
                                                                                session.getActions().size()
                                                                                                + " acción(es) configurada(s)",
                                                                                NamedTextColor.WHITE),

                                                                Component.text(
                                                                                "Click para gestionar",
                                                                                NamedTextColor.GRAY)

                                                )

                                                .build());

                setItem(
                                SLOT_SAVE,
                                ItemBuilder.createConfirmButton(
                                                session.isComplete()
                                                                ? "Guardar"
                                                                : "Guardar (falta posición o nombre)"));

                setItem(
                                SLOT_CANCEL,
                                ItemBuilder.createCancelButton(
                                                "Cancelar"));

        }

        @Override
        public void handleClick(InventoryClickEvent event) {

                event.setCancelled(true);

                switch (event.getRawSlot()) {

                        case SLOT_NAME -> {

                                close();

                                chatPromptManager.prompt(
                                                player,
                                                "Escribe el nuevo nombre para el NPC:",
                                                input -> {

                                                        session.setDisplayName(input);

                                                        reopen();

                                                });

                        }

                        case SLOT_SKIN -> {

                                close();

                                chatPromptManager.prompt(
                                                player,
                                                "Pega el link o ID de MineSkin:",
                                                input -> {

                                                        player.sendMessage(
                                                                        Component.text(
                                                                                        "Consultando MineSkin...",
                                                                                        NamedTextColor.GRAY));

                                                        mineSkinClient.resolve(

                                                                        input,

                                                                        (value, signature) -> {

                                                                                session.setSkin(
                                                                                                value,
                                                                                                signature);

                                                                                player.sendMessage(
                                                                                                Component.text(
                                                                                                                "✔ Skin aplicada.",
                                                                                                                NamedTextColor.GREEN));

                                                                                reopen();

                                                                        },

                                                                        () -> {

                                                                                player.sendMessage(
                                                                                                Component.text(
                                                                                                                "No se pudo resolver la skin.",
                                                                                                                NamedTextColor.RED));

                                                                                reopen();

                                                                        }

                                                );

                                                });

                        }

                        case SLOT_POSITION -> {

                                session.setLocation(
                                                player.getLocation());

                                player.sendMessage(
                                                Component.text(
                                                                "✔ Ubicación asignada.",
                                                                NamedTextColor.GREEN));

                                build();
                                reopen();

                        }

                        case SLOT_POSE -> {

                                close();

                                new NpcPoseSelectGUI(
                                                player,
                                                session,
                                                this).open();

                        }

                        case SLOT_ACTIONS -> {

                                close();

                                new NpcActionsGUI(
                                                player,
                                                session,
                                                this,
                                                chatPromptManager).open();

                        }

                        case SLOT_SAVE -> {

                                if (!session.isComplete()) {

                                        player.sendMessage(
                                                        Component.text(
                                                                        "Falta nombre o posición antes de guardar.",
                                                                        NamedTextColor.RED));

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

                                        player.sendMessage(
                                                        Component.text(
                                                                        "✔ NPC guardado: "
                                                                                        + session.getId(),
                                                                        NamedTextColor.GREEN));

                                } else {

                                        player.sendMessage(
                                                        Component.text(
                                                                        "Error al guardar el NPC.",
                                                                        NamedTextColor.RED));

                                }

                                sessionManager.end(
                                                player.getUniqueId());

                                close();

                        }

                        case SLOT_CANCEL -> {

                                sessionManager.end(
                                                player.getUniqueId());

                                close();

                                player.sendMessage(
                                                Component.text(
                                                                "Edición cancelada.",
                                                                NamedTextColor.YELLOW));

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
                                writer

                ).open();

        }

}