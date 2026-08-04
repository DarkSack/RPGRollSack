package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuildCustomizationGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int COLOR_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int MOTTO_SLOT = 12;
    private static final int DESCRIPTION_SLOT = 13;
    private static final int NAME_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private static final NamedTextColor[] PALETTE = { NamedTextColor.GOLD, NamedTextColor.RED, NamedTextColor.GREEN,
            NamedTextColor.AQUA, NamedTextColor.LIGHT_PURPLE, NamedTextColor.BLUE, NamedTextColor.WHITE };

    private final Guild guild;
    private final GuildManager guildManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;

    public GuildCustomizationGUI(Player player, Guild guild, GuildManager guildManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Personalización: " + guild.name(), NamedTextColor.GOLD), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + guild.name(), NamedTextColor.YELLOW))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.WHITE_DYE)
                .setName(Component.text("Color: " + guild.color(), guild.color()))
                .setLore(Component.text("Click para cambiar", NamedTextColor.GRAY))
                .build());

        setItem(ICON_SLOT, new ItemBuilder(guild.icon())
                .setName(Component.text("Ícono: " + guild.icon(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir un Material nuevo", NamedTextColor.GRAY))
                .build());

        setItem(MOTTO_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Lema: " + (guild.motto().isBlank() ? "(sin lema)" : guild.motto()),
                        NamedTextColor.YELLOW))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(guild.description().isBlank() ? "(sin descripción)"
                        : guild.description()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        if (!guild.roleOf(player.getUniqueId()).canManageSettings() && event.getSlot() != BACK_SLOT) {
            player.sendMessage(Component.text("No tenés permiso para personalizar la guild.", NamedTextColor.RED));
            return;
        }

        switch (event.getSlot()) {
            case NAME_SLOT -> chatPromptManager.prompt(player, "Escribí el nuevo nombre de la guild:", value -> {
                guild.setName(value.trim());
                save();
                reopen();
            });
            case COLOR_SLOT -> {
                int currentIndex = java.util.Arrays.asList(PALETTE).indexOf(guild.color());
                guild.setColor(PALETTE[(Math.max(0, currentIndex) + 1) % PALETTE.length]);
                save();
                build();
            }
            case ICON_SLOT -> chatPromptManager.prompt(player, "Escribí el nombre del Material (ej. DIAMOND_SWORD):",
                    value -> {
                        try {
                            guild.setIcon(Material.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)));
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(Component.text("Material inválido.", NamedTextColor.RED));
                        }
                        save();
                        reopen();
                    });
            case MOTTO_SLOT -> chatPromptManager.prompt(player, "Escribí el nuevo lema de la guild:", value -> {
                guild.setMotto(value.trim());
                save();
                reopen();
            });
            case DESCRIPTION_SLOT -> chatPromptManager.prompt(player, "Escribí la nueva descripción de la guild:",
                    value -> {
                        guild.setDescription(value.trim());
                        save();
                        reopen();
                    });
            case BACK_SLOT -> onBack.run();
            default -> {
            }
        }
    }

    private void save() {
        guildManager.save(guild);
    }

    private void reopen() {
        new GuildCustomizationGUI(player, guild, guildManager, chatPromptManager, onBack).open();
    }

}
