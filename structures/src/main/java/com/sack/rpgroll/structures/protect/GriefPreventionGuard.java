package com.sack.rpgroll.structures.protect;

import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.storage.Box;
import org.bukkit.event.Event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GriefPrevention por reflexión ({@link GriefPreventionApi}): claims que cruzan el rectángulo XZ de la caja; niega si
 * en alguno el dueño no tiene permiso de construir. Sin claim, se puede.
 * <p>
 * Doc oficial leída (GriefPrevention 16.18.8, última versión publicada; igual en master 18.0.0-SNAPSHOT): fuentes de
 * https://github.com/GriefPrevention/GriefPrevention ({@code Claim.java}, {@code ClaimPermission.java}):
 * {@code Claim.checkPermission(UUID, ClaimPermission.Build, Event)} (null = permitido; vale desconectado).
 */
final class GriefPreventionGuard implements AreaGuard {

    private static final Optional<Denial> DENIED = Optional.of(new Denial(Hook.GRIEFPREVENTION,
            "protect.griefprevention"));

    private GriefPreventionApi gp;
    private Object build;
    private Method checkPermission;

    @Override
    public Optional<Denial> check(UUID owner, String world, Box box) {
        try {
            resolve();
            for (Object claim : gp.crossing(world, box)) {
                // shortcut: el claim y cada subdivisión que cruza se miran por separado; una caja entera dentro
                // de una subdivisión con permiso se niega si el dueño no tiene permiso en el claim de fuera.
                List<Object> check = new ArrayList<>();
                check.add(claim);
                check.addAll(gp.crossingChildren(claim, world, box));
                for (Object c : check) {
                    if (checkPermission.invoke(c, owner, build, null) != null) {
                        return DENIED;
                    }
                }
            }
            return Optional.empty();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void resolve() throws ReflectiveOperationException {
        if (gp != null) {
            return;
        }
        GriefPreventionApi api = GriefPreventionApi.resolve();
        Class<?> permission = api.type("ClaimPermission");
        checkPermission = api.type("Claim").getMethod("checkPermission", UUID.class, permission, Event.class);
        build = permission.getField("Build").get(null);
        gp = api; // al final: si algo falla, se vuelve a intentar entero
    }
}
