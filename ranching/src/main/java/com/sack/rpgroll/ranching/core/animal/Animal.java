package com.sack.rpgroll.ranching.core.animal;

import com.sack.rpgroll.ranching.core.genetics.AllelePair;
import com.sack.rpgroll.ranching.core.genetics.AncestorRef;
import com.sack.rpgroll.ranching.core.species.GrowthStage;
import com.sack.rpgroll.ranching.core.species.Sex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Un animal puntual — el estado mutable de toda la simulación (genética ya
 * resuelta, edad, salud, felicidad, embarazo, enfermedad). La definición de
 * su especie/raza vive aparte, en {@code SpeciesManager}/{@code
 * BreedManager}; este objeto solo guarda el id de cuál le corresponde.
 * <p>
 * {@code id} es el MISMO uuid que la entidad de Bukkit real que representa
 * a este animal en el mundo — no hay una tabla de mapeo separada.
 */
public class Animal {

    private final UUID id;
    private final String speciesId;
    private String breedId;
    private final Sex sex;
    private final Map<String, AllelePair> genotype;
    private final Map<String, Double> phenotype;
    private final List<String> mutationTags;
    private final UUID motherId;
    private final UUID fatherId;
    private final List<AncestorRef> ancestors;
    private final int generation;
    private final long bornAtEpochMillis;

    private GrowthStage stage = GrowthStage.BABY;
    private long ageTicks;
    private double weight;
    private double fertility;
    private double happiness = 80;
    private double health = 100;

    private boolean pregnant;
    private long pregnancyRemainingTicks;
    private List<PendingOffspring> pendingLitter = new ArrayList<>();

    private String activeDiseaseId;
    private long diseaseRemainingTicks;
    private final Map<String, Long> diseaseImmunityRemainingTicks = new HashMap<>();
    private final Map<String, Double> diseaseRiskMultiplier = new HashMap<>();

    private double productionBonusAccumulator;
    private AnimalQuality quality = AnimalQuality.COMMON;

    public Animal(UUID id, String speciesId, String breedId, Sex sex, Map<String, AllelePair> genotype,
            Map<String, Double> phenotype, List<String> mutationTags, UUID motherId, UUID fatherId,
            List<AncestorRef> ancestors, int generation, double weight, long bornAtEpochMillis) {
        this.id = id;
        this.speciesId = speciesId;
        this.breedId = breedId;
        this.sex = sex;
        this.genotype = new HashMap<>(genotype);
        this.phenotype = new HashMap<>(phenotype);
        this.mutationTags = new ArrayList<>(mutationTags);
        this.motherId = motherId;
        this.fatherId = fatherId;
        this.ancestors = new ArrayList<>(ancestors);
        this.generation = generation;
        this.weight = weight;
        this.bornAtEpochMillis = bornAtEpochMillis;
    }

    public UUID id() {
        return id;
    }

    public String speciesId() {
        return speciesId;
    }

    public String breedId() {
        return breedId;
    }

    public void setBreedId(String breedId) {
        this.breedId = breedId;
    }

    public Sex sex() {
        return sex;
    }

    public Map<String, AllelePair> genotype() {
        return genotype;
    }

    public Map<String, Double> phenotype() {
        return phenotype;
    }

    public double phenotypeValue(String geneId, double fallback) {
        return phenotype.getOrDefault(geneId, fallback);
    }

    public List<String> mutationTags() {
        return mutationTags;
    }

    public UUID motherId() {
        return motherId;
    }

    public UUID fatherId() {
        return fatherId;
    }

    public boolean isFounder() {
        return motherId == null && fatherId == null;
    }

    public List<AncestorRef> ancestors() {
        return ancestors;
    }

    public int generation() {
        return generation;
    }

    public long bornAtEpochMillis() {
        return bornAtEpochMillis;
    }

    public GrowthStage stage() {
        return stage;
    }

    public void setStage(GrowthStage stage) {
        this.stage = stage;
    }

    public long ageTicks() {
        return ageTicks;
    }

    public void addAge(long ticks) {
        this.ageTicks += ticks;
    }

