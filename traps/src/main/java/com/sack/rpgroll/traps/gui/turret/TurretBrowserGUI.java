package com.sack.rpgroll.traps.gui.turret;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.gui.ChatPromptManager;
import com.sack.rpgroll.traps.turret.TurretDefinition;
import com.sack.rpgroll.traps.turret.TurretManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class TurretBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final TurretManager turretManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<TurretDefinition> turrets;

    public TurretBrowserGUI(Player player, TurretManager turretManager, ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.turret.browser.title"), SIZE);
        this.turretManager = turretManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.turrets = List.copyOf(turretManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < turrets.size() && i < 36; i++) {

            TurretDefinition turret = turrets.get(i);

            setItem(i, new ItemBuilder(turret.model())
                    .setName(Component.text(turret.id(), NamedTextColor.YELLOW))
                    .setLore(lang.component("gui.turret.browser.radius", "value", turret.radius()),
                            lang.component("gui.browser.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.browser.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < turrets.size() && slot < 36) {
            new TurretEditorGUI(player, turrets.get(slot), turretManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "gui.browser.prompt_new_id", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (turretManager.exists(id)) {
                lang.send(player, "gui.browser.id_exists");
                reopen();
                return;
            }

            TurretDefinition turret = new TurretDefinition(id, id, "", 12.0, true, true, 20, List.of(), null, null,
                    null,
                null, 0, -1);

            turretManager.save(turret);
            reopen();
        });
    }

    private void reopen() {
        this.turrets = List.copyOf(turretManager.getAll());
        open();
    }

}
