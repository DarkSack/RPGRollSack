package com.sack.rpgroll.player.jobs;

/**
 * Progreso de un jugador en un trabajo específico.
 */
public record JobProgress(String jobId, int level, int experience) {

    public JobProgress {
        if (level < 1) {
            throw new IllegalArgumentException("level debe ser al menos 1");
        }
        if (experience < 0) {
            throw new IllegalArgumentException("experience no puede ser negativo");
        }
    }

    public static JobProgress start(String jobId) {
        return new JobProgress(jobId, 1, 0);
    }

}