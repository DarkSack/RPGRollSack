package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gui.character.ClassSelectionGUI;
import com.sack.rpgroll.gui.character.RaceSelectionGUI;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.api.race.RaceManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

        if (args.length < 1) {
            player.sendMessage(Component.text("Uso: " + getUsage(), NamedTextColor.RED));
            return;
        }

        String target = args[0].toLowerCase();

        try {
            switch (target) {
                case "race" -> openRacePreview(player);
                case "class" -> openClassPreview(player);
                default -> player.sendMessage(
                        Component.text("Opción inválida. Usa: race o class", NamedTextColor.RED));
            }
        } catch (Exception exception) {
            player.sendMessage(Component.text("Error al abrir la interfaz.", NamedTextColor.RED));
            plugin.getLogger().severe("✘ Error en /rpg admingui: " + exception.getMessage());
        }
    }

    private void openRacePreview(Player player) {
        RaceManager raceManager = plugin.getBootstrap().getServices().get(RaceManager.class);
        RaceAttributeApplier raceAttributeApplier = plugin.getBootstrap().getServices().get(RaceAttributeApplier.class);

        RaceSelectionGUI gui = new RaceSelectionGUI(player, raceManager, raceId -> {
            player.sendMessage(Component.text("[Preview] Raza seleccionada: ", NamedTextColor.AQUA)
                    .append(Component.text(raceId, NamedTextColor.WHITE)));

            raceManager.get(raceId).ifPresent(race -> raceAttributeApplier.apply(player, race));

        }, false);

        gui.open();
    }

    private void openClassPreview(Player player) {
        ClassManager classManager = plugin.getBootstrap().getServices().get(ClassManager.class);

        ClassSelectionGUI gui = new ClassSelectionGUI(player, classManager, "(preview)", playerClass -> {
            player.sendMessage(Component.text("[Preview] Clase seleccionada: ", NamedTextColor.AQUA)
                    .append(Component.text(playerClass, NamedTextColor.WHITE)));
        }, false);

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