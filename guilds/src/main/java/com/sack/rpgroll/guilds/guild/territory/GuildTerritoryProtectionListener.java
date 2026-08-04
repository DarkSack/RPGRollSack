package com.sack.rpgroll.guilds.guild.territory;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

/**
 * Protección básica de territorios reclamados — spec: territorio propio
 * (sin WorldGuard). Bloquea romper/colocar bloques de no-miembros si
 * {@code protectBlocks} está activo, y PvP de no-miembros salvo que
 * {@code allowOutsiderPvp} esté habilitado.
 */
public class GuildTerritoryProtectionListener implements Listener {

    private final GuildManager guildManager;

    public GuildTerritoryProtectionListener(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    private Optional<GuildTerritory> territoryAt(org.bukkit.Location location) {
        for (Guild guild : guildManager.getAll()) {
            for (GuildTerritory territory : guild.territories()) {
                if (territory.contains(location)) {
                    return Optional.of(territory);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Guild> ownerOf(GuildTerritory territory) {
        return guildManager.getAll().stream()
                .filter(guild -> guild.territories().stream().anyMatch(t -> t == territory))
                .findFirst();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        territoryAt(event.getBlock().getLocation()).ifPresent(territory -> {
            if (territory.protectBlocks() && !isMemberOfOwner(territory, event.getPlayer())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(
                        Component.text("Este territorio está protegido por una guild.", NamedTextColor.RED));
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {

        territoryAt(event.getBlock().getLocation()).ifPresent(territory -> {
            if (territory.protectBlocks() && !isMemberOfOwner(territory, event.getPlayer())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(
                        Component.text("Este territorio está protegido por una guild.", NamedTextColor.RED));
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }

        territoryAt(victim.getLocation()).ifPresent(territory -> {
            if (!territory.allowOutsiderPvp() && !isMemberOfOwner(territory, attacker)) {
                event.setCancelled(true);
                attacker.sendMessage(Component.text("No podés atacar jugadores en este territorio.",
                        NamedTextColor.RED));
            }
        });
    }

    private boolean isMemberOfOwner(GuildTerritory territory, Player player) {
        return ownerOf(territory)
                .map(guild -> guild.isMember(player.getUniqueId()))
                .orElse(false);
    }

}
