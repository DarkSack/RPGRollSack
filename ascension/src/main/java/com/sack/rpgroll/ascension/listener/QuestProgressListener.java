package com.sack.rpgroll.ascension.listener;

import com.sack.rpgroll.ascension.engine.ProgressService;
import com.sack.rpgroll.ascension.progress.ProgressEvent;
import com.sack.rpgroll.ascension.progress.TriggerType;
import com.sack.rpgroll.quests.api.QuestCompleteEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Solo se registra si RPGRoll-Quests está habilitado: referencia sus clases. */
public class QuestProgressListener implements Listener {

    private final ProgressService progress;

    public QuestProgressListener(ProgressService progress) {
        this.progress = progress;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onComplete(QuestCompleteEvent event) {
        progress.handle(event.getPlayer(), new ProgressEvent(TriggerType.COMPLETE_QUEST, event.getQuestId(), null));
    }

}
