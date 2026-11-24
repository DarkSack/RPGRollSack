package com.sack.rpgroll.common.quick;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuickCommandsTest {

    private static final Predicate<String> ONLINE = name -> name.equalsIgnoreCase("Sack");

    @Test
    void thePlayerGoesFirstLikeVanillaGive() {
        var a = QuickCommands.GiveArgs.parse(new String[] { "Sack", "item:medallon", "5" }, ONLINE);
        assertEquals("Sack", a.target());
        assertEquals("item:medallon", a.thing());
        assertEquals(5, a.amount());
    }

    @Test
    void withoutAPlayerItIsForYou() {
        var a = QuickCommands.GiveArgs.parse(new String[] { "medallon", "3" }, ONLINE);
        assertNull(a.target());
        assertEquals("medallon", a.thing());
        assertEquals(3, a.amount());
        assertEquals(1, QuickCommands.GiveArgs.parse(new String[] { "medallon" }, ONLINE).amount());
        // Una sola palabra es siempre la cosa, aunque coincida con un jugador.
        assertEquals("Sack", QuickCommands.GiveArgs.parse(new String[] { "Sack" }, ONLINE).thing());
    }

    @Test
    void amountsAreKeptSane() {
        assertEquals(-1, QuickCommands.amount("muchos", 64));
        assertEquals(-1, QuickCommands.amount("0", 64));
        assertEquals(64, QuickCommands.amount("9999", 64));
        assertEquals(7, QuickCommands.amount("7", 64));
    }

    /** Un ítem de mentira que recuerda su cantidad; {@code similar} decide si apila con otro. */
    private static ItemStack stack(int maxStack, boolean similar) {
        ItemStack s = mock(ItemStack.class);
        AtomicInteger amount = new AtomicInteger(1);
        when(s.getAmount()).thenAnswer(i -> amount.get());
        doAnswer(i -> {
            amount.set(i.getArgument(0));
            return null;
        }).when(s).setAmount(anyInt());
        when(s.getMaxStackSize()).thenReturn(maxStack);
        when(s.isSimilar(any())).thenReturn(similar);
        return s;
    }

    @Test
    void uniqueItemsAreCreatedOncePerUnitAndNeverCloned() {
        // Como las mesas: no apilan y cada una lleva su id; el creador se queda sin existencias a la tercera.
        List<ItemStack> made = new ArrayList<>();
        List<ItemStack> given = new ArrayList<>();
        ItemStack first = stack(1, false);
        made.add(first);
        int issued = QuickCommands.issue(first, 5, () -> {
            if (made.size() >= 3) {
                return Optional.empty();
            }
            ItemStack s = stack(1, false);
            made.add(s);
            return Optional.of(s);
        }, given::add);

        assertEquals(3, issued);
        assertEquals(made, given);
        for (ItemStack s : given) {
            assertEquals(1, s.getAmount());
            verify(s, never()).clone();
        }
    }

    @Test
    void stackableItemsStillGoInFullStacks() {
        List<ItemStack> given = new ArrayList<>();
        int issued = QuickCommands.issue(stack(64, true), 70, () -> Optional.of(stack(64, true)), given::add);

        assertEquals(70, issued);
        assertEquals(2, given.size());
        assertEquals(64, given.get(0).getAmount());
        assertEquals(6, given.get(1).getAmount());
    }

    @Test
    void oneUnitIsTheFirstItem() {
        List<ItemStack> given = new ArrayList<>();
        ItemStack first = stack(64, true);
        assertEquals(1, QuickCommands.issue(first, 1, () -> {
            throw new AssertionError("no se pide otra pieza para una sola");
        }, given::add));
        assertEquals(List.of(first), given);
    }
}
