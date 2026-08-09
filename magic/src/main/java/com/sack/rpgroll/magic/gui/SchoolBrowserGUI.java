package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.MagicSchool;
import com.sack.rpgroll.magic.core.SchoolManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SchoolBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SchoolManager schoolManager;
    private final ChatPromptManager chatPromptManager;
    private List<MagicSchool> schools;

    public SchoolBrowserGUI(Player player, SchoolManager schoolManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Escuelas de Magia", NamedTextColor.GOLD), SIZE);
        this.schoolManager = schoolManager;
        this.chatPromptManager = chatPromptManager;
        this.schools = List.copyOf(schoolManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < schools.size() && i < 36; i++) {

            MagicSchool school = schools.get(i);
            NamedTextColor color = parseColor(school.color());

            setItem(i, new ItemBuilder(parseMaterial(school.icon()))
                    .setName(Component.text(school.displayName(), color))
                    .setLore(Component.text("id: " + school.id(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear escuela nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < schools.size() && slot < 36) {
            new SchoolEditorGUI(player, schools.get(slot), schoolManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva escuela:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (schoolManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una escuela con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            MagicSchool school = new MagicSchool(id, id, "WHITE", "BOOK", "", null, null, Map.of(), Map.of());
            schoolManager.save(school);
            reopen();
        });
    }

    private void reopen() {
        this.schools = List.copyOf(schoolManager.getAll());
        open();
    }

    static Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.BOOK;
        }
    }

    static NamedTextColor parseColor(String raw) {
        NamedTextColor color = NamedTextColor.NAMES.value(raw.trim().toLowerCase(Locale.ROOT));
        return color != null ? color : NamedTextColor.WHITE;
    }

}
