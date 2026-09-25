package com.sack.rpgroll.common;

import com.sack.rpgroll.gui.listener.GUIListener;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * RPGRoll-Lib: lo que comparten todos los plugins de RPGRoll (idiomas,
 * carga de contenido YAML, comandos, menús, utilidades de texto e ítems).
 * <p>
 * Es gratuito y no verifica licencia: cada módulo comprueba la suya con el
 * código de licencia que lleva DENTRO de su propio jar. Si la verificación
 * viviera aquí, cambiar este único jar por uno modificado desbloquearía
 * todos los módulos a la vez.
 * <p>
 * Un solo {@link GUIListener} para todo el ecosistema: el registro de menús
 * abiertos es estático y tiene que ser uno, lo abra el módulo que lo abra.
 */
public class RPGRollLib extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getLogger().info("✔ RPGRoll-Lib " + getPluginMeta().getVersion() + " listo.");
    }

}
