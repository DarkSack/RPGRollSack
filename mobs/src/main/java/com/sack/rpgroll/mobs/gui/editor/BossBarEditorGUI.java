package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobBossBarDef;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Editor de la bossbar: habilitada, color, estilo y título. */
public class BossBarEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;

    private static final int ENABLED_SLOT = 10;
    private static final int COLOR_SLOT = 11;
    private static final int STYLE_SLOT = 12;
    private static final int TITLE_SLOT = 13;

    private static final int BACK_SLOT = 22;

    private final MobEditorSession session;
    private final Runnable onBack;

    public BossBarEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("BossBar: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        MobBossBarDef bossBar = session.bossBar;

        setItem(ENABLED_SLOT, new ItemBuilder(bossBar.enabled() ? Material.DRAGON_HEAD : Material.SKELETON_SKULL)
                .setName(Component.text("Habilitada: " + bossBar.enabled(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(Component.text("Color: " + bossBar.color(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar al siguiente", NamedTextColor.GRAY))
                .build());

        setItem(STYLE_SLOT, new ItemBuilder(Material.STRING)
                .setName(Component.text("Estilo: " + bossBar.style(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar al siguiente", NamedTextColor.GRAY))
                .build());

        setItem(TITLE_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Título: "
                        + (bossBar.title() != null ? bossBar.title() : "(nombre del mob)"), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: escribir · Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        MobBossBarDef bossBar = session.bossBar;

        if (slot == ENABLED_SLOT) {
            session.bossBar = new MobBossBarDef(!bossBar.enabled(), bossBar.color(), bossBar.style(),
                    bossBar.title());
            build();
            return;
        }

        if (slot == COLOR_SLOT) {
            session.bossBar = new MobBossBarDef(bossBar.enabled(), nextEnum(BossBar.Color.values(), bossBar.color()),
                    bossBar.style(), bossBar.title());
            build();
            return;
        }

        if (slot == STYLE_SLOT) {
            session.bossBar = new MobBossBarDef(bossBar.enabled(), bossBar.color(),
                    nextEnum(BossBar.Overlay.values(), bossBar.style()), bossBar.title());
            build();
            return;
        }

        if (slot == TITLE_SLOT) {
            if (event.isShiftClick()) {
                session.bossBar = new MobBossBarDef(bossBar.enabled(), bossBar.color(), bossBar.style(), null);
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "Escribí el nuevo título:", value -> {
                MobBossBarDef current = session.bossBar;
                session.bossBar = new MobBossBarDef(current.enabled(), current.color(), current.style(), value);
                build();
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private <T extends Enum<T>> String nextEnum(T[] values, String currentName) {

        int currentIndex = -1;

        for (int i = 0; i < values.length; i++) {
            if (values[i].name().equalsIgnoreCase(currentName)) {
                currentIndex = i;
                break;
            }
        }

        return values[(currentIndex + 1) % values.length].name();
    }

}
