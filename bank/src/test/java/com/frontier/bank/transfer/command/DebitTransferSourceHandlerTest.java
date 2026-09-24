package com.frontier.bank.transfer.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.frontier.bank.common.error.TransferRejectedException;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.ledger.LedgerEntry;
import com.frontier.bank.ledger.LedgerEntryType;
import com.frontier.bank.ledger.LedgerRepository;
import com.frontier.bank.transfer.event.TransferDebited;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class DebitTransferSourceHandlerTest {

	@Mock
	private BalanceRepository balanceRepository;

	@Mock
	private LedgerRepository ledgerRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EventPublisher eventPublisher;

	private DebitTransferSourceHandler handler;

	private UUID sourceUserId;
	private UUID transferId;
	private User user;

	@BeforeEach
	void setUp() {
		handler = new DebitTransferSourceHandler(balanceRepository, ledgerRepository, userRepository,
				eventPublisher);
		sourceUserId = UUID.randomUUID();
		transferId = UUID.randomUUID();
		user = new User("Maria", "maria@example.com", "12345678901", "hash");
		ReflectionTestUtils.setField(user, "id", sourceUserId);
	}

	@Test
	void shouldDebitSourceAccountRecordingLedgerAndEvent() {
		Balance balance = balanceOf("100.00");
		when(userRepository.existsById(sourceUserId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(sourceUserId)).thenReturn(Optional.of(balance));
		when(balanceRepository.saveAndFlush(balance)).thenReturn(balance);

		UUID result = handler.handle(new DebitTransferSourceCommand(transferId, sourceUserId,
				new BigDecimal("40.00")));

		assertThat(result).isEqualTo(balance.getId());
		assertThat(balance.getAmount()).isEqualByComparingTo("60.00");

		// razão: uma entrada de débito da transferência, com o mesmo transactionId da operação
		ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
		verify(ledgerRepository).save(ledgerCaptor.capture());
		LedgerEntry entry = ledgerCaptor.getValue();
		assertThat(entry.getType()).isEqualTo(LedgerEntryType.TRANSFER_DEBIT);
		assertThat(entry.getTransactionId()).isEqualTo(transferId);
		assertThat(entry.getUserId()).isEqualTo(sourceUserId);
		assertThat(entry.getBalanceBefore()).isEqualByComparingTo("100.00");
		assertThat(entry.getBalanceAfter()).isEqualByComparingTo("60.00");

		ArgumentCaptor<DomainEvent<?>> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());
		assertThat(eventCaptor.getValue().eventType()).isEqualTo(TransferDebited.TYPE);
		assertThat(eventCaptor.getValue().aggregateId()).isEqualTo(balance.getId());
	}

	@Test
	void shouldRejectDebitWhenBalanceIsInsufficient() {
		Balance balance = balanceOf("10.00");
		when(userRepository.existsById(sourceUserId)).thenReturn(true);
		when(balanceRepository.findByUserIdForUpdate(sourceUserId)).thenReturn(Optional.of(balance));

		assertThatThrownBy(() -> handler.handle(new DebitTransferSourceCommand(transferId, sourceUserId,
				new BigDecimal("40.00"))))
				.isInstanceOf(TransferRejectedException.class)
				.hasMessageContaining("Saldo insuficiente");

		// nenhum efeito: sem razão, sem evento e saldo intacto
		assertThat(balance.getAmount()).isEqualByComparingTo("10.00");
		verify(ledgerRepository, never()).save(any());
		verify(eventPublisher, never()).publish(any());
	}

	private Balance balanceOf(String amount) {
		Balance balance = new Balance(user);
		ReflectionTestUtils.setField(balance, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(balance, "amount", new BigDecimal(amount));
		return balance;
	}

}
