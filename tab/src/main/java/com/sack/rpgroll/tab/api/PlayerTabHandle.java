package com.sack.rpgroll.tab.api;

import com.sack.rpgroll.tab.core.RefreshCoordinator;
import com.sack.rpgroll.tab.profile.PlayerStateManager;
import com.sack.rpgroll.tab.profile.ProfileManager;
import com.sack.rpgroll.tab.profile.TABProfile;

import org.bukkit.entity.Player;

/** {@code tab.player(player).refresh()} / {@code .setProfile("dungeon")} — ver sección 43 del diseño. */
public class PlayerTabHandle {

    private final Player player;
    private final RefreshCoordinator refreshCoordinator;
    private final PlayerStateManager playerStateManager;
    private final ProfileManager profileManager;

    public PlayerTabHandle(Player player, RefreshCoordinator refreshCoordinator, PlayerStateManager playerStateManager,
            ProfileManager profileManager) {
        this.player = player;
        this.refreshCoordinator = refreshCoordinator;
        this.playerStateManager = playerStateManager;
        this.profileManager = profileManager;
    }

    /** Vuelve a resolver el contexto y re-aplica todo (equivalente a que el jugador reingresara). */
    public void refresh() {
        refreshCoordinator.refreshPlayer(player);
    }

    /** Fuerza un perfil específico, ignorando el Context Engine hasta el próximo {@link #refresh()}. */
    public boolean setProfile(String profileId) {

        TABProfile profile = profileManager.get(profileId).orElse(null);

        if (profile == null) {
            return false;
        }

        playerStateManager.forceProfile(player, profile);
        refreshCoordinator.applyActiveState(player);
        return true;
    }

    public TABProfile activeProfile() {
        return playerStateManager.activeProfile(player).orElse(null);
    }

}
