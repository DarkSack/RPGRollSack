package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.character.ClassSelectionGUI;
import com.sack.rpgroll.gui.character.RaceSelectionGUI;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.api.race.RaceManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando de administrador para abrir las GUIs de selección directamente,
 * en modo preview — no modifica el personaje del admin, solo permite
 * revisar cómo se ven razas/clases cargadas desde YAML.
 * Uso: /rpg admingui <race|class>
 */
public class AdminGuiCommand implements RPGCommand {

    private final RPGRoll plugin;

    public AdminGuiCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;
        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        if (args.length < 1) {
            lang.send(player, "admin_gui_command.usage", "usage", getUsage());
            return;
        }

        String target = args[0].toLowerCase();

        try {
            switch (target) {
                case "race" -> openRacePreview(player, lang);
                case "class" -> openClassPreview(player, lang);
                default -> lang.send(player, "admin_gui_command.invalid_option");
            }
        } catch (Exception exception) {
            lang.send(player, "admin_gui_command.error");
            plugin.getLogger().severe("✘ Error en /rpg admingui: " + exception.getMessage());
        }
    }

    private void openRacePreview(Player player, LangManager lang) {
        RaceManager raceManager = plugin.getBootstrap().getServices().get(RaceManager.class);
        RaceAttributeApplier raceAttributeApplier = plugin.getBootstrap().getServices().get(RaceAttributeApplier.class);

        RaceSelectionGUI gui = new RaceSelectionGUI(player, raceManager, raceId -> {
            lang.send(player, "admin_gui_command.race_preview", "race", raceId);

            raceManager.get(raceId).ifPresent(race -> raceAttributeApplier.apply(player, race));

        }, false, lang);

        gui.open();
    }

    private void openClassPreview(Player player, LangManager lang) {
        ClassManager classManager = plugin.getBootstrap().getServices().get(ClassManager.class);

        ClassSelectionGUI gui = new ClassSelectionGUI(player, classManager, "(preview)", playerClass -> {
            lang.send(player, "admin_gui_command.class_preview", "class", playerClass);
        }, false, lang);

        gui.open();
    }

    @Override
    public String getName() {
        return "admingui";
    }

    @Override
    public String getDescription() {
        return "Abre las interfaces de selección en modo preview (admin)";
    }

    @Override
    public String getUsage() {
        return "/rpg admingui <race|class>";
    }

    @Override
    public String getPermission() {
        return "rpgroll.admin.gui";
    }

    @Override
    public List<String> getAliases() {
        return List.of("agui");
    }

}
