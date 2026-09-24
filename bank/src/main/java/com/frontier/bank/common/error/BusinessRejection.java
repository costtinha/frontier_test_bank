package com.frontier.bank.common.error;

/**
 * Marca uma falha de <b>negócio</b>: repetir não muda o resultado (saldo
 * insuficiente, recurso inexistente). O orquestrador de saga usa este marcador
 * para decidir entre <i>compensar</i> e <i>tentar de novo</i> — tudo que não for
 * rejeição de negócio é tratado como falha transitória (retry).
 */
public interface BusinessRejection {

}
