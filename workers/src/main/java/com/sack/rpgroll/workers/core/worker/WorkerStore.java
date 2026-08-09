package com.sack.rpgroll.workers.core.worker;

import com.sack.rpgroll.workers.core.economy.WageType;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Persiste cada {@link Worker} en su propio archivo bajo {@code workers/<uuid>.yml}. */
public class WorkerStore {

    private final Plugin plugin;
    private final File folder;

    public WorkerStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "workers");
    }

    public List<Worker> loadAll() {

        List<Worker> workers = new ArrayList<>();

        if (!folder.isDirectory()) {
            return workers;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return workers;
        }

        for (File file : files) {
            try {
                workers.add(load(YamlConfiguration.loadConfiguration(file)));
            } catch (Exception e) {
                plugin.getLogger().warning("✘ No se pudo leer el worker '" + file.getName() + "': " + e.getMessage());
            }
        }

        return workers;
    }

    public void save(Worker worker) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", worker.id().toString());
        config.set("profession", worker.professionId());
        config.set("custom-name", worker.customName());
        config.set("personality", worker.personality().name());

        for (var entry : worker.skillLevels().entrySet()) {
            config.set("skill-levels." + entry.getKey(), entry.getValue());
        }

        for (String skillId : worker.skillLevels().keySet()) {
            config.set("skill-experience." + skillId, worker.skillExperience(skillId));
        }

        config.set("needs.hunger", worker.hunger());
        config.set("needs.energy", worker.energy());
        config.set("needs.sleep", worker.sleep());
        config.set("needs.stress", worker.stress());
        config.set("needs.motivation", worker.motivation());
        config.set("needs.health", worker.health());
        config.set("needs.happiness", worker.happiness());

        for (var entry : worker.carriedItems().entrySet()) {
            config.set("carried-items." + entry.getKey(), entry.getValue());
        }

        setLocation(config, "home", worker.homeLocation());

        config.set("employer", worker.employerId() == null ? null : worker.employerId().toString());
        config.set("wage-amount", worker.wageAmount());
        config.set("wage-type", worker.wageType().name());
        config.set("wage-payment-remaining-ticks", worker.wagePaymentRemainingTicks());

        config.set("active-event", worker.activeWorkerEventId());
        config.set("event-remaining-ticks", worker.eventRemainingTicks());
        config.set("event-work-speed-multiplier", worker.eventWorkSpeedMultiplier());

        try {
            folder.mkdirs();
            config.save(new File(folder, worker.id() + ".yml"));
        } catch (Exception e) {
            plugin.getLogger().warning("✘ No se pudo guardar el worker " + worker.id() + ": " + e.getMessage());
        }
    }

    public void delete(UUID id) {
        File file = new File(folder, id + ".yml");
        if (file.isFile()) {
            file.delete();
        }
    }

    private Worker load(YamlConfiguration config) {

        UUID id = UUID.fromString(config.getString("id"));
        String professionId = config.getString("profession");

        PersonalityTrait personality;
        try {
            personality = PersonalityTrait.valueOf(config.getString("personality", "RESPONSIBLE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            personality = PersonalityTrait.RESPONSIBLE;
        }

        Worker worker = new Worker(id, professionId, personality);
        worker.setCustomName(config.getString("custom-name"));

        ConfigurationSection levelsSection = config.getConfigurationSection("skill-levels");
        ConfigurationSection experienceSection = config.getConfigurationSection("skill-experience");

        if (levelsSection != null) {
            for (String skillId : levelsSection.getKeys(false)) {
                worker.skillLevels().put(skillId, levelsSection.getInt(skillId));
            }
        }

        if (experienceSection != null) {
            for (String skillId : experienceSection.getKeys(false)) {
                worker.skillExperienceMap().put(skillId, experienceSection.getDouble(skillId));
            }
        }

        worker.setHunger(config.getDouble("needs.hunger", 100));
        worker.setEnergy(config.getDouble("needs.energy", 100));
        worker.setSleep(config.getDouble("needs.sleep", 100));
        worker.setStress(config.getDouble("needs.stress", 0));
        worker.setMotivation(config.getDouble("needs.motivation", 80));
        worker.setHealth(config.getDouble("needs.health", 100));
        worker.setHappiness(config.getDouble("needs.happiness", 80));

        ConfigurationSection carriedSection = config.getConfigurationSection("carried-items");
        if (carriedSection != null) {
            for (String materialName : carriedSection.getKeys(false)) {
                worker.addCarried(materialName, carriedSection.getInt(materialName));
            }
        }

        worker.setHomeLocation(loadLocation(config, "home"));

        String employerRaw = config.getString("employer");
        UUID employerId = employerRaw == null || employerRaw.isBlank() ? null : UUID.fromString(employerRaw);

        if (employerId != null) {
            WageType wageType;
            try {
                wageType = WageType.valueOf(config.getString("wage-type", "PER_TASK").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                wageType = WageType.PER_TASK;
            }
            worker.hire(employerId, config.getDouble("wage-amount", 0), wageType);
            worker.setWagePaymentRemainingTicks(config.getLong("wage-payment-remaining-ticks", 0));
        }

        String activeEvent = config.getString("active-event");
        if (activeEvent != null && !activeEvent.isBlank()) {
            worker.triggerEvent(activeEvent, config.getLong("event-remaining-ticks", 0),
                    config.getDouble("event-work-speed-multiplier", 1.0));
        }

        return worker;
    }

    private void setLocation(YamlConfiguration config, String path, Location location) {

        if (location == null || location.getWorld() == null) {
            config.set(path, null);
            return;
        }

        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private Location loadLocation(YamlConfiguration config, String path) {

        ConfigurationSection section = config.getConfigurationSection(path);

        if (section == null) {
            return null;
        }

        World world = Bukkit.getWorld(section.getString("world", ""));

        if (world == null) {
            return null;
        }

        return new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }

}
