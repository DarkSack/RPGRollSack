package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobBossBarDef;

import net.kyori.adventure.bossbar.BossBar;

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
    private final LangManager lang;

    public BossBarEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.bossbar.title", "id",
                session.original.id()), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        MobBossBarDef bossBar = session.bossBar;

        setItem(ENABLED_SLOT, new ItemBuilder(bossBar.enabled() ? Material.DRAGON_HEAD : Material.SKELETON_SKULL)
                .setName(lang.component("gui.bossbar.enabled_label", "value", bossBar.enabled()))
                .setLore(lang.component("gui.common.click_toggle"))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.RED_DYE)
                .setName(lang.component("gui.bossbar.color_label", "value", bossBar.color()))
                .setLore(lang.component("gui.common.click_next"))
                .build());

        setItem(STYLE_SLOT, new ItemBuilder(Material.STRING)
                .setName(lang.component("gui.bossbar.style_label", "value", bossBar.style()))
                .setLore(lang.component("gui.common.click_next"))
                .build());

        setItem(TITLE_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.bossbar.title_label", "value",
                        bossBar.title() != null ? bossBar.title() : lang.raw("gui.bossbar.title_default")))
                .setLore(lang.component("gui.common.click_write_shift_remove"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
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
            session.chatPromptManager.prompt(player, "gui.bossbar.prompt_title", value -> {
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
