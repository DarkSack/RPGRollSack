package com.sack.rpgroll.guilds.gui.guild;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.bank.VaultTransaction;
import com.sack.rpgroll.guilds.guild.bank.VaultTransactionType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Banco + almacén compartido. Los primeros {@code vaultSlots()} espacios
 * reflejan directamente {@code guild.vault().storage()} — click retira
 * (todo el stack), shift-click deposita el ítem que tengas en la mano.
 */
public class GuildVaultGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int DEPOSIT_MONEY_SLOT = 45;
    private static final int WITHDRAW_MONEY_SLOT = 46;
    private static final int LOG_SLOT = 48;
    private static final int BALANCE_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final Guild guild;
    private final GuildManager guildManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;

    public GuildVaultGUI(Player player, Guild guild, GuildManager guildManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Vault: " + guild.name(), NamedTextColor.GOLD), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private int slots() {
        return Math.min(45, guild.upgradeTree().vaultSlots());
    }

    @Override
    public void build() {

        clear();

        ItemStack[] storage = guild.vault().storage();

        for (int i = 0; i < SIZE; i++) {

            if (i < 45) {
                setItem(i, i < slots() && i < storage.length && storage[i] != null
                        ? storage[i]
                        : (i < slots() ? new ItemStack(Material.AIR) : ItemBuilder.createFiller()));
                continue;
            }
        }

        setItem(DEPOSIT_MONEY_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(Component.text("Depositar dinero", NamedTextColor.GREEN))
                .build());

        setItem(WITHDRAW_MONEY_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text("Retirar dinero", NamedTextColor.YELLOW))
                .setLore(Component.text("Requiere permiso de gestión de banco", NamedTextColor.GRAY))
                .build());

        setItem(LOG_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Ver historial (chat)", NamedTextColor.AQUA))
                .build());

        setItem(BALANCE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Saldo: " + guild.vault().balance(), NamedTextColor.GREEN))
                .setLore(Component.text("Espacios de almacenamiento: " + slots(), NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < slots()) {
            handleStorageClick(event, slot);
            return;
        }

        if (slot == DEPOSIT_MONEY_SLOT) {
            chatPromptManager.prompt(player, "Escribí cuánto dinero depositar:", value -> {
                double amount = parse(value);
                if (amount <= 0) {
                    player.sendMessage(Component.text("Monto inválido.", NamedTextColor.RED));
                    reopen();
                    return;
                }
                depositMoney(amount);
                reopen();
            });
            return;
        }

        if (slot == WITHDRAW_MONEY_SLOT) {

            if (!guild.roleOf(player.getUniqueId()).canManageBank()) {
                player.sendMessage(Component.text("No tenés permiso para retirar del banco.", NamedTextColor.RED));
                return;
            }

            chatPromptManager.prompt(player, "Escribí cuánto dinero retirar:", value -> {
                double amount = parse(value);
                if (amount <= 0) {
                    player.sendMessage(Component.text("Monto inválido.", NamedTextColor.RED));
                    reopen();
                    return;
                }
                withdrawMoney(amount);
                reopen();
            });
            return;
        }

        if (slot == LOG_SLOT) {
            player.sendMessage(Component.text("=== Historial del vault (últimos 10) ===", NamedTextColor.GOLD));
            guild.vault().log().stream().limit(10).forEach(entry -> player.sendMessage(
                    Component.text(" - [" + entry.type() + "] " + entry.description() + " ("
                            + (entry.actorName() != null ? entry.actorName() : "sistema") + ")", NamedTextColor.GRAY)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void handleStorageClick(InventoryClickEvent event, int slot) {

        ItemStack[] storage = guild.vault().storage();

        if (event.isShiftClick()) {

            ItemStack hand = player.getInventory().getItemInMainHand();

            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage(Component.text("Tenés que tener un ítem en la mano para depositar.",
                        NamedTextColor.RED));
                return;
            }

            if (slot >= storage.length || storage[slot] != null) {
                player.sendMessage(Component.text("Ese espacio no está libre.", NamedTextColor.RED));
                return;
            }

            storage[slot] = hand.clone();
            player.getInventory().setItemInMainHand(null);

            guild.vault().log(VaultTransaction.of(player.getUniqueId(), player.getName(),
                    VaultTransactionType.DEPOSIT_ITEM, 0, "Depositó " + hand.getType()));
            guildManager.save(guild);
            build();
            return;
        }

        if (slot >= storage.length || storage[slot] == null) {
            return;
        }

        ItemStack item = storage[slot];
        storage[slot] = null;

        var leftover = player.getInventory().addItem(item);
        leftover.values().forEach(overflow -> player.getWorld().dropItem(player.getLocation(), overflow));

        guild.vault().log(VaultTransaction.of(player.getUniqueId(), player.getName(),
                VaultTransactionType.WITHDRAW_ITEM, 0, "Retiró " + item.getType()));
        guildManager.save(guild);
        build();
    }

    private void depositMoney(double amount) {

        if (com.sack.rpgroll.api.RPGRollAPI.isReady()) {
            var economy = com.sack.rpgroll.api.RPGRollAPI.get().getEconomyProvider();
            if (economy.isAvailable() && !economy.getEconomy().get().withdrawPlayer(player, amount).transactionSuccess()) {
                player.sendMessage(Component.text("No tenés suficiente dinero.", NamedTextColor.RED));
                return;
            }
        }

        guild.vault().deposit(amount, VaultTransaction.of(player.getUniqueId(), player.getName(),
                VaultTransactionType.DEPOSIT_MONEY, amount, "Depósito de " + player.getName()));
        guildManager.save(guild);
        player.sendMessage(Component.text("✔ Depositaste " + amount + ".", NamedTextColor.GREEN));
    }

    private void withdrawMoney(double amount) {

        boolean withdrawn = guild.vault().withdraw(amount, VaultTransaction.of(player.getUniqueId(), player.getName(),
                VaultTransactionType.WITHDRAW_MONEY, amount, "Retiro de " + player.getName()));

        if (!withdrawn) {
            player.sendMessage(Component.text("No hay saldo suficiente en el vault.", NamedTextColor.RED));
            return;
        }

        if (com.sack.rpgroll.api.RPGRollAPI.isReady()) {
            var economy = com.sack.rpgroll.api.RPGRollAPI.get().getEconomyProvider();
            economy.getEconomy().ifPresent(eco -> eco.depositPlayer(player, amount));
        }

        guildManager.save(guild);
        player.sendMessage(Component.text("✔ Retiraste " + amount + ".", NamedTextColor.GREEN));
    }

    private double parse(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void reopen() {
        new GuildVaultGUI(player, guild, guildManager, chatPromptManager, onBack).open();
    }

}