    public double weight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = Math.max(0.1, weight);
    }

    public double fertility() {
        return fertility;
    }

    public void setFertility(double fertility) {
        this.fertility = Math.max(0, Math.min(1, fertility));
    }

    public double happiness() {
        return happiness;
    }

    public void setHappiness(double happiness) {
        this.happiness = Math.max(0, Math.min(100, happiness));
    }

    public double health() {
        return health;
    }

    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(100, health));
    }

    public boolean isPregnant() {
        return pregnant;
    }

    public void startPregnancy(long durationTicks, List<PendingOffspring> litter) {
        this.pregnant = true;
        this.pregnancyRemainingTicks = durationTicks;
        this.pendingLitter = new ArrayList<>(litter);
    }

    public long pregnancyRemainingTicks() {
        return pregnancyRemainingTicks;
    }

    /** @return true si con esta reducción el embarazo llegó a término. */
    public boolean advancePregnancy(long ticks) {

        if (!pregnant) {
            return false;
        }

        pregnancyRemainingTicks -= ticks;
        return pregnancyRemainingTicks <= 0;
    }

    public List<PendingOffspring> finishPregnancy() {

        List<PendingOffspring> litter = pendingLitter;
        this.pregnant = false;
        this.pregnancyRemainingTicks = 0;
        this.pendingLitter = new ArrayList<>();

        return litter;
    }

    public boolean isSick() {
        return activeDiseaseId != null;
    }

    public String activeDiseaseId() {
        return activeDiseaseId;
    }

    public long diseaseRemainingTicks() {
        return diseaseRemainingTicks;
    }

    public void infect(String diseaseId, long durationTicks) {
        this.activeDiseaseId = diseaseId;
        this.diseaseRemainingTicks = durationTicks;
    }

    public void reduceDiseaseDuration(long ticks) {
        this.diseaseRemainingTicks = Math.max(0, diseaseRemainingTicks - ticks);
    }

    public void cure() {
        this.activeDiseaseId = null;
        this.diseaseRemainingTicks = 0;
    }

    public Map<String, Long> diseaseImmunityRemainingTicks() {
        return diseaseImmunityRemainingTicks;
    }

    /** @param riskMultiplier 0-1, multiplica hacia abajo la chance de contagio/aparición mientras dure la inmunidad. */
    public void grantImmunity(String diseaseId, long durationTicks, double riskMultiplier) {
        diseaseImmunityRemainingTicks.put(diseaseId, durationTicks <= 0 ? Long.MAX_VALUE : durationTicks);
        diseaseRiskMultiplier.put(diseaseId, riskMultiplier);
    }

    public boolean isImmuneTo(String diseaseId) {
        return diseaseImmunityRemainingTicks.getOrDefault(diseaseId, 0L) > 0;
    }

    /** 1.0 = sin protección, hacia 0 = inmunidad casi total. */
    public double riskMultiplierFor(String diseaseId) {
        return isImmuneTo(diseaseId) ? diseaseRiskMultiplier.getOrDefault(diseaseId, 1.0) : 1.0;
    }

    public Map<String, Double> diseaseRiskMultipliers() {
        return diseaseRiskMultiplier;
    }

    public void addProductionBonus(double bonus) {
        this.productionBonusAccumulator += bonus;
    }

    /** Consume y devuelve el bono acumulado (feeds recientes) — se resetea a 0 tras leerlo. */
    public double consumeProductionBonus() {
        double value = productionBonusAccumulator;
        productionBonusAccumulator = 0;
        return value;
    }

    public AnimalQuality quality() {
        return quality;
    }

    public void setQuality(AnimalQuality quality) {
        this.quality = quality;
    }

    public void tickImmunities(long ticks) {

        for (String diseaseId : List.copyOf(diseaseImmunityRemainingTicks.keySet())) {

            long remaining = diseaseImmunityRemainingTicks.get(diseaseId);

            if (remaining == Long.MAX_VALUE) {
                continue;
            }

            remaining -= ticks;

            if (remaining <= 0) {
                diseaseImmunityRemainingTicks.remove(diseaseId);
            } else {
                diseaseImmunityRemainingTicks.put(diseaseId, remaining);
            }
        }
    }

}
