package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;
import com.sack.rpgroll.ascension.reward.Rewards;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Entrega un {@link Rewards}. Lo usan los logros, los rangos de facción y
 * las evoluciones de oficio, así que las tres cosas premian igual.
 */
public class RewardService {

    private final Plugin plugin;
    private final AscensionPlayerStateManager stateManager;
    private final LangManager lang;

    private TitleEngine titleEngine;
    private FactionEngine factionEngine;
    private Consumer<Player> bonusApplier = player -> { };

    public RewardService(Plugin plugin, AscensionPlayerStateManager stateManager, LangManager lang) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.lang = lang;
    }

    /** Se cablean después de construir: los motores también usan este servicio. */
    public void wire(TitleEngine titleEngine, FactionEngine factionEngine, Consumer<Player> bonusApplier) {
        this.titleEngine = titleEngine;
        this.factionEngine = factionEngine;
        this.bonusApplier = bonusApplier;
    }

    /**
     * @param sourceName nombre visible de lo que se desbloqueó, para el
     *                   anuncio público si {@link Rewards#broadcast()}
     */
    public void grant(Player player, Rewards rewards, String sourceName) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        if (rewards.money() > 0) {
            payMoney(player, rewards.money());
        }

        if (rewards.experience() > 0) {
            giveExperience(player, rewards.experience());
        }

        if (rewards.talentPoints() > 0) {
            state.addTalentPoints(rewards.talentPoints());
            lang.send(player, "progress.reward_talent_points", "amount", rewards.talentPoints());
        }

        if (rewards.title() != null && titleEngine != null) {
            titleEngine.unlock(player, rewards.title());
        }

        if (factionEngine != null) {
            for (Map.Entry<String, Integer> entry : rewards.reputation().entrySet()) {
                factionEngine.addReputation(player, entry.getKey(), entry.getValue());
            }
        }

        if (!rewards.stats().isEmpty()) {
            rewards.stats().forEach(state::addBonusStat);
            bonusApplier.accept(player);
        }

        for (String itemId : rewards.items()) {
            giveItem(player, itemId);
        }

        for (String command : rewards.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        }

        if (rewards.message() != null && !rewards.message().isBlank()) {
            player.sendMessage(ComponentUtils.parse(rewards.message().replace("{player}", player.getName())));
        }

        if (rewards.broadcast()) {
            String line = lang.raw("progress.broadcast", "player", player.getName(), "name", sourceName);
            Bukkit.getOnlinePlayers().forEach(online -> online.sendMessage(ComponentUtils.parse(line)));
        }
    }

    private void payMoney(Player player, double amount) {

        if (!RPGRollAPI.isReady() || !Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            plugin.getLogger().warning("✘ Recompensa de " + amount + " de dinero a " + player.getName()
                    + " sin entregar: no hay Vault ni economía.");
            return;
        }

        RPGRollAPI.get().getEconomyProvider().getEconomy().ifPresent(economy -> {
            economy.depositPlayer(player, amount);
            lang.send(player, "progress.reward_money", "amount", economy.format(amount));
        });
    }

    private void giveExperience(Player player, int amount) {

        if (!RPGRollAPI.isReady()) {
            return;
        }

        RPGRollAPI.get().getPlayer(player.getUniqueId()).ifPresent(rpgPlayer -> {
            RPGRollAPI.get().getPlayerManager().savePlayer(rpgPlayer.addExperience(amount));
            lang.send(player, "progress.reward_experience", "amount", amount);
        });
    }

    /**
     * Por comando de consola, como hacen las crates: así Ascension no depende
     * de las clases de RPGRoll-Items y funciona igual sin él (el ítem
     * simplemente no se entrega, y se avisa en consola).
     */
    private void giveItem(Player player, String itemId) {

        if (!Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Items")) {
            plugin.getLogger().warning("✘ Recompensa '" + itemId + "' a " + player.getName()
                    + " sin entregar: RPGRoll-Items no está instalado.");
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "itemadmin give " + player.getName() + " " + itemId + " 1");
    }

}
