package com.sack.rpgroll.tab.visibility;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * Controla si {@code subject} aparece en el tablist de {@code viewer} SIN
 * tocar su visibilidad real como entidad (que sigue siendo la que decida
 * {@code Player#hidePlayer}/{@code showPlayer}) — usa el flag nativo
 * "listed" del paquete {@code PLAYER_INFO_UPDATE} (protocolo 1.19.3+), que
 * es justo para esto: separar "está en el tablist" de "existe en el
 * mundo". Requiere ProtocolLib — ver {@link com.sack.rpgroll.tab.integration.ProtocolLibHook}.
 * <p>
 * Es la pieza más "de bajo nivel" del addon — el resto de RPGRoll-TAB no
 * depende de esta clase para funcionar.
 */
public class VisibilityEngine {

    public void setListed(Player viewer, Player subject, boolean listed) {

        PacketContainer packet = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.PLAYER_INFO);

        packet.getPlayerInfoActions().write(0, Set.of(EnumWrappers.PlayerInfoAction.UPDATE_LISTED));

        PlayerInfoData data = new PlayerInfoData(
                subject.getUniqueId(),
                subject.getPing(),
                listed,
                EnumWrappers.NativeGameMode.fromBukkit(subject.getGameMode()),
                WrappedGameProfile.fromPlayer(subject),
                WrappedChatComponent.fromText(subject.getName()));

        packet.getPlayerInfoDataLists().write(0, List.of(data));

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
        } catch (Exception e) {
            viewer.getServer().getLogger()
                    .warning("✘ RPGRoll-TAB: no se pudo actualizar visibilidad de tablist ("
                            + subject.getName() + " para " + viewer.getName() + "): " + e.getMessage());
        }
    }

}
