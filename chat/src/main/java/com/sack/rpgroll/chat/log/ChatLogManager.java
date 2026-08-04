package com.sack.rpgroll.chat.log;

import com.sack.rpgroll.chat.channel.ChatChannel;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Historial de chat — spec "Chat Logs: exportar/buscar/moderar/revisar".
 * La búsqueda opera sobre un buffer en memoria acotado ({@link #MAX_RECENT}
 * entradas); el archivo de texto diario es la copia durable para revisión
 * manual fuera del buffer. "Moderar" un mensaje ya entregado no puede
 * borrarlo de la pantalla del jugador (Minecraft no soporta editar/retirar
 * chat ya enviado) — marca la entrada como redactada en el historial.
 */
public class ChatLogManager {

    private static final int MAX_RECENT = 5000;

    private final Plugin plugin;
    private final File logsFolder;
    private final Deque<ChatLogEntry> recent = new ArrayDeque<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public ChatLogManager(Plugin plugin) {
        this.plugin = plugin;
        this.logsFolder = new File(plugin.getDataFolder(), "logs");

        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
    }

    public synchronized long log(ChatChannel channel, Player sender, String message, List<Player> recipients) {

        long id = idGenerator.getAndIncrement();
        ChatLogEntry entry = new ChatLogEntry(id, System.currentTimeMillis(), channel.id(), sender.getUniqueId(),
                sender.getName(), message, recipients.size());

        recent.addLast(entry);
        while (recent.size() > MAX_RECENT) {
            recent.pollFirst();
        }

        appendToFile(entry);
        return id;
    }

    private void appendToFile(ChatLogEntry entry) {

        String fileName = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now()) + ".log";
        File file = new File(logsFolder, fileName);

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(entry.formatLine());
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error escribiendo el log de chat: " + e.getMessage());
        }
    }

    public synchronized List<ChatLogEntry> search(String channelId, String playerName, String dateIso) {

        return recent.stream()
                .filter(entry -> channelId == null || entry.channelId().equalsIgnoreCase(channelId))
                .filter(entry -> playerName == null || entry.senderName().equalsIgnoreCase(playerName))
                .filter(entry -> dateIso == null || matchesDate(entry, dateIso))
                .toList();
    }

    private boolean matchesDate(ChatLogEntry entry, String dateIso) {
        String entryDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .format(java.time.Instant.ofEpochMilli(entry.timestamp()).atZone(java.time.ZoneId.systemDefault()));
        return entryDate.equals(dateIso);
    }

    public synchronized boolean redact(long messageId) {
        return recent.stream()
                .filter(entry -> entry.id() == messageId)
                .findFirst()
                .map(entry -> {
                    entry.redact();
                    return true;
                })
                .orElse(false);
    }

    public File export(List<ChatLogEntry> entries, String exportName) {

        File file = new File(logsFolder, "export-" + exportName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_")
                + "-" + System.currentTimeMillis() + ".log");

        try (FileWriter writer = new FileWriter(file)) {
            for (ChatLogEntry entry : entries) {
                writer.write(entry.formatLine());
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error exportando el log de chat: " + e.getMessage());
        }

        return file;
    }

    public File clearRecent() {
        File snapshot = export(List.copyOf(recent), "clear-snapshot");
        recent.clear();
        return snapshot;
    }

}
