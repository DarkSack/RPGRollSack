package com.sack.rpgroll.pass.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.pass.PassModule;
import com.sack.rpgroll.pass.mission.Mission;
import com.sack.rpgroll.pass.mission.MissionScope;
import com.sack.rpgroll.pass.player.PassPlayer;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Misiones de hoy, de la semana y de la temporada, con su avance. */
public class MissionsGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int BACK = 49;

    private final PassModule module;
    private final LangManager lang;

    public MissionsGUI(Player player, PassModule module) {
        super(player, module.lang().component("gui.missions.title"), SIZE);
        this.module = module;
        this.lang = module.lang();
    }

    @Override
    public void build() {

        clear();
        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        PassPlayer state = module.missions().refreshed(player);

        row(9, Material.CLOCK, "gui.missions.daily", MissionScope.DAILY, Material.PAPER, 7, state);
        row(18, Material.COMPASS, "gui.missions.weekly", MissionScope.WEEKLY, Material.MAP, 7, state);
        row(27, Material.NETHER_STAR, "gui.missions.season", MissionScope.SEASON, Material.BOOK, 16, state);

        setItem(BACK, GuiItems.item(Material.ARROW, lang.component("gui.back"), List.of()));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        if (event.getRawSlot() == BACK) {
            new PassGUI(player, module).open();
        }
    }

    /** Etiqueta en la primera columna y las misiones a su derecha (saltando a la fila siguiente si hacen falta). */
    private void row(int labelSlot, Material labelIcon, String labelKey, MissionScope scope, Material icon, int max,
            PassPlayer state) {

        setItem(labelSlot, GuiItems.item(labelIcon, lang.component(labelKey), List.of()));
        List<Mission> missions = module.missions().active(player, scope);

        for (int i = 0; i < missions.size() && i < max; i++) {

            Mission mission = missions.get(i);
            int slot = labelSlot + 1 + (i % 7) + (i / 7) * 9;
            boolean done = state.isCompleted(mission.id());
            int value = Math.min(mission.amount(), state.progress(mission.id()));

            setItem(slot, GuiItems.item(done ? Material.LIME_DYE : icon,
                    ComponentUtils.parse(mission.name()),
                    List.of(lang.component("gui.missions.progress", "bar", GuiItems.bar(value, mission.amount(), 10),
                                    "value", value, "amount", mission.amount()),
                            lang.component("gui.missions.reward", "xp", mission.xp()),
                            lang.component(done ? "gui.missions.done" : "gui.missions.pending"))));
        }
    }

}
