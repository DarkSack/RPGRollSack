package com.sack.rpgroll.worldforge.generation;

import com.sack.rpgroll.worldforge.api.Capability;
import com.sack.rpgroll.worldforge.profile.Profile;

import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;

import java.util.Set;

/**
 * One way of generating a world. {@link #capabilities()} is a promise: every capability
 * listed was tested on Paper 26.1.2 (docs/FEASIBILITY.md); profile fields that need
 * anything else are rejected by the validator.
 */
public interface GenerationProvider {

    String id();

    String description();

    Set<Capability> capabilities();

    /** The generator for world {@code world} with this profile, or null to leave Minecraft's own. */
    ChunkGenerator generator(String world, Profile profile);

    /** Applies the profile to a creator for a new world (the generator included). */
    WorldCreator configure(WorldCreator creator, Profile profile);
}
