package com.frontier.bank.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o agendamento usado pelo relay do outbox
 * ({@code com.frontier.bank.common.event.OutboxRelay}).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

}
