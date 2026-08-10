package com.sack.rpgroll.extras.stat;

import java.util.List;

public record RegenerationConfig(List<RateRule> rules, int intervalTicks) {
}
