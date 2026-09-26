package com.sack.rpgroll.economy.servershop;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.List;

/** Las categorías de {@code server-shop/}. */
public class ServerShopManager extends ContentManager<ServerShopCategory> {

    public ServerShopManager(JavaPlugin economyPlugin) {
        super(owningPlugin(), new YamlLoader(economyPlugin), "server-shop", "categoría de tienda",
                new ServerShopCategoryParser(economyPlugin.getLogger()::warning));
    }

    /** En el orden en que se colocan en el menú. */
    public List<ServerShopCategory> ordered() {
        return getAll().stream()
                .sorted(Comparator.comparingInt((ServerShopCategory c) -> c.slot() < 0 ? Integer.MAX_VALUE : c.slot())
                        .thenComparing(ServerShopCategory::id))
                .toList();
    }

    @Override
    protected boolean optionalContent() {
        return true;
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(ServerShopManager.class);
    }

}
