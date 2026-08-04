package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.guilds.gui.PaginatedGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.diplomacy.DiplomacyStatus;
import com.sack.rpgroll.guilds.guild.diplomacy.GuildDiplomacyManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class GuildDiplomacyGUI extends PaginatedGUI {

    private static final int SIZE = 54;
    private static final int CONTENT_SLOTS = 45;
    private static final int BACK_SLOT = 53;

    private final Guild guild;
    private final GuildManager guildManager;
    private final GuildDiplomacyManager diplomacyManager;
    private final Runnable onBack;
    private final List<Guild> others;

    public GuildDiplomacyGUI(Player player, Guild guild, GuildManager guildManager,
            GuildDiplomacyManager diplomacyManager, Runnable onBack) {
        super(player, Component.text("Diplomacia: " + guild.name(), NamedTextColor.GOLD), SIZE, CONTENT_SLOTS);
        this.guild = guild;
        this.guildManager = guildManager;
        this.diplomacyManager = diplomacyManager;
        this.onBack = onBack;
        this.others = guildManager.getAll().stream().filter(g -> !g.id().equals(guild.id())).toList();
    }

    @Override
    protected int totalItemCount() {
        return others.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        Guild other = others.get(absoluteIndex);
        DiplomacyStatus status = diplomacyManager.relation(guild.id(), other.id());

        setItem(contentSlot, new ItemBuilder(other.icon())
                .setName(Component.text(other.name() + " — " + status, statusColor(status)))
                .setLore(Component.text("Nivel " + other.level(), NamedTextColor.GRAY),
                        Component.text("Click para cambiar relación", NamedTextColor.YELLOW))
                .build());
    }

    private NamedTextColor statusColor(DiplomacyStatus status) {
        return switch (status) {
            case ALLIED -> NamedTextColor.GREEN;
            case PEACE -> NamedTextColor.AQUA;
            case NEUTRAL -> NamedTextColor.GRAY;
            case CONFLICT -> NamedTextColor.GOLD;
            case WAR -> NamedTextColor.RED;
        };
    }

    @Override
    protected void renderExtras() {

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));

        setItem(45, hasPreviousPage()
                ? new ItemBuilder(Material.ARROW).setName(Component.text("« Anterior", NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());
        setItem(53 - 1, hasNextPage()
                ? new ItemBuilder(Material.ARROW).setName(Component.text("Siguiente »", NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {

        if (!guild.roleOf(player.getUniqueId()).canManageSettings()) {
            player.sendMessage(Component.text("No tenés permiso para gestionar diplomacia.", NamedTextColor.RED));
            return;
        }

        Guild other = others.get(absoluteIndex);
        DiplomacyStatus current = diplomacyManager.relation(guild.id(), other.id());
        DiplomacyStatus[] values = DiplomacyStatus.values();
        DiplomacyStatus next = values[(current.ordinal() + 1) % values.length];

        diplomacyManager.setRelation(guild.id(), other.id(), next);
        build();
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        int slot = event.getSlot();

        if (slot == 45) {
            previousPage();
        } else if (slot == 52) {
            nextPage();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
