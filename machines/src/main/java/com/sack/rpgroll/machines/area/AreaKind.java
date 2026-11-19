package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.machines.core.Ui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/**
 * Un tipo de máquina de área (Pescadora, Granja, Leñadora). Lo común (ponerla, abrirla, guardarla,
 * el cofre, el claim, el crédito por velocidad) lo hace la base; el tipo solo dice qué hace con cada
 * acción que le toca.
 */
public abstract class AreaKind {

    private AreaSettings settings;

    /** Id en el ítem, en los datos y en {@code /machines give}: {@code fisher}, {@code farm}, {@code lumber}. */
    public abstract String id();

    /** Su fichero de configuración. */
    public abstract String file();

    /** Nombre de sus dibujos en el paquete de recursos (fondo, indicador): {@code pescadora}... */
    public abstract String art();

    public abstract Material defaultBlock();

    /** Sus mejoras con niveles, en el orden del menú (4 como mucho). */
    public abstract List<String> upgrades();

    /**
     * Hace hasta {@code budget} acciones (las que permita el crédito de la máquina) y devuelve
     * cuántas hizo. Pone el estado ({@code WORKING}, {@code IDLE}...) y el indicador de la máquina.
     */
    public abstract int work(AreaMachine machine, Block block, Player owner, int budget, AreaContext context);

    public AreaSettings settings() {
        return settings;
    }

    public void load(FileConfiguration config, Consumer<String> warn) {
        settings = AreaSettings.from(config, file(), defaultBlock(), upgrades(), warn);
        loadExtra(config, warn);
    }

    /** Lo propio del tipo en su fichero. */
    protected void loadExtra(FileConfiguration config, Consumer<String> warn) {
    }

    /** Lado del cuadrado que trabaja (el que tiene que estar en el claim del dueño). */
    public int side(AreaMachine machine) {
        return Math.max(1, (int) settings.value("area", machine));
    }

    /** Clave de idioma si no se puede poner en ese bloque (sin agua, por ejemplo), o null. */
    public String placeProblem(Block block) {
        return null;
    }

    /** Líneas propias en el estado de la máquina (agua, cultivos maduros...). */
    public List<Component> lore(AreaMachine machine, LangManager lang) {
        return List.of();
    }

    /** Cómo se lee el valor de una mejora en el menú. */
    public String format(String upgrade, double value, LangManager lang) {
        return Ui.number(value);
    }
}
