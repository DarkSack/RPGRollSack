package com.sack.rpgroll.pass.listener;

import com.sack.rpgroll.pass.mission.MissionService;
import com.sack.rpgroll.pass.mission.MissionType;
import com.sack.rpgroll.quests.api.QuestCompleteEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Solo se registra con RPGRoll-Quests activo: sus clases no existen sin él. */
public class QuestsHook implements Listener {

    private final MissionService missions;

    public QuestsHook(MissionService missions) {
        this.missions = missions;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuestComplete(QuestCompleteEvent event) {
        missions.progress(event.getPlayer(), MissionType.COMPLETE_QUEST, event.getQuestId(), 1);
    }

}
