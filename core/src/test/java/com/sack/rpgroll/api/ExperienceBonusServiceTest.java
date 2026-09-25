package com.sack.rpgroll.api;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExperienceBonusServiceTest {

    private final ExperienceBonusService service = new ExperienceBonusService();

    private static PermissionAttachmentInfo permission(String node, boolean value) {
        PermissionAttachmentInfo info = mock(PermissionAttachmentInfo.class);
        when(info.getPermission()).thenReturn(node);
        when(info.getValue()).thenReturn(value);
        return info;
    }

    private static Player withPermissions(PermissionAttachmentInfo... infos) {
        Player player = mock(Player.class);
        when(player.getEffectivePermissions()).thenReturn(Set.of(infos));
        return player;
    }

    @Test
    void inheritedRanksUseTheHighestBonusInsteadOfAddingThem() {

        Player mvp = withPermissions(
                permission("rpgroll.exp.bonus.10", true),
                permission("rpgroll.exp.bonus.20", true),
                permission("rpgroll.exp.bonus.30", true));

        assertEquals(130, service.boost(mvp, 100));
    }

    @Test
    void negatedAndWildcardPermissionsGiveNothing() {

        Player player = withPermissions(
                permission("rpgroll.exp.bonus.50", false),
                permission("rpgroll.exp.bonus.*", true));

        assertEquals(100, service.boost(player, 100));
    }

    @Test
    void addonSourcesAddToTheRankBonus() {

        Player vip = withPermissions(permission("rpgroll.exp.bonus.10", true));
        service.registerSource("prestige", p -> 5);

        assertEquals(23, service.boost(vip, 20));

        service.unregisterSource("prestige");
        assertEquals(22, service.boost(vip, 20));
    }

    @Test
    void neverTouchesZeroOrNegativeAmounts() {

        Player vip = withPermissions(permission("rpgroll.exp.bonus.10", true));

        assertEquals(0, service.boost(vip, 0));
        assertEquals(-5, service.boost(vip, -5));
    }

}
