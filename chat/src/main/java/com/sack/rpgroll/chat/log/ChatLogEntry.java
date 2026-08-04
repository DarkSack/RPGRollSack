package com.sack.rpgroll.chat.log;

import java.util.UUID;

/** Entrada de historial — spec "cada mensaje queda registrado". */
public class ChatLogEntry {

    private final long id;
    private final long timestamp;
    private final String channelId;
    private final UUID senderId;
    private final String senderName;
    private final String message;
    private final int recipientCount;
    private boolean redacted;

    public ChatLogEntry(long id, long timestamp, String channelId, UUID senderId, String senderName, String message,
            int recipientCount) {
        this.id = id;
        this.timestamp = timestamp;
        this.channelId = channelId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.recipientCount = recipientCount;
    }

    public long id() {
        return id;
    }

    public long timestamp() {
        return timestamp;
    }

    public String channelId() {
        return channelId;
    }

    public UUID senderId() {
        return senderId;
    }

    public String senderName() {
        return senderName;
    }

    public String message() {
        return message;
    }

    public int recipientCount() {
        return recipientCount;
    }

    public boolean redacted() {
        return redacted;
    }

    public void redact() {
        this.redacted = true;
    }

    public String formatLine() {
        return "[" + java.time.Instant.ofEpochMilli(timestamp) + "] #" + channelId + " " + senderName + ": "
                + (redacted ? "[redactado por staff]" : message);
    }

}
