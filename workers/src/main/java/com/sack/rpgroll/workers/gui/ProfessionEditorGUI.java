package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.reskin.EntityReskin;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.ai.AiAction;
import com.sack.rpgroll.workers.core.ai.AiCondition;
import com.sack.rpgroll.workers.core.economy.WageType;
import com.sack.rpgroll.workers.core.profession.AiRule;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfessionEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 9;
    private static final int ICON_SLOT = 10;
    private static final int ENTITY_TYPE_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int SKILLS_SLOT = 13;
    private static final int AI_RULES_SLOT = 14;
    private static final int CLEAR_RULES_SLOT = 15;
    private static final int SCHEDULE_SLOT = 16;
    private static final int WAGE_AMOUNT_SLOT = 19;
    private static final int WAGE_TYPE_SLOT = 20;
    private static final int TOOL_SLOT = 21;

    private static final int RESKIN_MATERIAL_SLOT = 22;
    private static final int RESKIN_CMD_SLOT = 23;
    private static final int RESKIN_SCALE_SLOT = 24;

    private static final int BACK_SLOT = 40;

    private final ProfessionManager professionManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Profession current;

    public ProfessionEditorGUI(Player player, Profession profession, ProfessionManager professionManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.title", "id", profession.id()), NamedTextColor.GOLD), SIZE);
        this.current = profession;
        this.professionManager = professionManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Profession updated) {
        current = updated;
        professionManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.name", "name",
                        current.displayName()), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(ProfessionBrowserGUI.parseMaterial(current.icon(), Material.VILLAGER_SPAWN_EGG))
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.icon", "icon", current.icon()), NamedTextColor.YELLOW)).build());

        setItem(ENTITY_TYPE_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.entity_type", "entity",
                        current.entityType()), NamedTextColor.AQUA)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.description"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? chatPromptManager.lang().raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(SKILLS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.skills", "skills",
                        String.join(", ", current.skillIds())), NamedTextColor.GOLD))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.skills_hint"), NamedTextColor.GRAY))
                .build());

        setItem(AI_RULES_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.ai_rules", "count",
                        current.aiRules().size()), NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines(chatPromptManager.lang().raw("gui.profession.editor.ai_rules_lore",
                        "conditions", java.util.Arrays.toString(AiCondition.values()), "actions",
                        java.util.Arrays.toString(AiAction.values()))))
                .build());

        setItem(CLEAR_RULES_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.clear_rules"), NamedTextColor.RED))
                .build());

        setItem(SCHEDULE_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.schedule", "schedule",
                        current.scheduleId() == null ? chatPromptManager.lang().raw("gui.profession.editor.schedule_none")
                                : current.scheduleId()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.schedule_hint", "keyword",
                        chatPromptManager.lang().raw("gui.profession.editor.schedule_none_keyword")), NamedTextColor.GRAY))
                .build());

        setItem(WAGE_AMOUNT_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.wage", "wage",
                        current.wageAmount()), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.wage_hint"), NamedTextColor.GRAY))
                .build());

        setItem(WAGE_TYPE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.wage_type", "type",
                        current.wageType()), NamedTextColor.GREEN))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.rotate_hint"), NamedTextColor.GRAY))
                .build());

        setItem(TOOL_SLOT, new ItemBuilder(Material.IRON_PICKAXE)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.tool", "tool",
                        current.toolMaterial() == null ? chatPromptManager.lang().raw("gui.profession.editor.tool_none")
                                : current.toolMaterial()), NamedTextColor.WHITE))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.tool_hint", "keyword",
                        chatPromptManager.lang().raw("gui.profession.editor.tool_none_keyword")), NamedTextColor.GRAY))
                .build());

        EntityReskin reskin = current.reskin();

        setItem(RESKIN_MATERIAL_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.reskin_material",
                        "value", reskin.material() != null ? reskin.material()
                                : chatPromptManager.lang().raw("gui.profession.editor.reskin_none")), NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines(chatPromptManager.lang().raw("gui.profession.editor.reskin_hint")))
                .build());

        setItem(RESKIN_CMD_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.reskin_cmd", "value",
                        reskin.customModelData()), NamedTextColor.LIGHT_PURPLE))
                .build());

        setItem(RESKIN_SCALE_SLOT, new ItemBuilder(Material.SLIME_BALL)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.reskin_scale", "value",
                        reskin.scale()), NamedTextColor.LIGHT_PURPLE))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.reskin_scale_hint"), NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"),
                    value -> replace(withField(f -> f.name = value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.profession.editor.prompt_icon"),
                    value -> replace(withField(f -> f.icon = value)));
        } else if (slot == ENTITY_TYPE_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.profession.editor.prompt_entity_type"),
                    value -> replace(withField(f -> f.entityType = value)));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"),
                    value -> replace(withField(f -> f.description = value)));
        } else if (slot == SKILLS_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.profession.editor.prompt_skills"),
                    value -> replace(withField(f -> f.skillIds = parseSet(value))));
        } else if (slot == AI_RULES_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.profession.editor.prompt_ai_rule"),
                    this::addRule);
        } else if (slot == CLEAR_RULES_SLOT) {
            replace(withField(f -> f.aiRules = List.of()));
        } else if (slot == SCHEDULE_SLOT) {
            String noneKeyword = chatPromptManager.lang().raw("gui.profession.editor.schedule_none_keyword");
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.profession.editor.prompt_schedule",
                    "keyword", noneKeyword), value -> replace(withField(
                    f -> f.scheduleId = value.equalsIgnoreCase(noneKeyword) ? null : value.trim().toLowerCase(Locale.ROOT))));
        } else if (slot == WAGE_AMOUNT_SLOT) {
            replace(withField(f -> f.wageAmount = Math.max(0, current.wageAmount() + sign * 10)));
        } else if (slot == WAGE_TYPE_SLOT) {
            WageType[] values = WageType.values();
            replace(withField(f -> f.wageType = values[(current.wageType().ordinal() + 1) % values.length]));
        } else if (slot == TOOL_SLOT) {
            String noneKeyword = chatPromptManager.lang().raw("gui.profession.editor.tool_none_keyword");
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.profession.editor.prompt_tool", "keyword",
                    noneKeyword), value -> replace(withField(
                    f -> f.toolMaterial = value.equalsIgnoreCase(noneKeyword) ? null : value)));
        } else if (slot == RESKIN_MATERIAL_SLOT) {
            if (event.isShiftClick()) {
                EntityReskin r = current.reskin();
                replace(withField(f -> f.reskin = new EntityReskin(null, r.customModelData(), r.scale(), r.yOffset())));
                return;
            }
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.profession.editor.prompt_reskin_material"),
                    value -> {
                        EntityReskin r = current.reskin();
                        replace(withField(f -> f.reskin = new EntityReskin(value.trim().toUpperCase(Locale.ROOT),
                                r.customModelData(), r.scale(), r.yOffset())));
                    });
        } else if (slot == RESKIN_CMD_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.profession.editor.prompt_reskin_cmd"),
                    value -> {
                        try {
                            EntityReskin r = current.reskin();
                            int cmd = Integer.parseInt(value.trim());
                            replace(withField(f -> f.reskin = new EntityReskin(r.material(), cmd, r.scale(), r.yOffset())));
                        } catch (NumberFormatException ignored) {
                            // valor inválido, se ignora
                        }
                    });
        } else if (slot == RESKIN_SCALE_SLOT) {
            EntityReskin r = current.reskin();
            double newScale = Math.max(0.1, r.scale() + sign / 10.0);
            replace(withField(f -> f.reskin = new EntityReskin(r.material(), r.customModelData(), newScale, r.yOffset())));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void addRule(String raw) {

        String[] parts = raw.split(";");

        if (parts.length < 3) {
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.invalid_rule_format"), NamedTextColor.RED));
            build();
            return;
        }

        AiCondition condition;
        AiAction action;

        try {
            condition = AiCondition.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            action = AiAction.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.invalid_rule_values"), NamedTextColor.RED));
            build();
            return;
        }

        int priority;
        try {
            priority = Integer.parseInt(parts[2].trim());
        } catch (NumberFormatException e) {
            priority = 100;
        }

        List<AiRule> rules = new ArrayList<>(current.aiRules());
        rules.add(new AiRule(condition, action, priority));

        replace(withField(f -> f.aiRules = rules));
    }

    private Set<String> parseSet(String raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw.split(",")) {
            if (!entry.isBlank()) {
                result.add(entry.trim().toLowerCase(Locale.ROOT));
            }
        }

        return result;
    }

    /** Pequeño staging mutable para no repetir el constructor entero de 11 campos en cada handler. */
    private static final class Fields {
        String name;
        String icon;
        String entityType;
        String description;
        Set<String> skillIds;
        List<AiRule> aiRules;
        String scheduleId;
        double wageAmount;
        WageType wageType;
        String toolMaterial;
        EntityReskin reskin;
    }

    private Profession withField(java.util.function.Consumer<Fields> mutator) {

        Fields fields = new Fields();
        fields.name = current.displayName();
        fields.icon = current.icon();
        fields.entityType = current.entityType();
        fields.description = current.description();
        fields.skillIds = current.skillIds();
        fields.aiRules = current.aiRules();
        fields.scheduleId = current.scheduleId();
        fields.wageAmount = current.wageAmount();
        fields.wageType = current.wageType();
        fields.toolMaterial = current.toolMaterial();
        fields.reskin = current.reskin();

        mutator.accept(fields);

        return new Profession(current.id(), fields.name, fields.icon, fields.description, fields.entityType,
                fields.skillIds, fields.aiRules, fields.scheduleId, fields.wageAmount, fields.wageType,
                fields.toolMaterial, fields.reskin);
    }

}
