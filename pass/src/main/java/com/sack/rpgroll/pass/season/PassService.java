package com.sack.rpgroll.pass.season;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.pass.PassClock;
import com.sack.rpgroll.pass.player.PassPlayer;
import com.sack.rpgroll.pass.player.PassPlayerStore;
import com.sack.rpgroll.pass.requirement.RequirementService;
import com.sack.rpgroll.pass.requirement.RequirementStatus;
import com.sack.rpgroll.pass.reward.Reward;
import com.sack.rpgroll.pass.reward.RewardService;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Puntos de pase, niveles y reclamo de las dos pistas de la temporada activa. */
public class PassService {

    public static final String PREMIUM_PERMISSION = "rpgroll.pass.premium";

    public enum ClaimResult {
        CLAIMED, CLOSED, LOCKED, REQUIREMENTS, ALREADY_CLAIMED, NEEDS_PREMIUM, NOTHING
    }

    private final PassPlayerStore store;
    private final RewardService rewards;
    private final LangManager lang;
    private final PassClock clock;
    private final RequirementService requirements;

    private Season season;
    private Consumer<PassPlayer> onSeasonStart = player -> {
    };

    public PassService(PassPlayerStore store, RewardService rewards, LangManager lang, PassClock clock) {
        this.store = store;
        this.rewards = rewards;
        this.lang = lang;
        this.clock = clock;
        this.requirements = new RequirementService(lang, clock);
    }

    /** Estado de cada requisito del nivel para este jugador (vacío si el nivel no exige nada). */
    public List<RequirementStatus> requirements(Player player, SeasonLevel level) {
        return season().map(s -> requirements.evaluate(player, player(player), s, level.requirements()))
                .orElse(List.of());
    }

    public boolean meetsRequirements(Player player, SeasonLevel level) {
        return requirements(player, level).stream().allMatch(RequirementStatus::met);
    }

    /** Suma minutos jugados de la temporada (los usa el requisito playtime). */
    public void addPlaytime(Player player, int minutes) {
        if (openSeason().isPresent()) {
            player(player).addPlaytime(minutes);
        }
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    /** Qué más hay que reiniciar cuando un jugador empieza una temporada nueva (sus misiones de temporada). */
    public void onSeasonStart(Consumer<PassPlayer> onSeasonStart) {
        this.onSeasonStart = onSeasonStart;
    }

    /** La temporada configurada, esté abierta o no. */
    public Optional<Season> season() {
        return Optional.ofNullable(season);
    }

    /** La temporada solo si hoy está dentro de sus fechas. */
    public Optional<Season> openSeason() {
        return season().filter(s -> s.isOpen(clock.today()));
    }

    public PassPlayer player(Player player) {

        PassPlayer state = store.get(player.getUniqueId());

        if (season != null && !season.id().equals(state.seasonId())) {
            state.startSeason(season.id());
            onSeasonStart.accept(state);
        }

        return state;
    }

    public boolean isPremium(Player player) {
        return player.hasPermission(PREMIUM_PERMISSION);
    }

    public int level(Player player) {
        return season().map(s -> s.levelFor(player(player).xp())).orElse(0);
    }

    /** Suma puntos de pase y avisa si sube de nivel. Sin temporada abierta no hace nada. */
    public void addXp(Player player, int amount) {

        Optional<Season> open = openSeason();

        if (open.isEmpty() || amount <= 0) {
            return;
        }

        Season current = open.get();
        PassPlayer state = player(player);
        int before = current.levelFor(state.xp());
        state.addXp(amount);
        int after = current.levelFor(state.xp());

        lang.send(player, "pass.xp_gained", "amount", amount);

        if (after > before) {
            lang.send(player, "pass.level_up", "level", after);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }
    }

    /**
     * Guarda ya el estado del jugador. Tras un reclamo no se espera al
     * autoguardado: si el servidor se cae antes, el jugador podría volver a
     * reclamar lo mismo (y el dinero de Vault ya está guardado).
     */
    public void save(Player player) {
        store.save(store.get(player.getUniqueId()));
    }

    public ClaimResult claim(Player player, int level, boolean premium) {

        ClaimResult result = claimLevel(player, level, premium);

        if (result == ClaimResult.CLAIMED) {
            save(player);
        }

        return result;
    }

    private ClaimResult claimLevel(Player player, int level, boolean premium) {

        Optional<Season> open = openSeason();

        if (open.isEmpty()) {
            return ClaimResult.CLOSED;
        }

        Season current = open.get();
        PassPlayer state = player(player);
        Optional<SeasonLevel> entry = current.level(level);
        List<Reward> track = entry
                .map(l -> premium ? l.premium() : l.free())
                .orElse(List.of());

        if (track.isEmpty()) {
            return ClaimResult.NOTHING;
        }

        if (state.hasClaimed(level, premium)) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        if (current.levelFor(state.xp()) < level) {
            return ClaimResult.LOCKED;
        }

        if (!requirements.meets(player, state, current, entry.get().requirements())) {
            return ClaimResult.REQUIREMENTS;
        }

        if (premium && !isPremium(player)) {
            return ClaimResult.NEEDS_PREMIUM;
        }

        state.markClaimed(level, premium);
        rewards.grant(player, track);
        return ClaimResult.CLAIMED;
    }

    /** Reclama todo lo desbloqueado de las dos pistas. Devuelve cuántas recompensas entregó. */
    public int claimAll(Player player) {

        Optional<Season> open = openSeason();

        if (open.isEmpty()) {
            return 0;
        }

        int claimed = 0;

        for (int level : open.get().levels().keySet()) {
            if (claimLevel(player, level, false) == ClaimResult.CLAIMED) {
                claimed++;
            }
            if (claimLevel(player, level, true) == ClaimResult.CLAIMED) {
                claimed++;
            }
        }

        if (claimed > 0) {
            save(player);
        }

        return claimed;
    }

    /** Hay algo desbloqueado sin reclamar (para el recordatorio al entrar). */
    public boolean hasUnclaimed(Player player) {

        Optional<Season> open = openSeason();

        if (open.isEmpty()) {
            return false;
        }

        PassPlayer state = player(player);
        int level = open.get().levelFor(state.xp());
        boolean premium = isPremium(player);

        for (SeasonLevel entry : open.get().levels().headMap(level, true).values()) {
            if (!requirements.meets(player, state, open.get(), entry.requirements())) {
                continue;
            }
            if (!entry.free().isEmpty() && !state.hasClaimed(entry.level(), false)) {
                return true;
            }
            if (premium && !entry.premium().isEmpty() && !state.hasClaimed(entry.level(), true)) {
                return true;
            }
        }

        return false;
    }

}
