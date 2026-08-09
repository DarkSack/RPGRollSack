package com.sack.rpgroll.workers.gui;

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
    private static final int BACK_SLOT = 40;

    private final ProfessionManager professionManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Profession current;

    public ProfessionEditorGUI(Player player, Profession profession, ProfessionManager professionManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Profesión: " + profession.id(), NamedTextColor.GOLD), SIZE);
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
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(ProfessionBrowserGUI.parseMaterial(current.icon(), Material.VILLAGER_SPAWN_EGG))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(ENTITY_TYPE_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(Component.text("Entidad vanilla: " + current.entityType(), NamedTextColor.AQUA)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(SKILLS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Habilidades: " + String.join(", ", current.skillIds()), NamedTextColor.GOLD))
                .setLore(Component.text("Escribí ids separados por comas", NamedTextColor.GRAY)).build());

        setItem(AI_RULES_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text("Reglas de IA: " + current.aiRules().size(), NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines("Formato: CONDICION;ACCION;prioridad\n"
                        + "Condiciones: " + java.util.Arrays.toString(AiCondition.values()) + "\n"
                        + "Acciones: " + java.util.Arrays.toString(AiAction.values()) + "\nEscribí para AGREGAR una."))
                .build());

        setItem(CLEAR_RULES_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Borrar todas las reglas", NamedTextColor.RED)).build());

        setItem(SCHEDULE_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Horario: " + (current.scheduleId() == null ? "(ninguno)" : current.scheduleId()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Escribí el id de un horario, o 'ninguno'", NamedTextColor.GRAY)).build());

        setItem(WAGE_AMOUNT_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text("Salario: " + current.wageAmount(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +10 · Click derecho: -10", NamedTextColor.GRAY)).build());

        setItem(WAGE_TYPE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Tipo de pago: " + current.wageType(), NamedTextColor.GREEN))
                .setLore(Component.text("Click para rotar", NamedTextColor.GRAY)).build());

        setItem(TOOL_SLOT, new ItemBuilder(Material.IRON_PICKAXE)
                .setName(Component.text("Herramienta: " + (current.toolMaterial() == null ? "(ninguna)" : current.toolMaterial()),
                        NamedTextColor.WHITE))
                .setLore(Component.text("Escribí un Material, o 'ninguna'", NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(withField(f -> f.name = value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(withField(f -> f.icon = value)));
        } else if (slot == ENTITY_TYPE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el EntityType (VILLAGER, ZOMBIE...):",
                    value -> replace(withField(f -> f.entityType = value)));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(withField(f -> f.description = value)));
        } else if (slot == SKILLS_SLOT) {
            chatPromptManager.prompt(player, "Escribí ids de habilidad separados por comas:",
                    value -> replace(withField(f -> f.skillIds = parseSet(value))));
        } else if (slot == AI_RULES_SLOT) {
            chatPromptManager.prompt(player, "Escribí 'CONDICION;ACCION;prioridad' para agregar una regla:", this::addRule);
        } else if (slot == CLEAR_RULES_SLOT) {
            replace(withField(f -> f.aiRules = List.of()));
        } else if (slot == SCHEDULE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del horario (o 'ninguno'):", value -> replace(withField(
                    f -> f.scheduleId = value.equalsIgnoreCase("ninguno") ? null : value.trim().toLowerCase(Locale.ROOT))));
        } else if (slot == WAGE_AMOUNT_SLOT) {
            replace(withField(f -> f.wageAmount = Math.max(0, current.wageAmount() + sign * 10)));
        } else if (slot == WAGE_TYPE_SLOT) {
            WageType[] values = WageType.values();
            replace(withField(f -> f.wageType = values[(current.wageType().ordinal() + 1) % values.length]));
        } else if (slot == TOOL_SLOT) {
            chatPromptManager.prompt(player, "Escribí un Material de herramienta (o 'ninguna'):", value -> replace(withField(
                    f -> f.toolMaterial = value.equalsIgnoreCase("ninguna") ? null : value)));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void addRule(String raw) {

        String[] parts = raw.split(";");

        if (parts.length < 3) {
            player.sendMessage(Component.text("Formato inválido — necesita 3 partes separadas por ';'.", NamedTextColor.RED));
            build();
            return;
        }

        AiCondition condition;
        AiAction action;

        try {
            condition = AiCondition.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            action = AiAction.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Condición o acción inválida.", NamedTextColor.RED));
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

        mutator.accept(fields);

        return new Profession(current.id(), fields.name, fields.icon, fields.description, fields.entityType,
                fields.skillIds, fields.aiRules, fields.scheduleId, fields.wageAmount, fields.wageType,
                fields.toolMaterial);
    }

}
