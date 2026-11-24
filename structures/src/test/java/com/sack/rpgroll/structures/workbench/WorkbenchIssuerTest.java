package com.sack.rpgroll.structures.workbench;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class WorkbenchIssuerTest {
    final SerialSigner signer = new SerialSigner(new byte[32]);
    final List<CompletableFuture<Long>> requested = new ArrayList<>();
    final WorkbenchIssuer issuer = new WorkbenchIssuer(() -> {
        CompletableFuture<Long> f = new CompletableFuture<>();
        requested.add(f);
        return f;
    }, signer, Runnable::run, Logger.getAnonymousLogger());

    @Test void poolHoldsSixtyFourSerials() {
        // Un /dar de 64 seguidos no debe agotarlo.
        assertEquals(64, WorkbenchIssuer.POOL_TARGET);
    }

    @Test void emptyPoolReturnsEmptyWithoutBlocking() {
        assertEquals(Optional.empty(), issuer.issueNow(WorkbenchTier.CONSTRUCTOR));
        assertEquals(WorkbenchIssuer.POOL_TARGET, requested.size());
    }

    @Test void filledPoolIssuesSignedDistinctBenchesAndRefills() {
        issuer.refill();
        for (int i = 0; i < requested.size(); i++) {
            requested.get(i).complete(100L + i);
        }
        int before = requested.size();
        WorkbenchStamp a = issuer.issueNow(WorkbenchTier.CONSTRUCTOR).orElseThrow();
        WorkbenchStamp b = issuer.issueNow(WorkbenchTier.CONSTRUCTOR).orElseThrow();
        assertNotEquals(a.id(), b.id());
        assertNotEquals(a.serial(), b.serial());
        assertTrue(signer.verify(a.id(), a.tier(), a.serial(), a.signature()));
        assertEquals(before + 2, requested.size(), "cada serie gastada pide otra");
    }

    @Test void failedReservationIsNotPooled() {
        issuer.refill();
        requested.forEach(f -> f.completeExceptionally(new RuntimeException("db")));
        assertEquals(Optional.empty(), issuer.issueNow(WorkbenchTier.CONSTRUCTOR));
    }

    @Test void issueFailureCompletesExceptionally() {
        CompletableFuture<List<WorkbenchStamp>> r = issuer.issue(WorkbenchTier.CONSTRUCTOR, 2);
        requested.get(0).complete(1L);
        requested.get(1).completeExceptionally(new RuntimeException("db"));
        assertTrue(r.isCompletedExceptionally());
    }

    @Test void sampleSignatureIsNeverGenuine() {
        assertFalse(signer.verify(new UUID(0, 0), WorkbenchTier.CONSTRUCTOR, 0, "muestra"));
    }
}
