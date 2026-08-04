package com.sack.rpgroll.chat.role;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Comparator;
import java.util.Optional;

/** Carga los roles de chat desde plugins/RPGRoll-Chat/roles/*.yml. */
public class ChatRoleManager extends ContentManager<ChatRole> {

    private final ChatRoleDefinitionWriter writer;

    public ChatRoleManager(JavaPlugin chatPlugin) {
        super(resolveCoreInstance(), new YamlLoader(chatPlugin), "roles", "rol de chat", new ChatRoleParser());
        this.writer = new ChatRoleDefinitionWriter(new File(chatPlugin.getDataFolder(), "roles"),
                chatPlugin.getLogger());
    }

    /** Persiste el rol a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(ChatRole role) {
        writer.save(role);
        reload();
    }

    /** @return el rol de mayor prioridad cuyo permiso tenga el jugador (vacío si ninguno aplica). */
    public Optional<ChatRole> resolveFor(Player player) {
        return getAll().stream()
                .filter(role -> role.matches(player))
                .max(Comparator.comparingInt(ChatRole::priority));
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
