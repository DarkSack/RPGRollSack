package com.sack.rpgroll.economy.api;

import com.sack.rpgroll.economy.auction.AuctionManager;
import com.sack.rpgroll.economy.bank.BankManager;
import com.sack.rpgroll.economy.company.CompanyManager;
import com.sack.rpgroll.economy.company.CompanyService;
import com.sack.rpgroll.economy.currency.CurrencyManager;
import com.sack.rpgroll.economy.inflation.InflationTracker;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.loan.LoanService;
import com.sack.rpgroll.economy.market.MarketEngine;
import com.sack.rpgroll.economy.market.MarketProductManager;
import com.sack.rpgroll.economy.shop.ShopManager;
import com.sack.rpgroll.economy.tax.TaxEngine;
import com.sack.rpgroll.economy.tax.TaxRuleManager;
import com.sack.rpgroll.economy.wallet.WalletService;

/**
 * Punto de entrada público de RPGRoll-Economy. Cualquier otro addon
 * (RPGRoll-Ranching, RPGRoll-Fishing, un futuro RPGRoll-Farming/Mining/
 * Crafting, RPGRoll-Workers...) que quiera cotizar su producción o cobrar/
 * pagar en el mercado dinámico entra por acá — típicamente vía
 * {@code market().get(productoId)} + {@code marketEngine().price(producto)}
 * para el precio actual, y {@code wallet()} para acreditar/debitar al jugador.
 */
public final class EconomyAPI {

    private static EconomyAPI instance;

    private final CurrencyManager currencyManager;
    private final MarketProductManager marketProductManager;
    private final TaxRuleManager taxRuleManager;
    private final WalletService walletService;
    private final TransactionLedger ledger;
    private final BankManager bankManager;
    private final LoanService loanService;
    private final TaxEngine taxEngine;
    private final MarketEngine marketEngine;
    private final ShopManager shopManager;
    private final AuctionManager auctionManager;
    private final CompanyManager companyManager;
    private final CompanyService companyService;
    private final InflationTracker inflationTracker;

    private EconomyAPI(CurrencyManager currencyManager, MarketProductManager marketProductManager,
            TaxRuleManager taxRuleManager, WalletService walletService, TransactionLedger ledger,
            BankManager bankManager, LoanService loanService, TaxEngine taxEngine, MarketEngine marketEngine,
            ShopManager shopManager, AuctionManager auctionManager, CompanyManager companyManager,
            CompanyService companyService, InflationTracker inflationTracker) {
        this.currencyManager = currencyManager;
        this.marketProductManager = marketProductManager;
        this.taxRuleManager = taxRuleManager;
        this.walletService = walletService;
        this.ledger = ledger;
        this.bankManager = bankManager;
        this.loanService = loanService;
        this.taxEngine = taxEngine;
        this.marketEngine = marketEngine;
        this.shopManager = shopManager;
        this.auctionManager = auctionManager;
        this.companyManager = companyManager;
        this.companyService = companyService;
        this.inflationTracker = inflationTracker;
    }

    public static void init(CurrencyManager currencyManager, MarketProductManager marketProductManager,
            TaxRuleManager taxRuleManager, WalletService walletService, TransactionLedger ledger,
            BankManager bankManager, LoanService loanService, TaxEngine taxEngine, MarketEngine marketEngine,
            ShopManager shopManager, AuctionManager auctionManager, CompanyManager companyManager,
            CompanyService companyService, InflationTracker inflationTracker) {
        instance = new EconomyAPI(currencyManager, marketProductManager, taxRuleManager, walletService, ledger,
                bankManager, loanService, taxEngine, marketEngine, shopManager, auctionManager, companyManager,
                companyService, inflationTracker);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-Economy todavía no está listo. */
    public static EconomyAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-Economy todavía no está listo.");
        }

        return instance;
    }

    public CurrencyManager currencies() {
        return currencyManager;
    }

    public MarketProductManager market() {
        return marketProductManager;
    }

    public MarketEngine marketEngine() {
        return marketEngine;
    }

    public TaxRuleManager taxRules() {
        return taxRuleManager;
    }

    public TaxEngine tax() {
        return taxEngine;
    }

    public WalletService wallet() {
        return walletService;
    }

    public TransactionLedger ledger() {
        return ledger;
    }

    public BankManager bank() {
        return bankManager;
    }

    public LoanService loans() {
        return loanService;
    }

    public ShopManager shops() {
        return shopManager;
    }

    public AuctionManager auctions() {
        return auctionManager;
    }

    public CompanyManager companies() {
        return companyManager;
    }

    public CompanyService companyService() {
        return companyService;
    }

    public InflationTracker inflation() {
        return inflationTracker;
    }

}
