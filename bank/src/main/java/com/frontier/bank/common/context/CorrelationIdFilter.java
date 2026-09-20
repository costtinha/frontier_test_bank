package com.frontier.bank.common.context;

import java.io.IOException;
import java.util.UUID;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Propaga o id de correlação da requisição HTTP para o {@link CorrelationContext},
 * devolvendo-o no cabeçalho de resposta. Roda antes dos demais filtros
 * (inclusive segurança) para que até respostas de erro sejam rastreáveis.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Correlation-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String incoming = request.getHeader(HEADER);
		String correlationId = (incoming == null || incoming.isBlank())
				? UUID.randomUUID().toString()
				: incoming;

		CorrelationContext.set(correlationId);
		response.setHeader(HEADER, correlationId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			CorrelationContext.clear();
		}
	}

}
