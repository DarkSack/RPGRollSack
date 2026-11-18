package com.sack.rpgroll.mobs.bestiary;

import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /bestiario (también /bestiary y /pokedex): abre tu bestiario. */
public final class BestiaryCommand implements CommandExecutor {

    private final Bestiary bestiary;
    private final LangManager lang;

    public BestiaryCommand(Bestiary bestiary, LangManager lang) {
        this.bestiary = bestiary;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            new BestiaryGUI(player, bestiary, lang).open();
        } else {
            sender.sendMessage("Solo para jugadores.");
        }
        return true;
    }
}
