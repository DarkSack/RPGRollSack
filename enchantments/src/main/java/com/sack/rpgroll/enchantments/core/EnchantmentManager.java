package com.sack.rpgroll.enchantments.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carga los encantamientos desde plugins/RPGRoll-Enchantments/enchantments/*.yml
 * usando el framework genérico de contenido de :common (mismo patrón que
 * CrateManager/NpcManager), y además permite registrar encantamientos
 * directamente por código — la filosofía del addon es que TODO pueda
 * definirse por YAML, pero un plugin externo también pueda sumar los suyos
 * llamando a {@link #register(CustomEnchantment)} sin tocar archivos.
 */
public class EnchantmentManager extends ContentManager<CustomEnchantment> {

    private final Map<String, CustomEnchantment> apiEnchantments = new LinkedHashMap<>();
    private final EnchantmentDefinitionWriter writer;

    public EnchantmentManager(JavaPlugin enchantmentsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(enchantmentsPlugin), "enchantments", "encantamiento",
                new EnchantmentParser());
        this.writer = new EnchantmentDefinitionWriter(new File(enchantmentsPlugin.getDataFolder(), "enchantments"),
                enchantmentsPlugin.getLogger());
    }

    /** Persiste el encantamiento a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(CustomEnchantment enchantment) {
        writer.save(enchantment);
        reload();
    }

    /**
     * Registra un encantamiento definido en código. A diferencia del
     * contenido leído de YAML, sobrevive a {@link #reload()} — no se pierde
     * al recargar los archivos de disco.
     */
    public void register(CustomEnchantment enchantment) {
        apiEnchantments.put(enchantment.id(), enchantment);
    }

    @Override
    public Optional<CustomEnchantment> get(String id) {

        Optional<CustomEnchantment> fromApi = Optional.ofNullable(apiEnchantments.get(id));
        return fromApi.isPresent() ? fromApi : super.get(id);
    }

    @Override
    public boolean exists(String id) {
        return apiEnchantments.containsKey(id) || super.exists(id);
    }

    @Override
    public Collection<CustomEnchantment> getAll() {

        List<CustomEnchantment> combined = new ArrayList<>(super.getAll());

        for (CustomEnchantment enchantment : apiEnchantments.values()) {
            if (!super.exists(enchantment.id())) {
                combined.add(enchantment);
            }
        }

        return combined;
    }

    @Override
    public int count() {
        return getAll().size();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
