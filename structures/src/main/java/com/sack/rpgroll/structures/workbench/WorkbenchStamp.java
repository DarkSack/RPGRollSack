package com.sack.rpgroll.structures.workbench;

import java.util.UUID;

/** Identidad firmada de una mesa tal como viaja en el ítem. */
public record WorkbenchStamp(UUID id, WorkbenchTier tier, long serial, String signature) {}
