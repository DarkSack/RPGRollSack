package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.config.ConfigManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.gameplay.combat.CombatTracker;
import com.sack.rpgroll.gameplay.hud.PlayerResourceBar;
import com.sack.rpgroll.gameplay.skill.Skill;
import com.sack.rpgroll.gameplay.skill.SkillCooldownTracker;
import com.sack.rpgroll.gameplay.skill.SkillManager;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Optional;

/**
 * Comando para usar (lanzar) una habilidad aprendida.
 * Uso: /rpg use <skillId>
 * <p>
 * Descuenta maná, respeta el cooldown propio de la skill y el cooldown
 * global entre skills (ver {@code skills.global_cooldown} en gameplay.yml),
 * y aplica daño mágico al objetivo apuntado (si hay uno en rango).
 */
public class UseSkillCommand implements RPGCommand {

    private static final double CAST_RANGE = 20.0;
    private static final double BASE_MAGIC_DAMAGE = 4.0;

    private final RPGRoll plugin;

    public UseSkillCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(Component.text("Uso: " + getUsage(), NamedTextColor.RED));
            return;
        }

        String skillId = args[0];

        try {

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            SkillManager skillManager = plugin.getBootstrap().getServices().get(SkillManager.class);
            SkillCooldownTracker cooldownTracker = plugin.getBootstrap().getServices()
                    .get(SkillCooldownTracker.class);
            CombatTracker combatTracker = plugin.getBootstrap().getServices().get(CombatTracker.class);
            PlayerResourceBar resourceBar = plugin.getBootstrap().getServices().get(PlayerResourceBar.class);
            ConfigManager configManager = plugin.getBootstrap().getServices().get(ConfigManager.class);

            Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());
            if (rpgPlayerOpt.isEmpty()) {
                player.sendMessage(Component.text("Error al cargar tus datos.", NamedTextColor.RED));
                return;
            }

            RPGPlayer rpgPlayer = rpgPlayerOpt.get();

            Optional<Skill> skillOpt = skillManager.get(skillId);
            if (skillOpt.isEmpty()) {
                player.sendMessage(Component.text("No existe la habilidad: " + skillId, NamedTextColor.RED));
                return;
            }

            Skill skill = skillOpt.get();

            if (!rpgPlayer.getSkills().hasSkill(skill.id())) {
                player.sendMessage(Component.text("No has aprendido esa habilidad.", NamedTextColor.RED));
                return;
            }

            FileConfiguration gameplayConfig = configManager.getConfig("gameplay.yml");
            double globalCooldown = gameplayConfig != null
                    ? gameplayConfig.getDouble("skills.global_cooldown", 1.0)
                    : 1.0;
            boolean allowInCombat = gameplayConfig == null || gameplayConfig.getBoolean("skills.allow_in_combat", true);
            int combatDuration = gameplayConfig != null ? gameplayConfig.getInt("combat.combat_duration", 10) : 10;

            if (!allowInCombat && combatTracker.isInCombat(player.getUniqueId(), combatDuration)) {
                player.sendMessage(Component.text("No puedes usar habilidades mientras estás en combate.",
                        NamedTextColor.RED));
                return;
            }

            CombatStats combatStats = rpgPlayer.getCombatStats();

            if (!skill.canUse(rpgPlayer.getLevel(), combatStats.currentMana())) {
                player.sendMessage(Component.text(
                        "No tienes suficiente maná (" + combatStats.currentMana() + "/" + skill.manaCost()
                                + ") o nivel suficiente (requiere nivel " + skill.requiredLevel() + ").",
                        NamedTextColor.RED));
                return;
            }

            double remainingCooldown = cooldownTracker.getRemainingSeconds(
                    player.getUniqueId(), skill.id(), skill.cooldownSeconds(), globalCooldown);

            if (remainingCooldown > 0) {
                player.sendMessage(Component.text(
                        String.format("Esa habilidad todavía está en cooldown (%.1fs restantes).", remainingCooldown),
                        NamedTextColor.YELLOW));
                return;
            }

            // Descontar maná
            RPGPlayer updatedPlayer = rpgPlayer.updateCombatStats(
                    combatStats.withMana(combatStats.currentMana() - skill.manaCost()));

            cooldownTracker.recordUse(player.getUniqueId(), skill.id());
            combatTracker.markInCombat(player.getUniqueId());

            LivingEntity target = findTarget(player);

            if (target != null) {

                int intelligenceModifier = rpgPlayer.getStats().getIntelligenceModifier();
                double damage = (BASE_MAGIC_DAMAGE + intelligenceModifier) * skill.damageMultiplier();

                target.damage(Math.max(0, damage), player);

                if (target instanceof Player targetPlayer) {
                    combatTracker.markInCombat(targetPlayer.getUniqueId());
                }

                player.sendMessage(Component.text(
                        "✔ Usaste " + skill.name() + " (-" + skill.manaCost() + " maná).", NamedTextColor.LIGHT_PURPLE));
            } else {
                player.sendMessage(Component.text(
                        "✔ Usaste " + skill.name() + " (-" + skill.manaCost()
                                + " maná), pero no había objetivo en rango.",
                        NamedTextColor.LIGHT_PURPLE));
            }

            playerManager.getCache().update(updatedPlayer);
            resourceBar.update(player, updatedPlayer);

        } catch (Exception exception) {

            player.sendMessage(Component.text("Error al usar la habilidad.", NamedTextColor.RED));
            exception.printStackTrace();

        }

    }

    private LivingEntity findTarget(Player player) {

        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                CAST_RANGE,
                entity -> entity instanceof LivingEntity && !entity.equals(player));

        if (result == null || !(result.getHitEntity() instanceof LivingEntity livingEntity)) {
            return null;
        }

        return livingEntity;

    }

    @Override
    public String getName() {
        return "use";
    }

    @Override
    public String getDescription() {
        return "Usa una habilidad aprendida";
    }

    @Override
    public String getUsage() {
        return "/rpg use <skillId>";
    }

    @Override
    public String getPermission() {
        return "rpgroll.player.useskill";
    }

    @Override
    public List<String> getAliases() {
        return List.of("usar", "cast");
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {

        if (args.length > 1 || !(sender instanceof Player player)) {
            return List.of();
        }

        try {
            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());
            return rpgPlayer.map(p -> List.copyOf(p.getSkills().getLearnedSkillIds())).orElse(List.of());
        } catch (Exception exception) {
            return List.of();
        }

    }

}
