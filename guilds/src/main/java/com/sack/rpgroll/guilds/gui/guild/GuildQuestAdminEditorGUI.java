package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestDefinition;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestManager;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Editor administrativo de una {@link GuildQuestDefinition} — distinto de GuildQuestsGUI (que es para que un miembro la inicie). */
public class GuildQuestAdminEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 9;
    private static final int TYPE_SLOT = 10;
    private static final int TARGET_REF_SLOT = 11;
    private static final int TARGET_AMOUNT_SLOT = 12;
    private static final int REWARD_MONEY_SLOT = 13;
    private static final int REWARD_XP_SLOT = 14;
    private static final int MIN_LEVEL_SLOT = 15;
    private static final int DESCRIPTION_SLOT = 16;
    private static final int BACK_SLOT = 26;

    private final GuildQuestManager questManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private GuildQuestDefinition current;

    public GuildQuestAdminEditorGUI(Player player, GuildQuestDefinition definition, GuildQuestManager questManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("guildadmin.quest.editor.title", "id",
                definition.id()), NamedTextColor.GOLD), SIZE);
        this.current = definition;
        this.questManager = questManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private com.sack.rpgroll.common.lang.LangManager lang() {
        return chatPromptManager.lang();
    }

    private void replace(GuildQuestDefinition updated) {
        current = updated;
        questManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text(lang().raw("guildadmin.quest.name_label", "name", current.displayName()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("guildadmin.quest.lore.click_to_type_new"), NamedTextColor.GRAY))
                .build());

        setItem(TYPE_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(Component.text(lang().raw("guildadmin.quest.type_label", "type", current.type()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("guildadmin.quest.lore.click_next"), NamedTextColor.GRAY),
                        Component.text(lang().raw("guildadmin.quest.lore.win_war_note"), NamedTextColor.DARK_GRAY))
                .build());

        setItem(TARGET_REF_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text(lang().raw("guildadmin.quest.reference_label", "reference",
                        current.targetReference() == null ? lang().raw("guildadmin.quest.reference_none")
                                : current.targetReference()), NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("guildadmin.quest.lore.reference_example"), NamedTextColor.GRAY),
                        Component.text(lang().raw("guildadmin.quest.lore.click_to_type_new"), NamedTextColor.GRAY))
                .build());

        setItem(TARGET_AMOUNT_SLOT, new ItemBuilder(Material.TARGET)
                .setName(Component.text(lang().raw("guildadmin.quest.target_amount_label", "amount",
                        current.targetAmount()), NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("guildadmin.quest.lore.click_increments"), NamedTextColor.GRAY))
                .build());

        setItem(REWARD_MONEY_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(Component.text(lang().raw("guildadmin.quest.reward_money_label", "amount",
                        current.rewardMoney()), NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("guildadmin.quest.lore.click_100"), NamedTextColor.GRAY))
                .build());

        setItem(REWARD_XP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text(lang().raw("guildadmin.quest.reward_xp_label", "amount", current.rewardXp()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("guildadmin.quest.lore.click_50"), NamedTextColor.GRAY))
                .build());

        setItem(MIN_LEVEL_SLOT, new ItemBuilder(Material.LADDER)
                .setName(Component.text(lang().raw("guildadmin.quest.min_level_label", "level",
                        current.minGuildLevel()), NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("guildadmin.quest.lore.click_increments"), NamedTextColor.GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text(lang().raw("guildadmin.quest.description_label"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? lang().raw("guild.customization.no_description") : current.description()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "guildadmin.quest.prompt_name",
                    value -> replace(new GuildQuestDefinition(current.id(), value.trim(), current.description(),
                            current.type(), current.targetReference(), current.targetAmount(), current.rewardMoney(),
                            current.rewardXp(), current.minGuildLevel())));
            return;
        }

        if (slot == TYPE_SLOT) {
            GuildQuestType[] values = GuildQuestType.values();
            GuildQuestType next = values[(current.type().ordinal() + 1) % values.length];
            replace(new GuildQuestDefinition(current.id(), current.displayName(), current.description(), next,
                    current.targetReference(), current.targetAmount(), current.rewardMoney(), current.rewardXp(),
                    current.minGuildLevel()));
            return;
        }

        if (slot == TARGET_REF_SLOT) {
            chatPromptManager.prompt(player, "guildadmin.quest.prompt_reference",
                    value -> replace(new GuildQuestDefinition(current.id(), current.displayName(),
                            current.description(), current.type(), value.isBlank() ? null : value.trim(),
                            current.targetAmount(), current.rewardMoney(), current.rewardXp(),
                            current.minGuildLevel())));
            return;
        }

        if (slot == TARGET_AMOUNT_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : event.isShiftClick() ? 10 : 1;
            replace(new GuildQuestDefinition(current.id(), current.displayName(), current.description(),
                    current.type(), current.targetReference(), Math.max(1, current.targetAmount() + delta),
                    current.rewardMoney(), current.rewardXp(), current.minGuildLevel()));
            return;
        }

        if (slot == REWARD_MONEY_SLOT) {
            double delta = click == ClickType.RIGHT ? -100 : 100;
            replace(new GuildQuestDefinition(current.id(), current.displayName(), current.description(),
                    current.type(), current.targetReference(), current.targetAmount(),
                    Math.max(0, current.rewardMoney() + delta), current.rewardXp(), current.minGuildLevel()));
            return;
        }

        if (slot == REWARD_XP_SLOT) {
            int delta = click == ClickType.RIGHT ? -50 : 50;
            replace(new GuildQuestDefinition(current.id(), current.displayName(), current.description(),
                    current.type(), current.targetReference(), current.targetAmount(), current.rewardMoney(),
                    Math.max(0, current.rewardXp() + delta), current.minGuildLevel()));
            return;
        }

        if (slot == MIN_LEVEL_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new GuildQuestDefinition(current.id(), current.displayName(), current.description(),
                    current.type(), current.targetReference(), current.targetAmount(), current.rewardMoney(),
                    current.rewardXp(), Math.max(1, current.minGuildLevel() + delta)));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "guildadmin.quest.prompt_description",
                    value -> replace(new GuildQuestDefinition(current.id(), current.displayName(), value,
                            current.type(), current.targetReference(), current.targetAmount(), current.rewardMoney(),
                            current.rewardXp(), current.minGuildLevel())));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
