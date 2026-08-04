package com.sack.rpgroll.quests.registry;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.quests.engine.QuestEngine;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Locale;

/**
 * Registra los tipos de acción base que el motor de quests trae listos
 * para usar en eventos (on-start, on-complete, etc.) y en recompensas
 * "extra". Un addon puede sumar los suyos vía
 * {@code ActionRegistry#register(String, QuestActionHandler)} sin tocar
 * esta clase.
 */
public final class BuiltinActions {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private BuiltinActions() {
    }

    public static void registerAll(ActionRegistry registry, Plugin plugin, QuestEngine engine) {

        registry.register("MESSAGE", (action, ctx) -> {
            String message = action.param("value", "");
            if (!message.isBlank()) {
                ctx.player().sendMessage(LEGACY.deserialize(applyPlaceholders(message, ctx.player())));
            }
        });

        registry.register("SOUND", (action, ctx) -> {

            try {
                Sound sound = Sound.valueOf(action.param("sound", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase(Locale.ROOT));
                float volume = Float.parseFloat(action.param("volume", "1.0"));
                float pitch = Float.parseFloat(action.param("pitch", "1.0"));
                ctx.player().playSound(ctx.player().getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ SOUND inválido en acción de quest: " + action.param("sound", ""));
            }
        });

        registry.register("PARTICLE", (action, ctx) -> {

            try {
                Particle particle = Particle.valueOf(action.param("particle", "FLAME").toUpperCase(Locale.ROOT));
                int count = Integer.parseInt(action.param("count", "20"));
                Location location = ctx.player().getLocation();
                location.getWorld().spawnParticle(particle, location, count, 0.3, 0.5, 0.3, 0.01);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ PARTICLE inválida en acción de quest: " + action.param("particle", ""));
            }
        });

        registry.register("TITLE", (action, ctx) -> {

            Component main = LEGACY.deserialize(action.param("title", ""));
            Component subtitle = LEGACY.deserialize(action.param("subtitle", ""));

            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500));
            ctx.player().showTitle(Title.title(main, subtitle, times));
        });

        registry.register("BOSSBAR", (action, ctx) -> {

            Component text = LEGACY.deserialize(action.param("text", ""));
            BossBar bossBar = BossBar.bossBar(text, 1.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);

            ctx.player().showBossBar(bossBar);

            long durationTicks = Long.parseLong(action.param("duration-ticks", "100"));
            new BukkitRunnable() {
                @Override
                public void run() {
                    ctx.player().hideBossBar(bossBar);
                }
            }.runTaskLater(plugin, durationTicks);
        });

        registry.register("COMMAND", (action, ctx) -> {
            String command = action.param("value", "");
            if (!command.isBlank()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), applyPlaceholders(command, ctx.player()));
            }
        });

        registry.register("GIVE_EXP", (action, ctx) -> {

            if (!RPGRollAPI.isReady()) {
                return;
            }

            int amount = Integer.parseInt(action.param("amount", "0"));
            RPGRollAPI api = RPGRollAPI.get();

            api.getPlayer(ctx.player().getUniqueId()).ifPresent(rpgPlayer -> {
                RPGPlayer updated = rpgPlayer.addExperience(amount);
                api.getPlayerManager().savePlayer(updated);
            });
        });

        registry.register("GIVE_MONEY", (action, ctx) -> {

            if (!RPGRollAPI.isReady()) {
                return;
            }

            double amount = Double.parseDouble(action.param("amount", "0"));
            RPGRollAPI.get().getEconomyProvider().getEconomy()
                    .ifPresent(economy -> economy.depositPlayer(ctx.player(), amount));
        });

        registry.register("GIVE_ITEM", (action, ctx) -> {

            org.bukkit.Material material;

            try {
                material = org.bukkit.Material.valueOf(action.param("material", "").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ GIVE_ITEM inválido en acción de quest: " + action.param("material", ""));
                return;
            }

            int amount = Integer.parseInt(action.param("amount", "1"));
            ItemStack item = new ItemStack(material, amount);

            var leftover = ctx.player().getInventory().addItem(item);
            leftover.values().forEach(remaining ->
                    ctx.player().getWorld().dropItemNaturally(ctx.player().getLocation(), remaining));
        });

        registry.register("TELEPORT", (action, ctx) -> {

            String world = action.param("world", ctx.player().getWorld().getName());
            var target = Bukkit.getWorld(world);

            if (target == null) {
                return;
            }

            double x = Double.parseDouble(action.param("x", "0"));
            double y = Double.parseDouble(action.param("y", "0"));
            double z = Double.parseDouble(action.param("z", "0"));

            ctx.player().teleport(new Location(target, x, y, z));
        });

        registry.register("START_QUEST", (action, ctx) -> {
            String questId = action.param("value", "");
            engine.getQuestManager().get(questId).ifPresent(nextQuest -> engine.startQuest(ctx.player(), nextQuest));
        });

        registry.register("COMPLETE_QUEST", (action, ctx) -> {
            // Sin atajo directo "forzar completar": se completa avanzando sus objetivos.
            // Documentado como punto de extensión — un addon puede registrar su propia
            // lógica de "completar instantáneamente" si la necesita.
            plugin.getLogger().info("… COMPLETE_QUEST no fuerza progreso automáticamente, solo informativo.");
        });

        registry.register("UNLOCK_PROFESSION", (action, ctx) -> {

            if (!RPGRollAPI.isReady()) {
                return;
            }

            String jobId = action.param("value", "");
            RPGRollAPI api = RPGRollAPI.get();

            api.getPlayer(ctx.player().getUniqueId()).ifPresent(rpgPlayer -> {
                RPGPlayer updated = rpgPlayer.joinJob(jobId);
                api.getPlayerManager().savePlayer(updated);
            });
        });

        registry.register("UNLOCK_SKILL", (action, ctx) -> {

            if (!RPGRollAPI.isReady()) {
                return;
            }

            String skillId = action.param("value", "");
            RPGRollAPI api = RPGRollAPI.get();

            api.getPlayer(ctx.player().getUniqueId()).ifPresent(rpgPlayer -> {
                RPGPlayer updated = rpgPlayer.learnSkill(skillId);
                api.getPlayerManager().savePlayer(updated);
            });
        });
    }

    private static String applyPlaceholders(String raw, Player player) {
        return raw.replace("{player}", player.getName());
    }

}
