package com.sack.rpgroll.structures.workbench;

/** Estado de la mesa. Hasta el plan 3 toda mesa está en {@code IDLE}. */
public enum WorkbenchState { IDLE, WAITING_MATERIALS, READY, BUILDING, PAUSED, COMPLETED, ERROR }
