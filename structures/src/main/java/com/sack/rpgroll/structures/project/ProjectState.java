package com.sack.rpgroll.structures.project;

/** Estado de una obra. COMPLETED y CANCELLED son finales; el resto cuenta como obra activa. */
public enum ProjectState {
    WAITING_MATERIALS, READY, BUILDING, PAUSED, COMPLETED, CANCELLED, ERROR
}
