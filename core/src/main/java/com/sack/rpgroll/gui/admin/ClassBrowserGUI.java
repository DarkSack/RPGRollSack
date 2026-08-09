package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.playerclass.ClassManagerImpl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ClassManagerImpl classManager;
    private final ChatPromptManager chatPromptManager;
    private List<PlayerClass> classes;

    public ClassBrowserGUI(Player player, ClassManagerImpl classManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Clases RPGRoll", NamedTextColor.GOLD), SIZE);
        this.classManager = classManager;
        this.chatPromptManager = chatPromptManager;
        this.classes = List.copyOf(classManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < classes.size() && i < 36; i++) {

            PlayerClass playerClass = classes.get(i);

            setItem(i, new ItemBuilder(Material.IRON_SWORD)
                    .setName(Component.text(playerClass.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text(playerClass.baseAttributes().size() + " atributo(s) base",
                            NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear clase nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < classes.size() && slot < 36) {
            new ClassEditorGUI(player, classes.get(slot), classManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id de la nueva clase:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (classManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una clase con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            PlayerClass playerClass = new PlayerClass(id, id, "", Map.of(), List.of(), "", List.of());
            classManager.save(playerClass);
            reopen();
        });
    }

    private void reopen() {
        this.classes = List.copyOf(classManager.getAll());
        open();
    }

}
