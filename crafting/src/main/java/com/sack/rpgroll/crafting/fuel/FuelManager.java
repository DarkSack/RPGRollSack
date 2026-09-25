package com.sack.rpgroll.crafting.fuel;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.crafting.item.ItemIdentity;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Optional;

public class FuelManager extends ContentManager<FuelDefinition> {

    private final FuelDefinitionWriter writer;

    public FuelManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "fuels", "combustible", new FuelDefinitionParser());
        this.writer = new FuelDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(FuelDefinition fuel) {
        writer.save(fuel);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    /** Busca la primera definición de combustible que reconozca este ItemStack, si la hay. */
    public Optional<FuelDefinition> matching(ItemStack stack) {

        if (stack == null || stack.getType() == Material.AIR) {
            return Optional.empty();
        }

        for (FuelDefinition fuel : getAll()) {

            if (fuel.isCustomItem()) {
                if (ItemIdentity.matchesId(stack, fuel.materialOrItemId())) {
                    return Optional.of(fuel);
                }
                continue;
            }

            try {
                if (stack.getType() == Material.valueOf(fuel.materialOrItemId().trim().toUpperCase(Locale.ROOT))) {
                    return Optional.of(fuel);
                }
            } catch (IllegalArgumentException ignored) {
                // material inválido en la definición — se ignora, ya se avisó al cargar
            }
        }

        return Optional.empty();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(FuelManager.class);
    }

}
