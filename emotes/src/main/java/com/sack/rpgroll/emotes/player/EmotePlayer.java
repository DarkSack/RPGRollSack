package com.sack.rpgroll.emotes.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Emotes de un jugador: los desbloqueados, sus favoritos (huecos fijos, null = vacío)
 * y la marca de su último uso. Los ids se guardan en minúsculas, aunque ya no existan
 * en el catálogo, para no perderlos si el emote vuelve. Mutable; solo se toca desde el
 * hilo principal.
 */
public class EmotePlayer {

    private final UUID uuid;
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final String[] favorites;
    private long lastUseMillis;

    EmotePlayer(UUID uuid, int maxFavorites) {
        this.uuid = uuid;
        this.favorites = new String[maxFavorites];
    }

    public UUID uuid() {
        return uuid;
    }

    /** Vista de solo lectura de los desbloqueados. */
    public Set<String> unlocked() {
        return Collections.unmodifiableSet(unlocked);
    }

    /** @return true si no lo tenía. */
    public boolean unlock(String id) {
        return !isBlank(id) && unlocked.add(normalize(id));
    }

    /** Lo bloquea y lo quita de favoritos. @return true si lo tenía. */
    public boolean lock(String id) {
        if (isBlank(id)) {
            return false;
        }
        String key = normalize(id);
        for (int i = 0; i < favorites.length; i++) {
            if (key.equals(favorites[i])) {
                favorites[i] = null;
            }
        }
        return unlocked.remove(key);
    }

    /** Copia de los huecos: tamaño fijo, null = vacío. */
    public List<String> favorites() {
        return new ArrayList<>(java.util.Arrays.asList(favorites));
    }

    /** Hueco numerado desde 1; fuera de rango da null. */
    public String favorite(int slot1Based) {
        return slot1Based < 1 || slot1Based > favorites.length ? null : favorites[slot1Based - 1];
    }

    /** Lo quita si ya es favorito; si no, lo pone en el primer hueco libre. @return false si no hay hueco. */
    public boolean toggleFavorite(String id) {
        if (isBlank(id)) {
            return false;
        }
        String key = normalize(id);
        int free = -1;
        for (int i = 0; i < favorites.length; i++) {
            if (key.equals(favorites[i])) {
                favorites[i] = null;
                return true;
            }
            if (free < 0 && favorites[i] == null) {
                free = i;
            }
        }
        if (free < 0) {
            return false;
        }
        favorites[free] = key;
        return true;
    }

    public long lastUseMillis() {
        return lastUseMillis;
    }

    public void lastUseMillis(long millis) {
        this.lastUseMillis = millis;
    }

    /** Carga de disco: ignora huecos fuera de rango y ids vacíos. */
    void setFavoriteSlot(int index, String id) {
        if (index >= 0 && index < favorites.length && id != null && !id.isBlank()) {
            favorites[index] = normalize(id);
        }
    }

    private static boolean isBlank(String id) {
        return id == null || id.isBlank();
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

}
