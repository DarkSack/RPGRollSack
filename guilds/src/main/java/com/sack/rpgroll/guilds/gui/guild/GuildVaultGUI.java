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
        super(player, chatPromptManager.lang().component("guild.vault.title", "name", guild.name()), SIZE);
        this.guild = guild;
        this.guildManager = guildManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private com.sack.rpgroll.common.lang.LangManager lang() {
        return chatPromptManager.lang();
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
                .setName(Component.text(lang().raw("guild.vault.button.deposit"), NamedTextColor.GREEN))
                .build());

        setItem(WITHDRAW_MONEY_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text(lang().raw("guild.vault.button.withdraw"), NamedTextColor.YELLOW))
                .setLore(Component.text(lang().raw("guild.vault.lore.requires_bank_permission"), NamedTextColor.GRAY))
                .build());

        setItem(LOG_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text(lang().raw("guild.vault.button.log"), NamedTextColor.AQUA))
                .build());

        setItem(BALANCE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(lang().raw("guild.vault.balance", "amount", guild.vault().balance()),
                        NamedTextColor.GREEN))
                .setLore(Component.text(lang().raw("guild.vault.storage_slots", "count", slots()), NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("common.back")));
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
            chatPromptManager.prompt(player, "guild.vault.prompt_deposit", value -> {
                double amount = parse(value);
                if (amount <= 0) {
                    lang().send(player, "guild.vault.invalid_amount");
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
                lang().send(player, "guild.vault.no_permission_withdraw");
                return;
            }

            chatPromptManager.prompt(player, "guild.vault.prompt_withdraw", value -> {
                double amount = parse(value);
                if (amount <= 0) {
                    lang().send(player, "guild.vault.invalid_amount");
                    reopen();
                    return;
                }
                withdrawMoney(amount);
                reopen();
            });
            return;
        }

        if (slot == LOG_SLOT) {
            lang().send(player, "guild.vault.log_header");
            guild.vault().log().stream().limit(10).forEach(entry -> lang().send(player, "guild.vault.log_entry",
                    "type", entry.type(), "description", entry.description(),
                    "actor", entry.actorName() != null ? entry.actorName() : lang().raw("guild.vault.log_system")));
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
                lang().send(player, "guild.vault.need_item_in_hand");
                return;
            }

            if (slot >= storage.length || storage[slot] != null) {
                lang().send(player, "guild.vault.slot_not_free");
                return;
            }

            storage[slot] = hand.clone();
            player.getInventory().setItemInMainHand(null);

            guild.vault().log(VaultTransaction.of(player.getUniqueId(), player.getName(),
                    VaultTransactionType.DEPOSIT_ITEM, 0, lang().raw("guild.vault.log.deposited_item",
                            "item", hand.getType())));
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
                VaultTransactionType.WITHDRAW_ITEM, 0, lang().raw("guild.vault.log.withdrew_item",
                        "item", item.getType())));
        guildManager.save(guild);
        build();
    }

    private void depositMoney(double amount) {

        if (com.sack.rpgroll.api.RPGRollAPI.isReady()) {
            var economy = com.sack.rpgroll.api.RPGRollAPI.get().getEconomyProvider();
            if (economy.isAvailable() && !economy.getEconomy().get().withdrawPlayer(player, amount).transactionSuccess()) {
                lang().send(player, "guild.vault.not_enough_money");
                return;
            }
        }

        guild.vault().deposit(amount, VaultTransaction.of(player.getUniqueId(), player.getName(),
                VaultTransactionType.DEPOSIT_MONEY, amount, lang().raw("guild.vault.log.deposit_of",
                        "player", player.getName())));
        guildManager.save(guild);
        lang().send(player, "guild.vault.deposited", "amount", amount);
    }

    private void withdrawMoney(double amount) {

        boolean withdrawn = guild.vault().withdraw(amount, VaultTransaction.of(player.getUniqueId(), player.getName(),
                VaultTransactionType.WITHDRAW_MONEY, amount, lang().raw("guild.vault.log.withdraw_of",
                        "player", player.getName())));

        if (!withdrawn) {
            lang().send(player, "guild.vault.insufficient_balance");
            return;
        }

        if (com.sack.rpgroll.api.RPGRollAPI.isReady()) {
            var economy = com.sack.rpgroll.api.RPGRollAPI.get().getEconomyProvider();
            economy.getEconomy().ifPresent(eco -> eco.depositPlayer(player, amount));
        }

        guildManager.save(guild);
        lang().send(player, "guild.vault.withdrew", "amount", amount);
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
