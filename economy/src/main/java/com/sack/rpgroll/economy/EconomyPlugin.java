package com.sack.rpgroll.economy;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.economy.api.EconomyAPI;
import com.sack.rpgroll.economy.auction.AuctionManager;
import com.sack.rpgroll.economy.auction.AuctionService;
import com.sack.rpgroll.economy.auction.AuctionSettings;
import com.sack.rpgroll.economy.command.AuctionCommand;
import com.sack.rpgroll.economy.auction.AuctionStore;
import com.sack.rpgroll.economy.bank.BankAccountStore;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.command.EconomyAdminCommand;
import com.sack.rpgroll.economy.command.EconomyCommand;
import com.sack.rpgroll.economy.company.CompanyManager;
import com.sack.rpgroll.economy.company.CompanyService;
import com.sack.rpgroll.economy.company.CompanyStore;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.gui.ChatPromptManager;
import com.sack.rpgroll.economy.inflation.InflationTracker;
import com.sack.rpgroll.economy.integration.EconomyPlaceholders;
import com.sack.rpgroll.economy.integration.EconomyVaultProvider;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.loan.LoanService;
import com.sack.rpgroll.economy.loan.LoanStore;
import com.sack.rpgroll.economy.integration.GuildTerritoryTaxTask;
import com.sack.rpgroll.economy.market.MarketEngine;
import com.sack.rpgroll.economy.servershop.ServerShopManager;
import com.sack.rpgroll.economy.servershop.ServerShopService;
import com.sack.rpgroll.economy.command.ServerShopCommand;
import com.sack.rpgroll.economy.market.MarketProductManager;
import com.sack.rpgroll.economy.market.MarketRegionManager;
import com.sack.rpgroll.economy.market.MarketStateStore;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.economy.shop.ShopStore;
import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.economy.tax.TaxRuleManager;
import com.sack.rpgroll.economy.wallet.WalletManager;
import com.sack.rpgroll.economy.wallet.WalletService;
import com.sack.rpgroll.economy.wallet.WalletStore;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Punto de entrada de RPGRoll-Economy. Cablea monedas/mercado/impuestos
 * (contenido YAML), wallets/bancos/préstamos/tiendas/subastas/empresas
 * (estado en vivo, persistido en {@code plugins/RPGRoll-Economy/}), y se
 * registra como proveedor del servicio Economy de Vault para que el resto
 * del ecosistema (y cualquier plugin externo) quede funcional de inmediato.
 */
