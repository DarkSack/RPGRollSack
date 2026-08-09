package com.sack.rpgroll.crates.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.crates.core.Crate;
import com.sack.rpgroll.crates.core.CrateActionExecutor;
import com.sack.rpgroll.crates.core.CrateReward;
import com.sack.rpgroll.crates.core.CrateRewardSelector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * GUI de apertura de un crate: anima una "ruleta" horizontal (fila
 * central de un cofre de 3 filas) que se desliza y desacelera hasta
 * detenerse sobre la recompensa ya sorteada.
 * <p>
 * Importante: el sorteo ({@link CrateRewardSelector#select}) ocurre en
 * el constructor, ANTES de que arranque cualquier animación — la
 * animación es puramente visual y nunca decide el resultado. Esto evita
 * que un lag del servidor o un cierre anticipado de la GUI puedan
 * alterar qué le toca al jugador.
 */
public class CrateSpinGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int[] REEL_SLOTS = { 9, 10, 11, 12, 13, 14, 15, 16, 17 };
    private static final int CENTER_OFFSET = 4; // índice del slot central dentro de REEL_SLOTS
    private static final int POINTER_TOP_SLOT = 4;
    private static final int POINTER_BOTTOM_SLOT = 22;

    private static final int TOTAL_STEPS = 45;
    private static final int MIN_DELAY_TICKS = 1;
    private static final int MAX_DELAY_TICKS = 9;
    private static final long REWARD_GRANT_DELAY_TICKS = 20L;
    private static final long AUTO_CLOSE_DELAY_TICKS = 80L;


    private final Plugin plugin;
    private final CrateActionExecutor actionExecutor;
    private final List<CrateReward> reel;
    private final CrateReward winningReward;

    private BukkitTask animationTask;
    private int currentStep = 0;

    public CrateSpinGUI(Plugin plugin, Player player, Crate crate, CrateActionExecutor actionExecutor) {
        super(player, Component.text(crate.guiTitle(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD), SIZE);
        this.plugin = plugin;
        this.actionExecutor = actionExecutor;
        this.winningReward = CrateRewardSelector.select(crate.rewards());
        this.reel = buildReel(crate.rewards(), winningReward);
    }

    @Override
    public void open() {
        super.open();
        scheduleNextStep();
    }

    /**
     * Arma la secuencia completa que se va a "deslizar": recompensas
     * aleatorias de relleno, con la recompensa ganadora fijada en la
     * posición exacta donde el slot central va a terminar quedando.
     */
    private List<CrateReward> buildReel(List<CrateReward> pool, CrateReward winner) {

        List<CrateReward> sequence = new ArrayList<>();
        int length = TOTAL_STEPS + REEL_SLOTS.length;

        for (int i = 0; i < length; i++) {
            sequence.add(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
        }

        sequence.set(TOTAL_STEPS + CENTER_OFFSET, winner);

        return sequence;
    }

    @Override
    public void build() {

        clear();

        for (int i = 0; i < SIZE; i++) {
            if (!isReelSlot(i)) {
                setItem(i, ItemBuilder.createFiller());
            }
        }

        setItem(POINTER_TOP_SLOT, new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
                .setName(Component.text("▼", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .build());

        setItem(POINTER_BOTTOM_SLOT, new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
                .setName(Component.text("▲", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .build());

        renderWindow(0);
    }

    private boolean isReelSlot(int slot) {
        for (int reelSlot : REEL_SLOTS) {
            if (reelSlot == slot) {
                return true;
            }
        }
        return false;
    }

    private void renderWindow(int startIndex) {
        for (int i = 0; i < REEL_SLOTS.length; i++) {
            setItem(REEL_SLOTS[i], toDisplayItem(reel.get(startIndex + i)));
        }
    }

    private ItemStack toDisplayItem(CrateReward reward) {

        List<Component> lore = new ArrayList<>();
        for (String line : reward.lore()) {
            lore.add(ComponentUtils.parse(line));
        }

        return new ItemBuilder(reward.icon())
                .setName(ComponentUtils.parse(reward.displayName()).decorate(TextDecoration.BOLD))
                .setLore(lore)
                .build();
    }

    private void scheduleNextStep() {

        int delay = computeDelay(currentStep);

        animationTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {

            currentStep++;
            renderWindow(currentStep);

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);

            if (currentStep >= TOTAL_STEPS) {
                onSpinFinished();
            } else {
                scheduleNextStep();
            }

        }, delay);
    }

    /**
     * Curva de easing simple: arranca rápido (MIN_DELAY_TICKS) y termina
     * lento (MAX_DELAY_TICKS), acelerando la desaceleración hacia el
     * final (ease-in cuadrático) para que se sienta como una ruleta real
     * frenando de a poco.
     */
    private int computeDelay(int step) {
        double progress = (double) step / TOTAL_STEPS;
        double eased = progress * progress;
        return MIN_DELAY_TICKS + (int) Math.round((MAX_DELAY_TICKS - MIN_DELAY_TICKS) * eased);
    }

    private void onSpinFinished() {

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        player.sendMessage(Component.text("¡Ganaste: ", NamedTextColor.GREEN)
                .append(ComponentUtils.parse(winningReward.displayName()))
                .append(Component.text("!", NamedTextColor.GREEN)));

        // Pequeño delay antes de entregar/cerrar para que el jugador
        // alcance a ver el resultado quieto en el slot central.
        Bukkit.getScheduler().runTaskLater(plugin, () -> actionExecutor.grant(player, winningReward),
                REWARD_GRANT_DELAY_TICKS);

        Bukkit.getScheduler().runTaskLater(plugin, this::close, AUTO_CLOSE_DELAY_TICKS);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // Es una animación, no una selección: ningún click hace nada.
        event.setCancelled(true);
    }

    /**
     * Cancela la tarea de animación en curso. No se llama automáticamente
     * al cerrar la GUI (GUIListener de :core no expone un hook de cierre
     * propio) — el resultado ya está decidido de antemano, así que dejar
     * que la animación termine en segundo plano es seguro. Expuesto por
     * si el integrador quiere engancharlo a su propio manejo de
     * InventoryCloseEvent.
     */
    public void cancelAnimation() {
        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }
    }

}
