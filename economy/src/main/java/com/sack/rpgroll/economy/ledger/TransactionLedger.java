package com.sack.rpgroll.economy.ledger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Libro mayor de transacciones: cada movimiento económico (depósito, cobro
 * de impuesto, venta de mercado, pago de salario, préstamo...) queda
 * registrado acá — es lo que hace posible auditar, detectar abuso o
 * simplemente mostrarle a un jugador su propio historial.
 * <p>
 * Para no reescribir el archivo del día en disco en cada transacción
 * (potencialmente cientos por minuto en un server activo), los registros
 * nuevos se acumulan primero en memoria ({@code recentBuffer} para consulta
 * inmediata, {@code pendingFlush} para persistencia) y se vuelcan a
 * {@code ledger/<yyyy-MM-dd>.yml} recién en el próximo {@link #flush()},
 * llamado periódicamente por una tarea y una vez más en {@code onDisable}.
 */
public class TransactionLedger {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int RECENT_BUFFER_CAP = 5000;

    private final File ledgerFolder;
    private final Deque<TransactionRecord> recentBuffer = new ConcurrentLinkedDeque<>();
    private final List<TransactionRecord> pendingFlush = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());

    public TransactionLedger(JavaPlugin economyPlugin) {
        this.ledgerFolder = new File(economyPlugin.getDataFolder(), "ledger");
        this.ledgerFolder.mkdirs();
    }

    public TransactionRecord record(UUID actorId, TransactionType type, String currencyId, double amount,
            double balanceAfter, String description) {

        TransactionRecord entry = new TransactionRecord(idCounter.incrementAndGet(), System.currentTimeMillis(),
                actorId, type, currencyId, amount, balanceAfter, description);

        recentBuffer.addFirst(entry);
        while (recentBuffer.size() > RECENT_BUFFER_CAP) {
            recentBuffer.removeLast();
        }

        synchronized (pendingFlush) {
            pendingFlush.add(entry);
        }

        return entry;
    }

    /** Vuelca a disco cualquier registro acumulado desde el último flush. Seguro de llamar sin nada pendiente. */
    public void flush() {

        List<TransactionRecord> toWrite;

        synchronized (pendingFlush) {
            if (pendingFlush.isEmpty()) {
                return;
            }
            toWrite = new ArrayList<>(pendingFlush);
            pendingFlush.clear();
        }

        File file = new File(ledgerFolder, LocalDate.now().format(DAY_FORMAT) + ".yml");
        YamlConfiguration config = file.isFile() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        List<Map<?, ?>> existing = new ArrayList<>(config.getMapList("entries"));

        for (TransactionRecord entry : toWrite) {
            existing.add(serialize(entry));
        }

        config.set("entries", existing);

        try {
            config.save(file);
        } catch (Exception e) {
            economyLoggerWarn("No se pudo guardar el libro mayor del día: " + e.getMessage());
        }
    }

    public List<TransactionRecord> recent(UUID actorId, int limit) {

        List<TransactionRecord> result = new ArrayList<>();

        for (TransactionRecord entry : recentBuffer) {
            if (actorId == null || actorId.equals(entry.actorId())) {
                result.add(entry);
                if (result.size() >= limit) {
                    break;
                }
            }
        }

        return Collections.unmodifiableList(result);
    }

    public List<TransactionRecord> recentAll(int limit) {
        return recent(null, limit);
    }

    private Map<String, Object> serialize(TransactionRecord entry) {

        Map<String, Object> map = new HashMap<>();
        map.put("id", entry.id());
        map.put("timestamp", entry.timestampMillis());
        map.put("actor", entry.actorId() == null ? null : entry.actorId().toString());
        map.put("type", entry.type().name());
        map.put("currency", entry.currencyId());
        map.put("amount", entry.amount());
        map.put("balance-after", entry.balanceAfter());
        map.put("description", entry.description());
        return map;
    }

    private void economyLoggerWarn(String message) {
        java.util.logging.Logger.getLogger("RPGRoll-Economy").warning("✘ " + message);
    }

}
