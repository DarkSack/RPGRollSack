package com.sack.rpgroll.economy.company;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CompanyStore {

    private final File folder;

    public CompanyStore(File dataFolder) {
        this.folder = new File(dataFolder, "companies");
        this.folder.mkdirs();
    }

    public List<Company> loadAll() {

        List<Company> companies = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return companies;
        }

        for (File file : files) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            Company company = new Company(
                    UUID.fromString(config.getString("id")),
                    config.getString("name", "Empresa"),
                    UUID.fromString(config.getString("owner")),
                    UUID.fromString(config.getString("bank-account")));

            ConfigurationSection membersSection = config.getConfigurationSection("members");
            if (membersSection != null) {
                for (String memberId : membersSection.getKeys(false)) {
                    company.members().put(UUID.fromString(memberId),
                            CompanyRole.valueOf(membersSection.getString(memberId).toUpperCase(Locale.ROOT)));
                }
            }

            ConfigurationSection wagesSection = config.getConfigurationSection("wages");
            if (wagesSection != null) {
                for (String memberId : wagesSection.getKeys(false)) {
                    company.wages().put(UUID.fromString(memberId), wagesSection.getDouble(memberId));
                }
            }

            companies.add(company);
        }

        return companies;
    }

    public void save(Company company) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", company.id().toString());
        config.set("name", company.name());
        config.set("owner", company.ownerId().toString());
        config.set("bank-account", company.bankAccountId().toString());

        for (var entry : company.members().entrySet()) {
            config.set("members." + entry.getKey(), entry.getValue().name());
        }

        for (var entry : company.wages().entrySet()) {
            config.set("wages." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(new File(folder, company.id() + ".yml"));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la empresa " + company.id(), e);
        }
    }

    public void delete(UUID id) {
        new File(folder, id + ".yml").delete();
    }

}
