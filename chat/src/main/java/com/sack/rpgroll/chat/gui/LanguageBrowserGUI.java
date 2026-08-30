package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.language.Language;
import com.sack.rpgroll.chat.language.LanguageManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class LanguageBrowserGUI extends PaginatedGUI {

    private static final int SIZE = 45;
    private static final int CONTENT_SLOTS = 36;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final LanguageManager languageManager;
    private final ChatPromptManager chatPromptManager;
    private List<Language> languages;

    public LanguageBrowserGUI(Player player, LanguageManager languageManager, ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("language.browser_title"), SIZE, CONTENT_SLOTS);
        this.languageManager = languageManager;
        this.chatPromptManager = chatPromptManager;
        this.languages = List.copyOf(languageManager.getAll());
    }

    @Override
    protected int totalItemCount() {
        return languages.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        Language language = languages.get(absoluteIndex);
        LangManager lang = chatPromptManager.lang();

        setItem(contentSlot, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(ComponentUtils.parse(language.displayName()))
                .setLore(Component.text(language.id(), NamedTextColor.DARK_GRAY),
                        lang.component("language.browser_lore_races", "value",
                                        language.defaultForRaces().isEmpty() ? lang.raw("language.races_universal")
                                                : String.join(", ", language.defaultForRaces()))
                                .colorIfAbsent(NamedTextColor.GRAY),
                        lang.component("gui.click_edit").colorIfAbsent(NamedTextColor.YELLOW))
                .build());
    }

    @Override
    protected void renderExtras() {

        LangManager lang = chatPromptManager.lang();

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("language.create_new").colorIfAbsent(NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.close")));
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {
        new LanguageEditorGUI(player, languages.get(absoluteIndex), languageManager, chatPromptManager, this::reopen)
                .open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("language.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (languageManager.exists(id)) {
                chatPromptManager.lang().send(player, "language.already_exists");
                reopen();
                return;
            }

            Language language = new Language(id, id, '?', List.of());
            languageManager.save(language);
            reopen();
        });
    }

    private void reopen() {
        this.languages = List.copyOf(languageManager.getAll());
        open();
    }

}
