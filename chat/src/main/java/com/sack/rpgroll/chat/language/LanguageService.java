package com.sack.rpgroll.chat.language;

import com.sack.rpgroll.api.RPGRollAPI;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Resuelve qué idiomas conoce un jugador (sembrando los de su raza la primera vez) y aplica ofuscación. */
public class LanguageService {

    private static final String DEFAULT_LANGUAGE_ID = "comun";

    private final LanguageManager languageManager;
    private final PlayerLanguageStateManager stateManager;

    public LanguageService(LanguageManager languageManager, PlayerLanguageStateManager stateManager) {
        this.languageManager = languageManager;
        this.stateManager = stateManager;
    }

    /** Al primer contacto, siembra los idiomas por defecto de la raza del jugador (si RPGRollAPI está lista). */
    public PlayerLanguageState resolve(Player player) {

        PlayerLanguageState state = stateManager.getOrLoad(player);

        if (state.seeded()) {
            return state;
        }

        String raceId = RPGRollAPI.isReady()
                ? RPGRollAPI.get().getPlayer(player.getUniqueId()).map(p -> p.getRace()).orElse(null)
                : null;

        languageManager.defaultLanguagesForRace(raceId).forEach(language -> state.learn(language.id()));

        if (state.speakingLanguageId() == null) {
            languageManager.get(DEFAULT_LANGUAGE_ID).ifPresentOrElse(
                    language -> state.setSpeakingLanguageId(language.id()),
                    () -> state.knownLanguageIds().stream().findFirst().ifPresent(state::setSpeakingLanguageId));
        }

        state.markSeeded();
        return state;
    }

    public Language speakingLanguage(Player player) {

        PlayerLanguageState state = resolve(player);
        String id = state.speakingLanguageId();

        return id != null ? languageManager.get(id).orElse(fallback()) : fallback();
    }

    private Language fallback() {
        return languageManager.get(DEFAULT_LANGUAGE_ID)
                .orElseGet(() -> new Language(DEFAULT_LANGUAGE_ID, "Común", '?', List.of()));
    }

    public boolean knows(Player player, String languageId) {
        return resolve(player).knows(languageId);
    }

    public void unload(UUID uuid) {
        stateManager.unload(uuid);
    }

    public enum LearnResult {
        OK,
        ALREADY_KNOWN,
        NOT_FOUND
    }

    public LearnResult learn(Player player, String languageId) {

        Optional<Language> language = languageManager.get(languageId);

        if (language.isEmpty()) {
            return LearnResult.NOT_FOUND;
        }

        PlayerLanguageState state = resolve(player);

        if (!state.learn(language.get().id())) {
            return LearnResult.ALREADY_KNOWN;
        }

        return LearnResult.OK;
    }

    public boolean setSpeaking(Player player, String languageId) {

        PlayerLanguageState state = resolve(player);

        if (!state.knows(languageId)) {
            return false;
        }

        state.setSpeakingLanguageId(languageId.toLowerCase(java.util.Locale.ROOT));
        return true;
    }

    /** @return el mensaje tal cual si {@code receiver} conoce el idioma, u ofuscado si no. */
    public String render(String message, Language language, Player receiver) {
        return knows(receiver, language.id()) ? message : language.obfuscate(message);
    }

}
