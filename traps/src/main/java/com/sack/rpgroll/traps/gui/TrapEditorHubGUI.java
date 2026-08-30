package com.sack.rpgroll.traps.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.core.TrapDefinition;
import com.sack.rpgroll.traps.core.TrapManager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Hub del editor de una TrapDefinition: secciones Info/Trigger, Acciones, Condiciones, Bloque/Llave, Camuflaje, Cadena. */
public class TrapEditorHubGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int INFO_SLOT = 10;
    private static final int ACTIONS_SLOT = 11;
    private static final int CONDITIONS_SLOT = 12;
    private static final int BLOCK_SLOT = 13;
    private static final int DISGUISE_SLOT = 14;
    private static final int CHAIN_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private final TrapManager trapManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private String trapId;

    public TrapEditorHubGUI(Player player, TrapDefinition trap, TrapManager trapManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.hub.title", "id", trap.id()), SIZE);
        this.trapId = trap.id();
        this.trapManager = trapManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private LangManager lang() {
        return chatPromptManager.lang();
    }

    private TrapDefinition current() {
        return trapManager.get(trapId).orElseThrow();
    }

    private void save(TrapDefinition updated) {
        this.trapId = updated.id();
        trapManager.save(updated);
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(INFO_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(lang().component("gui.hub.info"))
                .setLore(lang().component("gui.hub.info_desc"))
                .build());

        setItem(ACTIONS_SLOT, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(lang().component("gui.hub.actions"))
                .setLore(lang().component("gui.hub.actions_desc", "count", current().actions().size()))
                .build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(lang().component("gui.hub.conditions"))
                .setLore(lang().component("gui.hub.conditions_desc", "count", current().conditions().size()))
                .build());

        setItem(BLOCK_SLOT, new ItemBuilder(Material.IRON_DOOR)
                .setName(lang().component("gui.hub.block"))
                .setLore(lang().component("gui.hub.block_desc"))
                .build());

        setItem(DISGUISE_SLOT, new ItemBuilder(Material.LEATHER)
                .setName(lang().component("gui.hub.disguise"))
                .setLore(lang().component("gui.hub.disguise_desc"))
                .build());

        setItem(CHAIN_SLOT, new ItemBuilder(Material.IRON_CHAIN)
                .setName(lang().component("gui.hub.chain"))
                .setLore(lang().component("gui.hub.chain_desc", "count", current().chain().size()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == INFO_SLOT) {
            new TrapInfoEditorGUI(player, current(), chatPromptManager, this::save, this::reopen).open();
            return;
        }

        if (slot == ACTIONS_SLOT) {
            new TrapActionsEditorGUI(player, lang().raw("gui.hub.actions"), current().actions(), chatPromptManager,
                    actions -> save(withActions(actions)), this::reopen).open();
            return;
        }

        if (slot == CONDITIONS_SLOT) {
            new StringListEditorGUI(player, lang().raw("gui.hub.conditions"), current().conditions(),
                    chatPromptManager, "gui.conditions.prompt_add",
                    conditions -> save(withConditions(conditions)), this::reopen).open();
            return;
        }

        if (slot == BLOCK_SLOT) {
            new TrapBlockKeyEditorGUI(player, current().block(), chatPromptManager,
                    block -> save(withBlock(block)), this::reopen).open();
            return;
        }

        if (slot == DISGUISE_SLOT) {
            new TrapDisguiseEditorGUI(player, current().disguise(), chatPromptManager,
                    disguise -> save(withDisguise(disguise)), this::reopen).open();
            return;
        }

        if (slot == CHAIN_SLOT) {
            new StringListEditorGUI(player, lang().raw("gui.hub.chain"), current().chain(), chatPromptManager,
                    "gui.chain.prompt_add", chain -> save(withChain(chain)), this::reopen).open();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        open();
    }

    private TrapDefinition withActions(java.util.List<com.sack.rpgroll.traps.core.TrapAction> actions) {
        TrapDefinition c = current();
        return new TrapDefinition(c.id(), c.displayName(), c.description(), c.icon(), c.trigger(),
                c.triggerParams(), c.radius(), c.conditions(), actions, c.cooldownMillis(), c.charges(), c.chain(),
                c.block(), c.disguise());
    }

    private TrapDefinition withConditions(java.util.List<String> conditions) {
        TrapDefinition c = current();
        return new TrapDefinition(c.id(), c.displayName(), c.description(), c.icon(), c.trigger(),
                c.triggerParams(), c.radius(), conditions, c.actions(), c.cooldownMillis(), c.charges(), c.chain(),
                c.block(), c.disguise());
    }

    private TrapDefinition withBlock(com.sack.rpgroll.traps.core.TrapBlockConfig block) {
        TrapDefinition c = current();
        return new TrapDefinition(c.id(), c.displayName(), c.description(), c.icon(), c.trigger(),
                c.triggerParams(), c.radius(), c.conditions(), c.actions(), c.cooldownMillis(), c.charges(),
                c.chain(), block, c.disguise());
    }

    private TrapDefinition withDisguise(com.sack.rpgroll.traps.core.TrapDisguiseConfig disguise) {
        TrapDefinition c = current();
        return new TrapDefinition(c.id(), c.displayName(), c.description(), c.icon(), c.trigger(),
                c.triggerParams(), c.radius(), c.conditions(), c.actions(), c.cooldownMillis(), c.charges(),
                c.chain(), c.block(), disguise);
    }

    private TrapDefinition withChain(java.util.List<String> chain) {
        TrapDefinition c = current();
        return new TrapDefinition(c.id(), c.displayName(), c.description(), c.icon(), c.trigger(),
                c.triggerParams(), c.radius(), c.conditions(), c.actions(), c.cooldownMillis(), c.charges(), chain,
                c.block(), c.disguise());
    }

}
