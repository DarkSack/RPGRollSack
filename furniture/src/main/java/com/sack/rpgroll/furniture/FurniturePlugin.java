package com.sack.rpgroll.furniture;

import com.sack.rpgroll.common.command.BrigadierCommands;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.furniture.command.FurnitureAdminCommand;
import com.sack.rpgroll.furniture.command.FurnitureCommand;
import com.sack.rpgroll.furniture.core.FurnitureManager;
import com.sack.rpgroll.furniture.item.FurnitureItems;
import com.sack.rpgroll.furniture.listener.AmbientTask;
import com.sack.rpgroll.furniture.listener.FurnitureListener;
import com.sack.rpgroll.furniture.listener.LifecycleListener;
import com.sack.rpgroll.furniture.placed.FurnitureIndex;
import com.sack.rpgroll.furniture.placed.FurnitureInteractions;
import com.sack.rpgroll.furniture.placed.FurnitureService;
import com.sack.rpgroll.furniture.seat.SeatService;
import com.sack.rpgroll.furniture.storage.StorageService;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

/**
 * RPGRoll-Furniture: muebles y decoración colocables.
 * <p>
 * Cada mueble es un ItemDisplay con un modelo del resource pack, más barreras o una Interaction
 * para chocar y hacer clic. Puede tener asientos, almacén, luz, estados, estante, estación de
 * trabajo, partículas y versiones por madera o color. Se fabrican en un carpintero (un mueble con
 * {@code workstation: CARPENTER}, o /furnitureadmin carpenter desde un NPC), se venden en la
 * tienda o se dan como recompensa con /furnitureadmin give.
 * <p>
 * En Bedrock se ven con la extensión GeyserDisplayEntity y los mapeos que trae
 * {@code bedrock/} (ver su README).
 */
public class FurniturePlugin extends JavaPlugin {

    private LangManager lang;
    private FurnitureSettings settings;
    private FurnitureManager manager;
    private FurnitureItems items;
    private FurnitureIndex index;
    private SeatService seats;
    private StorageService storage;

    @Override
    public void onEnable() {

        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        new DirectoryCreator(this).create(List.of("furniture", "bedrock"));
        new ResourceCopier(this).copyDirectories(List.of("furniture", "bedrock"));

        lang = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        settings = FurnitureSettings.from(getConfig());
        lang.reload(getConfig().getString("language", "es"));

        FurnitureKeys keys = new FurnitureKeys(this);
        manager = new FurnitureManager();
        loadFurniture();

        items = new FurnitureItems(keys, manager, lang);
        // Para el recetario (RPGRoll-Recipes): lo que fabrica cada carpintero.
        com.sack.rpgroll.common.recipe.RecipeSource.register(this,
                new com.sack.rpgroll.furniture.item.FurnitureRecipeSource(manager, items, lang));
        index = new FurnitureIndex();
        FurnitureService service = new FurnitureService(keys, manager, items, index, () -> settings);
        seats = new SeatService(keys, () -> settings.seatOffset());
        storage = new StorageService();
        service.onBeforeRemove(seats::ejectAll);
        service.onBeforeRemove(storage::flush);

        FurnitureInteractions interactions = new FurnitureInteractions(service, seats, storage, lang);
        LifecycleListener lifecycle = new LifecycleListener(this, keys, index, seats, storage);

        var plugins = getServer().getPluginManager();
        plugins.registerEvents(new FurnitureListener(service, interactions, lang), this);
        plugins.registerEvents(lifecycle, this);
        lifecycle.indexLoaded();

        getServer().getScheduler().runTaskTimer(this, new AmbientTask(index, manager, () -> settings.ambientRange()),
                AmbientTask.PERIOD, AmbientTask.PERIOD);

        registerPack();

        FurnitureCommand player = new FurnitureCommand(manager, items, lang);
        BrigadierCommands.register(this, "muebles", "Catálogo de muebles", List.of("furniture", "decoracion"),
                player, player, "rpgroll.furniture.catalog");
        FurnitureAdminCommand admin = new FurnitureAdminCommand(manager, items, service, lang, this::reload);
        BrigadierCommands.register(this, "furnitureadmin", "Administra RPGRoll-Furniture", List.of("fadmin"),
                admin, admin, "rpgroll.furniture.admin");

        getLogger().info("✔ RPGRoll-Furniture habilitado: " + manager.count() + " mueble(s) en "
                + manager.categories().size() + " categoría(s), " + index.size() + " colocado(s) ya cargado(s).");
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.flushAll();
        }
        if (seats != null) {
            seats.clearAll();
        }
        if (index != null) {
            index.clear();
        }
    }

    /**
     * El ítem de un mueble para otros plugins (la tienda de Economy, recompensas).
     *
     * @param ref id del mueble, o id:versión; una versión que no existe da la primera
     */
    public java.util.Optional<org.bukkit.inventory.ItemStack> createItem(String ref, int amount) {

        if (manager == null || items == null || ref == null) {
            return java.util.Optional.empty();
        }
        String[] parts = ref.split(":", 2);
        return manager.get(parts[0]).map(def -> items.create(def, parts.length > 1 ? parts[1] : null, amount));
    }

    private void loadFurniture() {
        manager.load(new File(getDataFolder(), "furniture"), getConfig().getConfigurationSection("categories"),
                message -> getLogger().warning(message));
        manager.checkItems(message -> getLogger().warning(message));
    }

    private void reload() {
        reloadConfig();
        settings = FurnitureSettings.from(getConfig());
        lang.reload(getConfig().getString("language", "es"));
        loadFurniture();
    }

    /**
     * El pack de Java de los muebles de fábrica viaja dentro del jar ({@code resourcepack/}) y se
     * registra en SackResourcePack si está. Sin él, el dueño lo sirve por su cuenta: el mismo
     * contenido está en el zip del producto.
     */
    private void registerPack() {

        if (!getServer().getPluginManager().isPluginEnabled("SackResourcePack")
                || !getConfig().getBoolean("resource-pack.register-in-sackresourcepack", true)) {
            return;
        }
        try {
            if (com.sack.rpgroll.sackresourcepack.api.AssetsAPI.isReady()
                    && com.sack.rpgroll.sackresourcepack.api.AssetsAPI.assets().registerPlugin(this)) {
                getLogger().info("✔ Modelos de los muebles registrados en SackResourcePack (aplica con /srp rebuild).");
            }
        } catch (LinkageError e) {
            getLogger().warning("✘ No se pudo registrar el pack en SackResourcePack: " + e.getMessage());
        }
    }
}
