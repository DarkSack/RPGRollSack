package com.sack.rpgroll.magic.api;

import com.sack.rpgroll.magic.core.CatalystManager;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.SchoolManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellCatalyst;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.engine.CastResult;
import com.sack.rpgroll.magic.engine.SpellCastEngine;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;
import com.sack.rpgroll.magic.runtime.SpellbookManager;

import org.bukkit.entity.Player;

/**
 * Punto de entrada público de RPGRoll-Magic para cualquier otro addon: un
 * hechizo de encantamiento, una habilidad de mob, un objetivo de quest, o
 * simplemente otro plugin que quiera disparar un cast por código en vez de
 * depender del click del jugador.
 */
public final class MagicAPI {

    private static MagicAPI instance;

    private final SchoolManager schoolManager;
    private final SpellManager spellManager;
    private final GrimoireManager grimoireManager;
    private final RuneManager runeManager;
    private final CatalystManager catalystManager;
    private final SpellbookManager spellbookManager;
    private final SpellCastEngine engine;

    private MagicAPI(SchoolManager schoolManager, SpellManager spellManager, GrimoireManager grimoireManager,
            RuneManager runeManager, CatalystManager catalystManager, SpellbookManager spellbookManager,
            SpellCastEngine engine) {
        this.schoolManager = schoolManager;
        this.spellManager = spellManager;
        this.grimoireManager = grimoireManager;
        this.runeManager = runeManager;
        this.catalystManager = catalystManager;
        this.spellbookManager = spellbookManager;
        this.engine = engine;
    }

    public static void init(SchoolManager schoolManager, SpellManager spellManager, GrimoireManager grimoireManager,
            RuneManager runeManager, CatalystManager catalystManager, SpellbookManager spellbookManager,
            SpellCastEngine engine) {
        instance = new MagicAPI(schoolManager, spellManager, grimoireManager, runeManager, catalystManager,
                spellbookManager, engine);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-Magic todavía no está listo. */
    public static MagicAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-Magic todavía no está listo.");
        }

        return instance;
    }

    // ============ Casteo por código ============

    public CastResult cast(String spellId, Player caster) {
        return cast(spellId, caster, null);
    }

    public CastResult cast(String spellId, Player caster, SpellCatalyst catalyst) {

        var spellOpt = spellManager.get(spellId);

        if (spellOpt.isEmpty()) {
            return CastResult.failure(engine.lang().raw("cast.reason.unknown_spell", "id", spellId));
        }

        Spell spell = spellOpt.get();
        PlayerSpellbook spellbook = spellbookManager.getOrLoad(caster);

        return engine.cast(spell, caster, spellbook, catalyst);
    }

    // ============ Spellbook ============

    public boolean learn(Player player, String spellId) {

        if (!spellManager.exists(spellId)) {
            return false;
        }

        return spellbookManager.getOrLoad(player).learn(spellId);
    }

    public void forget(Player player, String spellId) {
        spellbookManager.getOrLoad(player).forget(spellId);
    }

    public boolean knows(Player player, String spellId) {
        return spellbookManager.getOrLoad(player).knows(spellId);
    }

    public boolean select(Player player, String spellId) {
        return spellbookManager.getOrLoad(player).select(spellId);
    }

    public boolean isOnCooldown(Player player, String spellId) {
        return spellbookManager.getOrLoad(player).isOnCooldown(spellId, System.currentTimeMillis());
    }

    // ============ Acceso interno (GUI/comandos del propio addon) ============

    public SchoolManager getSchoolManager() {
        return schoolManager;
    }

    public SpellManager getSpellManager() {
        return spellManager;
    }

    public GrimoireManager getGrimoireManager() {
        return grimoireManager;
    }

    public RuneManager getRuneManager() {
        return runeManager;
    }

    public CatalystManager getCatalystManager() {
        return catalystManager;
    }

    public SpellbookManager getSpellbookManager() {
        return spellbookManager;
    }

    public SpellCastEngine getEngine() {
        return engine;
    }

}
