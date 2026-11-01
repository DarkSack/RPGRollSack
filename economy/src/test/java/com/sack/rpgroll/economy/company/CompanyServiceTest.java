package com.sack.rpgroll.economy.company;

import com.sack.rpgroll.economy.bank.*;
import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.wallet.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyServiceTest {
    @Test
    void treasuryThatCannotBePaidIsPreservedAndRetriedWithoutDoublePayment() {
        BankManager banks = mock(BankManager.class);
        WalletService wallet = mock(WalletService.class);
        CompanyManager companies = mock(CompanyManager.class);
        CompanyService service = new CompanyService(companies, banks, wallet, mock(TransactionLedger.class));
        UUID owner = UUID.randomUUID();
        BankAccount treasury = new BankAccount(UUID.randomUUID(), BankAccountType.COMPANY, owner, "Treasury");
        treasury.setBalance("gold", 100);
        Company company = new Company(UUID.randomUUID(), "Company", owner, treasury.id());
        when(banks.get(treasury.id())).thenReturn(Optional.of(treasury));
        when(wallet.deposit(eq(owner), eq("gold"), eq(100.0), any(), anyString()))
                .thenReturn(EconomyResult.LIMIT_EXCEEDED, EconomyResult.SUCCESS);
        assertEquals(EconomyResult.LIMIT_EXCEEDED, service.disband(company));
        assertEquals(100, treasury.balance("gold"));
        verify(banks, never()).delete(any());
        verify(companies, never()).delete(any());
        assertEquals(EconomyResult.SUCCESS, service.disband(company));
        assertEquals(0, treasury.balance("gold"));
        verify(banks).save(treasury);
        verify(banks).delete(treasury.id());
        verify(companies).delete(company.id());
    }
}
