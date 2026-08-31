package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.genetics.Gene;
import com.sack.rpgroll.ranching.core.genetics.GeneDominance;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GeneBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final GeneManager geneManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Gene> genes;

    public GeneBrowserGUI(Player player, GeneManager geneManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.hub.genes"), NamedTextColor.GOLD), SIZE);
        this.geneManager = geneManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.genes = List.copyOf(geneManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < genes.size() && i < 36; i++) {

            Gene gene = genes.get(i);

            setItem(i, new ItemBuilder(Material.NETHER_STAR)
                    .setName(ComponentUtils.parse(gene.displayName()))
                    .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.browser.id_line", "id", gene.id()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.gene.browser.attribute_key_line", "key", gene.attributeKey()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.gene.browser.dominance_line", "dominance", gene.dominance()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_to_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.gene.browser.new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < genes.size() && slot < 36) {
            new GeneEditorGUI(player, genes.get(slot), geneManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.gene.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (geneManager.exists(id)) {
                player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.gene.browser.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            geneManager.save(new Gene(id, id, "", id, GeneDominance.MIXED, 0, 100, Set.of(), List.of()));
            reopen();
        });
    }

    private void reopen() {
        this.genes = List.copyOf(geneManager.getAll());
        open();
    }

}
