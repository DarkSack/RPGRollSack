package com.sack.rpgroll.pass;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.pass.daily.DailyService;
import com.sack.rpgroll.pass.mission.MissionService;
import com.sack.rpgroll.pass.reward.RewardService;
import com.sack.rpgroll.pass.season.PassService;
import com.sack.rpgroll.pass.vote.VoteService;

/** Los servicios del módulo juntos, para que menús y comandos no reciban seis parámetros. */
public record PassModule(LangManager lang, PassService pass, MissionService missions, DailyService daily,
        VoteService votes, RewardService rewards) {
}