public class EconomyPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("currencies", "market", "market-regions", "tax",
            "server-shop");

    private LangManager langManager;

    private CurrencyManager currencyManager;
    private MarketProductManager marketProductManager;
    private MarketRegionManager marketRegionManager;
    private TaxRuleManager taxRuleManager;

    private WalletManager walletManager;
    private WalletService walletService;
    private TransactionLedger ledger;
    private BankManager bankManager;
    private LoanService loanService;
    private TaxEngine taxEngine;
    private MarketEngine marketEngine;
    private ShopManager shopManager;
    private AuctionManager auctionManager;
    private CompanyManager companyManager;
    private CompanyService companyService;
    private InflationTracker inflationTracker;
    private ServerShopManager serverShopManager;
    private ServerShopService serverShopService;

    private AuctionService auctionService;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        String defaultCurrencyId = getConfig().getString("default-currency", "gold");

        currencyManager = new CurrencyManager(this, defaultCurrencyId);
        currencyManager.initialize();

        marketProductManager = new MarketProductManager(this);
        marketProductManager.initialize();

        marketRegionManager = new MarketRegionManager(this);
        marketRegionManager.initialize();

        taxRuleManager = new TaxRuleManager(this);
        taxRuleManager.initialize();

        WalletStore walletStore = new WalletStore(getDataFolder());
        walletManager = new WalletManager(walletStore);
        ledger = new TransactionLedger(this);
        walletService = new WalletService(walletManager, currencyManager, ledger);

        BankAccountStore bankAccountStore = new BankAccountStore(getDataFolder());
        bankManager = new BankManager(bankAccountStore, currencyManager, walletService, ledger);
        bankManager.loadAll();

        LoanStore loanStore = new LoanStore(getDataFolder());
        loanService = new LoanService(loanStore, bankManager, ledger);
        loanService.loadAll();

        taxEngine = new TaxEngine(taxRuleManager, ledger);

        MarketStateStore marketStateStore = new MarketStateStore(getDataFolder());
        marketEngine = new MarketEngine(marketProductManager, marketStateStore, marketRegionManager);
        marketEngine.loadAll();

        ShopStore shopStore = new ShopStore(getDataFolder());
        shopManager = new ShopManager(shopStore, walletService, taxEngine);
        shopManager.loadAll();

        AuctionStore auctionStore = new AuctionStore(getDataFolder(), getLogger());
        auctionManager = new AuctionManager(auctionStore, walletService, taxEngine, auctionSettings(),
                System::currentTimeMillis);
        auctionManager.loadAll();
        auctionService = new AuctionService(this, auctionManager, currencyManager, walletService, langManager);
        getServer().getPluginManager().registerEvents(auctionService, this);

        CompanyStore companyStore = new CompanyStore(getDataFolder());
        companyManager = new CompanyManager(companyStore);
        companyManager.loadAll();
        companyService = new CompanyService(companyManager, bankManager, walletService, ledger);

        inflationTracker = new InflationTracker(walletStore, bankManager, getDataFolder());
        inflationTracker.load();

        serverShopManager = new ServerShopManager(this);
        serverShopManager.initialize();
        serverShopService = new ServerShopService(walletService, currencyManager, marketProductManager, marketEngine,
                getConfig().getDouble("server-shop.market-sell-ratio", 0.4));

        EconomyAPI.init(currencyManager, marketProductManager, marketRegionManager, taxRuleManager, walletService,
                ledger, bankManager, loanService, taxEngine, marketEngine, shopManager, auctionManager,
                companyManager, companyService, inflationTracker);

        registerVault();
        registerPlaceholders();

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        registerCommands(chatPromptManager);
        startTasks();

        getLogger().info("✔ RPGRoll-Economy habilitado. " + currencyManager.count() + " moneda(s), "
                + marketProductManager.count() + " producto(s) de mercado, " + taxRuleManager.count() + " regla(s) tributaria(s).");
    }

    @Override
    public void onDisable() {

        if (walletManager != null) {
            walletManager.saveAll();
        }
        if (bankManager != null) {
            bankManager.saveAll();
        }
        if (loanService != null) {
            loanService.saveAll();
        }
        if (marketEngine != null) {
            marketEngine.saveAll();
        }
        if (shopManager != null) {
            shopManager.saveAll();
        }
        if (companyManager != null) {
            companyManager.saveAll();
        }
        if (ledger != null) {
            ledger.flush();
        }
    }

    /**
     * Prioridad con la que se registra el proveedor de Vault.
     * <p>
     * Por defecto {@code HIGHEST} y no {@code NORMAL} por un motivo concreto:
     * plugins generalistas como EssentialsX también registran una economía con
     * prioridad {@code Normal}, y Vault resuelve el empate por orden de carga.
     * Al cargar antes (alfabéticamente, o por dependencias), Essentials ganaba
     * y el resto del ecosistema terminaba moviendo dinero en SUS cuentas
     * mientras los mercados, bancos e impuestos de este addon seguían
     * operando sobre las carteras propias: dos bolsillos para el mismo
     * jugador, sin ningún aviso.
     * <p>
     * Quien instala RPGRoll-Economy quiere que la economía sea esta. Aun así
     * es configurable, porque un servidor puede querer lo contrario —
     * conservar la economía de otro plugin y usar este addon solo por sus
     * mercados.
     */
    private ServicePriority vaultPriority() {

        String configured = getConfig().getString("vault-priority", "highest");

        try {
            return ServicePriority.valueOf(
                    configured.trim().substring(0, 1).toUpperCase() + configured.trim().substring(1).toLowerCase());
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            getLogger().warning("vault-priority: '" + configured + "' no es válido"
                    + " (lowest, low, normal, high, highest). Se usa highest.");
            return ServicePriority.Highest;
        }
    }

    private void registerVault() {

        if (!getServer().getPluginManager().isPluginEnabled("Vault")) {
            getLogger().warning("✘ Vault no está instalado — RPGRoll-Economy funciona igual, pero ningún plugin "
                    + "externo que hable Vault (ni el resto del ecosistema) va a poder cobrar/pagar dinero.");
            return;
        }

        ServicePriority priority = vaultPriority();

        getServer().getServicesManager().register(Economy.class, new EconomyVaultProvider(walletService, currencyManager),
                this, priority);
        getLogger().info("✔ RPGRoll-Economy registrado como proveedor del servicio Economy de Vault"
                + " (prioridad " + priority + ").");
    }

    private void registerPlaceholders() {

        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }

        new EconomyPlaceholders(this, walletService, currencyManager, bankManager, marketEngine, taxRuleManager,
                inflationTracker).register();
    }

    private void registerCommands(ChatPromptManager chatPromptManager) {

        var adminExecutor = new EconomyAdminCommand(currencyManager, marketProductManager, marketEngine, taxRuleManager,
                    walletService, inflationTracker, chatPromptManager, this::reloadContent);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "economyadmin",
                "Comandos administrativos de RPGRoll-Economy (Economy Studio)", "rpgrolleconomy.admin.*", adminExecutor);

        var playerExecutor = new EconomyCommand(currencyManager, walletService, bankManager, loanService, shopManager,
                    taxEngine, auctionManager, companyManager, companyService, chatPromptManager,
                    auctionService);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "economy",
                "Comandos de jugador de RPGRoll-Economy", "rpgrolleconomy.use", playerExecutor);

        var shopExecutor = new ServerShopCommand(serverShopManager, serverShopService, walletService, langManager);
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "tienda", "Tienda del servidor",
                getConfig().getStringList("server-shop.aliases"), shopExecutor, shopExecutor,
                "rpgrolleconomy.servershop");

        var auctionExecutor = new AuctionCommand(auctionService, chatPromptManager);
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "subasta", "Casa de Subastas",
                getConfig().getStringList("auction-house.aliases"), auctionExecutor, auctionExecutor,
                "rpgrolleconomy.auction");
    }

    private AuctionSettings auctionSettings() {
        return AuctionSettings.from(getConfig(), message -> getLogger().warning("config.yml: " + message));
    }

    private void reloadContent() {
        reloadConfig();
        langManager.reload(getConfig().getString("language", "es"));
        currencyManager.reload();
        marketProductManager.reload();
        marketRegionManager.reload();
        taxRuleManager.reload();
        serverShopManager.reload();
        serverShopService.setSellRatio(getConfig().getDouble("server-shop.market-sell-ratio", 0.4));
        auctionManager.settings(auctionSettings());
    }

    private void startTasks() {

        long marketInterval = getConfig().getLong("market-recovery-interval-ticks", 1200);
        long loanInterval = getConfig().getLong("loan-check-interval-ticks", 12000);
        long inflationInterval = getConfig().getLong("inflation-snapshot-interval-ticks", 24000);
        long auctionInterval = getConfig().getLong("auction-check-interval-ticks", 200);

        getServer().getScheduler().runTaskTimer(this, marketEngine::runRecovery, marketInterval, marketInterval);
        getServer().getScheduler().runTaskTimer(this, loanService::accrueInterest, loanInterval, loanInterval);
        getServer().getScheduler().runTaskTimer(this, inflationTracker::takeSnapshot, inflationInterval, inflationInterval);
        getServer().getScheduler().runTaskTimer(this, auctionManager::processExpired, auctionInterval, auctionInterval);
        getServer().getScheduler().runTaskTimer(this, ledger::flush, 600, 600);

        long guildTaxInterval = getConfig().getLong("guild-territory-tax-interval-ticks", 24000);
        double guildTaxPerTerritory = getConfig().getDouble("guild-territory-tax-per-territory", 50.0);
        String defaultCurrencyId = currencyManager.defaultCurrency().id();

        GuildTerritoryTaxTask guildTerritoryTaxTask = new GuildTerritoryTaxTask(this, taxEngine, guildTaxPerTerritory,
                defaultCurrencyId);
        getServer().getScheduler().runTaskTimer(this, guildTerritoryTaxTask::run, guildTaxInterval, guildTaxInterval);
    }

}
