package com.sack.rpgroll.npcs.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.npcs.core.NpcEditSession;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;

public class NpcPoseSelectGUI extends InventoryGUI {

    private static final String[] POSES = { "STANDING", "SITTING", "SNEAKING", "SWIMMING", "SLEEPING" };

    private final NpcEditSession session;
    private final NpcAdminGUI parent;
    private final LangManager langManager;
    private final Map<Integer, String> slotToPose = new HashMap<>();

    public NpcPoseSelectGUI(Player player, NpcEditSession session, NpcAdminGUI parent, LangManager langManager) {
        super(player, langManager.component("pose.title"), 27);
        this.session = session;
        this.parent = parent;
        this.langManager = langManager;
    }

    @Override
    public void build() {

        clear();
        slotToPose.clear();

        for (int i = 0; i < 9; i++) {
            setItem(i, ItemBuilder.createFiller());
        }

        int slot = 10;
        for (String pose : POSES) {

            boolean selected = pose.equals(session.getPose());

            setItem(slot, new ItemBuilder(selected ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setName(Component.text(pose, selected ? NamedTextColor.GREEN : NamedTextColor.WHITE))
                    .build());

            slotToPose.put(slot, pose);
            slot++;
        }

        for (int i = 18; i < 27; i++) {
            setItem(i, ItemBuilder.createFiller());
        }

        setItem(22, ItemBuilder.createCancelButton(langManager.raw("pose.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 22) {
            close();
            parent.reopen();
            return;
        }

        String pose = slotToPose.get(slot);
        if (pose != null) {
            session.setPose(pose);
            close();
            parent.reopen();
        }
    }

}