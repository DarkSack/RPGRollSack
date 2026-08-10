package com.sack.rpgroll.tab.sorting;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;

public record SortingDefinition(String id, List<SortRule> rules) implements RPGContent {
}
