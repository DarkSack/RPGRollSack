package com.sack.rpgroll.structures.workbench;

/** Estado de la mesa. Sale de la obra activa ({@code ProjectService.benchState}); sin obra es {@code IDLE}. */
public enum WorkbenchState { IDLE, WAITING_MATERIALS, READY, BUILDING, PAUSED, COMPLETED, ERROR }
