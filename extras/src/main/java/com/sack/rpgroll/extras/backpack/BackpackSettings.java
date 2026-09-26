package com.sack.rpgroll.extras.backpack;

import org.bukkit.Material;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Todo lo que se lee de backpacks.yml. */
public record BackpackSettings(
        boolean enabled,
        boolean openInAir,
        boolean openFromInventory,
        boolean allowPlace,
        boolean placedPlacerOnly,
        boolean discoverRecipes,
        Set<Material> forbidden,
        String title,
        String ownerLoreUnbound,
        String ownerLoreBound,
        Sounds sounds,
        Gui gui,
        List<BackpackTier> tiers) {

    public record Sounds(String open, String close, String page, String bind) {
    }

    /**
     * La fila de botones: {@code infoSlot}, {@code tabSlots} y {@code bindSlot}
     * son columnas 0-8 de esa fila; -1 quita el botón.
     */
    public record Gui(
            BackpackButton filler,
            BackpackButton locked,
            int infoSlot,
            BackpackButton info,
            List<Integer> tabSlots,
            BackpackButton tab,
            BackpackButton tabSelected,
            int bindSlot,
            BackpackButton bindUnbound,
            BackpackButton bindBound) {
    }

    public static final BackpackSettings DISABLED = new BackpackSettings(false, false, false, false, true, false,
            Set.of(), "", "", "", new Sounds("", "", "", ""),
            new Gui(null, null, -1, null, List.of(), null, null, -1, null, null), List.of());

    public Optional<BackpackTier> tier(String id) {
        return tiers.stream().filter(t -> t.id().equalsIgnoreCase(id)).findFirst();
    }

    public Optional<BackpackTier> previous(BackpackTier tier) {
        return tier.index() > 0 ? Optional.of(tiers.get(tier.index() - 1)) : Optional.empty();
    }

}
