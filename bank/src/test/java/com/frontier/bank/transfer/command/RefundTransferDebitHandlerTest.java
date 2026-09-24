package com.frontier.bank.transfer.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.ledger.LedgerEntry;
import com.frontier.bank.ledger.LedgerEntryType;
import com.frontier.bank.ledger.LedgerRepository;
import com.frontier.bank.transfer.event.TransferDebitRefunded;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

/**
 * Compensação da saga: o estorno devolve o valor à origem e <b>não apaga</b> o
 * débito anterior — o razão registra as duas pontas.
 */
@ExtendWith(MockitoExtension.class)
class RefundTransferDebitHandlerTest {

	@Mock
	private BalanceRepository balanceRepository;

	@Mock
	private LedgerRepository ledgerRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventPublisher eventPublisher;

	private RefundTransferDebitHandler handler;

	private UUID sourceUserId;
	private UUID transferId;
	private User user;

	@BeforeEach
	void setUp() {
		handler = new RefundTransferDebitHandler(balanceRepository, ledgerRepository, userRepository,
				eventPublisher);
		sourceUserId = UUID.randomUUID();
		transferId = UUID.randomUUID();
		user = new User("Maria", "maria@example.com", "12345678901", "hash");
		ReflectionTestUtils.setField(user, "id", sourceUserId);
	}

	@Test
	void shouldRefundDebitedAmountBackToSourceAccount() {
		Balance balance = balanceOf("60.00");
		when(userRepository.existsById(sourceUserId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(sourceUserId)).thenReturn(Optional.of(balance));
		when(balanceRepository.saveAndFlush(balance)).thenReturn(balance);

		handler.handle(new RefundTransferDebitCommand(transferId, sourceUserId, new BigDecimal("40.00")));

		assertThat(balance.getAmount()).isEqualByComparingTo("100.00");

		// estorno entra no razão como lançamento novo (nunca remoção do débito)
		ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
		verify(ledgerRepository).save(ledgerCaptor.capture());
		LedgerEntry entry = ledgerCaptor.getValue();
		assertThat(entry.getType()).isEqualTo(LedgerEntryType.TRANSFER_REFUND);
		assertThat(entry.getTransactionId()).isEqualTo(transferId);
		assertThat(entry.getBalanceBefore()).isEqualByComparingTo("60.00");
		assertThat(entry.getBalanceAfter()).isEqualByComparingTo("100.00");

		ArgumentCaptor<DomainEvent<?>> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());
		assertThat(eventCaptor.getValue().eventType()).isEqualTo(TransferDebitRefunded.TYPE);
	}

	private Balance balanceOf(String amount) {
		Balance balance = new Balance(user);
		ReflectionTestUtils.setField(balance, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(balance, "amount", new BigDecimal(amount));
		return balance;
	}

}
