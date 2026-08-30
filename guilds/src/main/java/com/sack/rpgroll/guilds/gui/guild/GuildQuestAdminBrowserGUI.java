package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.gui.PaginatedGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestDefinition;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestManager;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestType;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/** Navegador administrativo de definiciones de quest de guild — crear/editar el contenido, no iniciarlas. */
public class GuildQuestAdminBrowserGUI extends PaginatedGUI {

    private static final int SIZE = 45;
    private static final int CONTENT_SLOTS = 36;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final GuildQuestManager questManager;
    private final ChatPromptManager chatPromptManager;
    private List<GuildQuestDefinition> definitions;

    public GuildQuestAdminBrowserGUI(Player player, GuildQuestManager questManager,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text(chatPromptManager.lang().raw("guildadmin.quest.browser.title"),
                NamedTextColor.GOLD), SIZE, CONTENT_SLOTS);
        this.questManager = questManager;
        this.chatPromptManager = chatPromptManager;
        this.definitions = List.copyOf(questManager.getAll());
    }

    private com.sack.rpgroll.common.lang.LangManager lang() {
        return chatPromptManager.lang();
    }

    @Override
    protected int totalItemCount() {
        return definitions.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        GuildQuestDefinition definition = definitions.get(absoluteIndex);

        setItem(contentSlot, new ItemBuilder(Material.MAP)
                .setName(ComponentUtils.parse(definition.displayName()))
                .setLore(Component.text(definition.id(), NamedTextColor.DARK_GRAY),
                        Component.text(lang().raw("guild.quests.type_target", "type", definition.type(),
                                "target", definition.targetAmount()), NamedTextColor.GRAY),
                        Component.text(lang().raw("guildadmin.quest.click_edit"), NamedTextColor.YELLOW))
                .build());
    }

    @Override
    protected void renderExtras() {

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(lang().raw("guildadmin.quest.button.new"), NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("common.close")));
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {
        new GuildQuestAdminEditorGUI(player, definitions.get(absoluteIndex), questManager, chatPromptManager,
                this::reopen).open();
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        switch (event.getSlot()) {
            case NEW_SLOT -> promptNew();
            case BACK_SLOT -> close();
            default -> {
            }
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "guildadmin.quest.prompt_new_id", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (questManager.exists(id)) {
                lang().send(player, "guildadmin.quest.id_already_exists");
                reopen();
                return;
            }

            GuildQuestDefinition definition = new GuildQuestDefinition(id, id, "", GuildQuestType.GATHER_RESOURCE,
                    null, 1, 0, 0, 1);

            questManager.save(definition);
            reopen();
        });
    }

    private void reopen() {
        this.definitions = List.copyOf(questManager.getAll());
        open();
    }

}
