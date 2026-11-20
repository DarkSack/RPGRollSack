package com.sack.rpgroll.guilds.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.GuildNameStyle;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.command.CommandSender;

import java.util.Optional;

/** Lo que comparten {@code /guild color} y {@code /guildadmin color}: validar, guardar y avisar. */
final class GuildColorSetter {

    private GuildColorSetter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** {@code input} es el nombre con códigos de color, o {@code reset} para quitarlos. */
    static void apply(CommandSender sender, Guild guild, String input, GuildManager guildManager, LangManager lang) {

        if (input.equalsIgnoreCase("reset")) {
            guild.setStyledName(null);
            guildManager.save(guild);
            lang.send(sender, "guild.color.reset");
            return;
        }

        Optional<String> styled = GuildNameStyle.parse(input, guild.name());

        if (styled.isEmpty()) {
            lang.send(sender, "guild.color.invalid", "name", guild.name());
            return;
        }

        guild.setStyledName(styled.get());
        guildManager.save(guild);
        // Como componente: así el aviso enseña los colores en vez de los códigos.
        lang.send(sender, "guild.color.success", "name", ComponentUtils.parse(styled.get()));
    }

}
