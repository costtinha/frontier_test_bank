package com.frontier.bank.common.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class CommandBusTest {

	record Ping(String payload) {
	}

	record Sum(int a, int b) {
	}

	@Test
	void shouldRouteCommandToRegisteredHandler() {
		CommandHandler<Ping, String> pingHandler = new CommandHandler<>() {
			@Override
			public String handle(Ping command) {
				return "pong:" + command.payload();
			}
		};
		CommandHandler<Sum, Integer> sumHandler = new CommandHandler<>() {
			@Override
			public Integer handle(Sum command) {
				return command.a() + command.b();
			}
		};
		CommandBus bus = new CommandBus(List.of(pingHandler, sumHandler));

		String pong = bus.dispatch(new Ping("x"));
		assertThat(pong).isEqualTo("pong:x");
		Integer sum = bus.dispatch(new Sum(2, 3));
		assertThat(sum).isEqualTo(5);
	}

	@Test
	void shouldRejectCommandWithoutHandler() {
		CommandBus bus = new CommandBus(List.of());

		assertThatThrownBy(() -> bus.dispatch(new Ping("x")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Ping");
	}

	@Test
	void shouldRejectDuplicateHandlersForSameCommand() {
		CommandHandler<Ping, String> first = new CommandHandler<>() {
			@Override
			public String handle(Ping command) {
				return "1";
			}
		};
		CommandHandler<Ping, String> second = new CommandHandler<>() {
			@Override
			public String handle(Ping command) {
				return "2";
			}
		};

		assertThatThrownBy(() -> new CommandBus(List.of(first, second)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Ping");
	}

}
