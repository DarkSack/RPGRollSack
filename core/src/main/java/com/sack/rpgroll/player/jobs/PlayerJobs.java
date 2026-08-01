package com.sack.rpgroll.player.jobs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Progreso inmutable de todos los trabajos activos de un jugador.
 * Máximo 3 trabajos simultáneos.
 */
public record PlayerJobs(Map<String, JobProgress> activeJobs) {

    public static final int MAX_ACTIVE_JOBS = 3;

    public PlayerJobs {
        activeJobs = activeJobs == null ? Map.of() : Map.copyOf(activeJobs);
    }

    public static PlayerJobs empty() {
        return new PlayerJobs(Map.of());
    }

    public boolean hasJob(String jobId) {
        return activeJobs.containsKey(jobId);
    }

    public boolean isFull() {
        return activeJobs.size() >= MAX_ACTIVE_JOBS;
    }

    public int count() {
        return activeJobs.size();
    }

    public Set<String> getActiveJobIds() {
        return activeJobs.keySet();
    }

    public Optional<JobProgress> getProgress(String jobId) {
        return Optional.ofNullable(activeJobs.get(jobId));
    }

    public int getLevel(String jobId) {
        return getProgress(jobId).map(JobProgress::level).orElse(0);
    }

    public int getExperience(String jobId) {
        return getProgress(jobId).map(JobProgress::experience).orElse(0);
    }

    /**
     * Une al jugador a un nuevo trabajo. No valida el máximo aquí —
     * esa validación de negocio vive en el caller (comando/GUI), que
     * decide qué mensaje mostrar si ya está lleno.
     *
     * @return nueva instancia, o la misma si ya tenía ese trabajo
     */
    public PlayerJobs join(String jobId) {

        if (hasJob(jobId)) {
            return this;
        }

        Map<String, JobProgress> updated = new LinkedHashMap<>(activeJobs);
        updated.put(jobId, JobProgress.start(jobId));

        return new PlayerJobs(updated);
    }

    /**
     * Abandona un trabajo, perdiendo su progreso.
     *
     * @return nueva instancia, o la misma si no tenía ese trabajo
     */
    public PlayerJobs leave(String jobId) {

        if (!hasJob(jobId)) {
            return this;
        }

        Map<String, JobProgress> updated = new LinkedHashMap<>(activeJobs);
        updated.remove(jobId);

        return new PlayerJobs(updated);
    }

    /**
     * Reemplaza el progreso de un trabajo específico (usado tras un level up
     * o al sumar experiencia). El trabajo debe existir ya en activeJobs.
     */
    public PlayerJobs withProgress(JobProgress progress) {

        if (!hasJob(progress.jobId())) {
            return this;
        }

        Map<String, JobProgress> updated = new LinkedHashMap<>(activeJobs);
        updated.put(progress.jobId(), progress);

        return new PlayerJobs(updated);
    }

}