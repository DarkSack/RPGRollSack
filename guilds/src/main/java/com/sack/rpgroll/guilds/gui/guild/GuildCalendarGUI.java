package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.event.GuildEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GuildCalendarGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final Guild guild;
    private final GuildManager guildManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final List<GuildEvent> events;

    public GuildCalendarGUI(Player player, Guild guild, GuildManager guildManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Calendario: " + guild.name(), NamedTextColor.GOLD), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.events = guild.calendar();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        SimpleDateFormat format = new SimpleDateFormat("dd/MM HH:mm");

        for (int i = 0; i < events.size() && i < 36; i++) {

            GuildEvent event = events.get(i);

            setItem(i, new ItemBuilder(Material.CLOCK)
                    .setName(Component.text(event.name(), NamedTextColor.YELLOW))
                    .setLore(Component.text(event.type() + " · " + format.format(new Date(event.scheduledAtMillis())),
                            NamedTextColor.GRAY),
                            Component.text(event.description(), NamedTextColor.GRAY),
                            Component.text("Shift-click para eliminar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agendar evento", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        boolean canManage = guild.roleOf(player.getUniqueId()).canManageSettings();

        if (slot < events.size() && slot < 36) {

            if (event.isShiftClick()) {

                if (!canManage) {
                    player.sendMessage(Component.text("No tenés permiso para editar el calendario.",
                            NamedTextColor.RED));
                    return;
                }

                guild.removeCalendarEvent(events.get(slot).id());
                guildManager.save(guild);
                reopen();
            }
            return;
        }

        if (slot == ADD_SLOT) {

            if (!canManage) {
                player.sendMessage(Component.text("No tenés permiso para agendar eventos.", NamedTextColor.RED));
                return;
            }

            promptNewEvent();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNewEvent() {
        chatPromptManager.prompt(player,
                "Escribí: nombre;tipo;minutos-desde-ahora;descripción (ej. Torneo;torneo;60;Torneo semanal)", value -> {

                    String[] parts = value.split(";", 4);

                    if (parts.length < 4) {
                        player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        reopen();
                        return;
                    }

                    long minutes;
                    try {
                        minutes = Long.parseLong(parts[2].trim());
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("Los minutos deben ser un número.", NamedTextColor.RED));
                        reopen();
                        return;
                    }

                    GuildEvent guildEvent = new GuildEvent(UUID.randomUUID().toString(), parts[0].trim(),
                            parts[1].trim(), parts[3].trim(),
                            System.currentTimeMillis() + minutes * 60_000);

                    guild.addCalendarEvent(guildEvent);
                    guildManager.save(guild);
                    reopen();
                });
    }

    private void reopen() {
        new GuildCalendarGUI(player, guild, guildManager, chatPromptManager, onBack).open();
    }

}
