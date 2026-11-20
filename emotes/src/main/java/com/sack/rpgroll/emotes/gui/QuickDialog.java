package com.sack.rpgroll.emotes.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.emotes.catalog.Emote;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.player.EmotePlayer;
import com.sack.rpgroll.util.ComponentUtils;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Diálogo rápido ({@code /emotes rapido}): un botón por favorito ocupado, más «Abrir menú» y «Parar».
 * Cada botón ejecuta el comando de siempre, así que enfriamiento, permisos y combate se respetan igual.
 */
public final class QuickDialog {

    /** Un botón: texto visible y comando que ejecuta el cliente al pulsarlo. */
    public record Button(Component label, String command) {
    }

    private QuickDialog() {
    }

    /**
     * Botones en orden de hueco. Los favoritos vacíos o que ya no están en el catálogo no salen.
     *
     * @param name cómo se muestra el nombre de un emote
     */
    static List<Button> buttons(EmotePlayer data, EmoteCatalog catalog, Function<Emote, Component> name,
                                Component menuLabel, Component stopLabel) {
        List<Button> out = new ArrayList<>();
        for (int slot = 1; slot <= data.favorites().size(); slot++) {
            String id = data.favorite(slot);
            Optional<Emote> emote = id == null ? Optional.empty() : catalog.get(id);
            if (emote.isPresent()) {
                out.add(new Button(name.apply(emote.get()), "/emote " + slot));
            }
        }
        out.add(new Button(menuLabel, "/emotes"));
        out.add(new Button(stopLabel, "/emote stop"));
        return out;
    }

    /** Construye el diálogo con la API de Paper y se lo muestra al jugador. */
    public static void show(Player p, EmotePlayer data, EmoteCatalog catalog, LangManager lang) {
        List<ActionButton> actions = buttons(data, catalog, e -> name(e, lang),
                lang.component("quick.menu"), lang.component("quick.stop")).stream()
                .map(b -> ActionButton.builder(b.label())
                        .action(DialogAction.staticAction(ClickEvent.runCommand(b.command())))
                        .build())
                .toList();
        p.showDialog(Dialog.create(f -> f.empty()
                .base(DialogBase.builder(lang.component("quick.title")).build())
                .type(DialogType.multiAction(actions).columns(2).build())));
    }

    /** Igual que en el menú: {@code lang:<clave>} se traduce; si no, MiniMessage. */
    private static Component name(Emote e, LangManager lang) {
        return e.name().startsWith("lang:") ? lang.component(e.name().substring(5)) : ComponentUtils.parse(e.name());
    }
}
