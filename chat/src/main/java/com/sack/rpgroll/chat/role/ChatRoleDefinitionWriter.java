package com.sack.rpgroll.chat.role;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class ChatRoleDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public ChatRoleDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(ChatRole role) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", role.id());
        config.set("prefix", role.prefix());
        config.set("suffix", role.suffix());
        config.set("color", role.color());
        config.set("icon", role.icon());
        config.set("priority", role.priority());
        config.set("permission", role.permission());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, role.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando rol '" + role.id() + "': " + e.getMessage());
        }
    }

}
