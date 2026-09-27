package com.sack.rpgroll.workers.core.behavior;

import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.integration.GuildsIntegration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * Dónde trabaja un worker y qué bloques puede tocar. Busca alrededor de su
 * hogar y no de donde se haya ido caminando (si no, veta a veta se alejaba
 * sin límite y acababa picando bases ajenas), solo mira chunks ya cargados,
 * y pregunta antes de cambiar un bloque.
 */
final class WorkSite {

    private static final BlockFace[] FACES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST };

    private WorkSite() {
    }

    /** Centro de búsqueda: el hogar si está en este mundo; si no, donde está parado. */
    static Location anchor(Worker worker, LivingEntity entity) {

        Location home = worker.homeLocation();

        if (home != null && entity.getWorld().equals(home.getWorld())) {
            return home;
        }

        return entity.getLocation();
    }

    /** getBlockAt en un chunk sin cargar lo carga de forma síncrona: los barridos se lo saltan. */
    static boolean isLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4);
    }

    /** Alguna cara da a un hueco: lo que está enterrado no se pica a través de la pared. */
    static boolean isExposed(Block block) {

        for (BlockFace face : FACES) {

            Block neighbour = block.getRelative(face);

            if (isLoaded(block.getWorld(), neighbour.getX(), neighbour.getZ()) && neighbour.isPassable()) {
                return true;
            }
        }

        return false;
    }

    /** Los territorios protegidos de un gremio solo los trabajan los workers de sus miembros. */
    static boolean mayWorkAt(Worker worker, Block block) {
        return GuildsIntegration.mayWorkAt(worker.employerId(), block.getLocation());
    }

    /**
     * El mismo aviso que dan los aldeanos al cosechar o los endermans al
     * coger un bloque: WorldGuard puede cancelarlo y CoreProtect lo registra.
     */
    static boolean mayChange(Worker worker, LivingEntity entity, Block block, BlockData to) {

        if (!mayWorkAt(worker, block)) {
            return false;
        }

        EntityChangeBlockEvent event = new EntityChangeBlockEvent(entity, block, to);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

}
