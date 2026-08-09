package com.sack.rpgroll.economy.company;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CompanyManager {

    private final CompanyStore store;
    private final Map<UUID, Company> companies = new ConcurrentHashMap<>();

    public CompanyManager(CompanyStore store) {
        this.store = store;
    }

    public void loadAll() {
        companies.clear();
        for (Company company : store.loadAll()) {
            companies.put(company.id(), company);
        }
    }

    public void saveAll() {
        companies.values().forEach(store::save);
    }

    public void register(Company company) {
        companies.put(company.id(), company);
        store.save(company);
    }

    public void save(Company company) {
        store.save(company);
    }

    public void delete(UUID id) {
        companies.remove(id);
        store.delete(id);
    }

    public Optional<Company> get(UUID id) {
        return Optional.ofNullable(companies.get(id));
    }

    public Optional<Company> byName(String name) {
        return companies.values().stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst();
    }

    public List<Company> byMember(UUID playerId) {

        List<Company> result = new ArrayList<>();

        for (Company company : companies.values()) {
            if (company.isMember(playerId)) {
                result.add(company);
            }
        }

        return result;
    }

    public java.util.Collection<Company> all() {
        return companies.values();
    }

}
