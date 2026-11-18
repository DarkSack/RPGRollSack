package com.sack.rpgroll.gui.confirm;

import com.sack.rpgroll.gui.CoreMenus;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * "¿Seguro?" antes de una acción que no se deshace (abandonar un oficio, crear el personaje...).
 * El ítem del centro explica qué pasa; confirmar a la izquierda, volver a la derecha, separados
 * por un hueco para que un toque en Bedrock no caiga en el botón equivocado.
 * <p>
 * Con {@code required = true} (creación obligatoria del personaje) cerrar con ESC reabre la
 * ventana: solo se sale eligiendo uno de los dos botones.
 */
public class ConfirmGUI extends InventoryGUI {

    /** Fondo dibujado (sackito:menus/confirmar): confirmar (11) y volver (15) ya van dibujados. */
    public static final String BACKGROUND = "confirmar";
    /** Fondo para acciones que pierden algo: solo huecos, los botones de color invertido se ven tal cual. */
    public static final String BACKGROUND_DANGER = "confirmar_peligro";

    static final int CONFIRM_SLOT = 11;
    static final int SUBJECT_SLOT = 13;
    static final int CANCEL_SLOT = 15;

    private final ItemStack subject;
    private final ItemStack confirmButton;
    private final ItemStack cancelButton;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private final boolean required;
    private final boolean destructive;

    /**
     * @param destructive true si confirmar pierde algo (abandonar un oficio): el botón de confirmar
     *                    va en rojo y el de quedarse en verde, para que el color no invite a perderlo.
     */
    public ConfirmGUI(Player player, Component title, ItemStack subject, String confirmText, String cancelText,
            boolean destructive, Runnable onConfirm, Runnable onCancel, boolean required) {
        super(player, CoreMenus.skin().title(background(destructive), title), 27);
        this.subject = subject;
        this.confirmButton = button(destructive ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE,
                confirmText, destructive ? NamedTextColor.RED : NamedTextColor.GREEN);
        this.cancelButton = destructive
                ? button(Material.LIME_STAINED_GLASS_PANE, cancelText, NamedTextColor.GREEN)
                : button(Material.ARROW, cancelText, NamedTextColor.YELLOW);
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.required = required;
        this.destructive = destructive;
    }

    /** Fondo según el tipo: con {@code destructive} no se dibuja ✓/✗ sobre botones que invierten el color. */
    public static String background(boolean destructive) {
        return destructive ? BACKGROUND_DANGER : BACKGROUND;
    }

    /** Botón dibujado en el fondo normal; en el de peligro se queda visible con su color. */
    private ItemStack drawn(ItemStack button) {
        return destructive ? button : CoreMenus.skin().button(button);
    }

    private static ItemStack button(Material material, String text, NamedTextColor color) {
        return new ItemBuilder(material)
                .setName(ComponentUtils.parseWithDefault(text, color).decorate(TextDecoration.BOLD))
                .build();
    }

    @Override
    public boolean isSelectionRequired() {
        return required;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < 27; slot++) {
            setItem(slot, CoreMenus.filler());
        }

        setItem(CONFIRM_SLOT, drawn(confirmButton));
        setItem(SUBJECT_SLOT, subject);
        setItem(CANCEL_SLOT, drawn(cancelButton));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        Runnable choice = switch (event.getRawSlot()) {
            case CONFIRM_SLOT -> onConfirm;
            case CANCEL_SLOT -> onCancel;
            default -> null;
        };

        if (choice == null) {
            return;
        }

        markSelectionMade();
        close();
        choice.run();
    }

}
