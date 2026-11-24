package com.sack.rpgroll.structures.command;

import com.sack.rpgroll.structures.workbench.SerialSigner;
import com.sack.rpgroll.structures.workbench.WorkbenchIssuer;
import com.sack.rpgroll.structures.workbench.WorkbenchStamp;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static com.sack.rpgroll.structures.workbench.WorkbenchTier.*;
import static org.junit.jupiter.api.Assertions.*;

class GiveArgsTest {

    @Test void parsesGive() {
        assertEquals(new GiveArgs("Sack", INGENIERO, 3),
                GiveArgs.parse(new String[]{"Sack", "ingeniero", "3"}).orElseThrow());
    }

    @Test void amountDefaultsToOne() {
        assertEquals(1, GiveArgs.parse(new String[]{"Sack", "constructor"}).orElseThrow().amount());
    }

    @Test void amountIsClampedTo64AndAtLeast1() {
        assertEquals(64, GiveArgs.parse(new String[]{"Sack", "arquitecto", "500"}).orElseThrow().amount());
        assertEquals(1, GiveArgs.parse(new String[]{"Sack", "arquitecto", "0"}).orElseThrow().amount());
        assertEquals(1, GiveArgs.parse(new String[]{"Sack", "arquitecto", "-4"}).orElseThrow().amount());
    }

    @Test void unknownTierIsAnError() {
        assertEquals("admin.give.bad_tier", GiveArgs.parse(new String[]{"Sack", "maestro"}).error());
    }

    @Test void badAmountAndMissingArgsAreErrors() {
        assertEquals("admin.give.bad_amount", GiveArgs.parse(new String[]{"Sack", "ingeniero", "x"}).error());
        assertEquals("admin.give.usage", GiveArgs.parse(new String[]{"Sack"}).error());
        assertThrows(IllegalStateException.class, () -> GiveArgs.parse(new String[]{}).orElseThrow());
    }

    @Test void giveCreatesDistinctBenches() {
        SerialSigner signer = new SerialSigner(new byte[32]);
        AtomicLong next = new AtomicLong(1);
        WorkbenchIssuer issuer = new WorkbenchIssuer(() -> CompletableFuture.completedFuture(next.getAndIncrement()),
                signer, Runnable::run, Logger.getAnonymousLogger());
        List<WorkbenchStamp> stamps = issuer.issue(INGENIERO, 3).join();
        assertEquals(3, stamps.size());
        assertEquals(3, stamps.stream().map(WorkbenchStamp::id).distinct().count());
        assertEquals(3, stamps.stream().map(WorkbenchStamp::serial).distinct().count());
        for (WorkbenchStamp s : stamps) {
            assertEquals(INGENIERO, s.tier());
            assertTrue(signer.verify(s.id(), s.tier(), s.serial(), s.signature()));
        }
    }
}
