package com.frontier.bank.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@AfterEach
	void clearContext() {
		CorrelationContext.clear();
	}

	@Test
	void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> seenInsideChain = new AtomicReference<>();

		filter.doFilter(request, response, (req, res) -> seenInsideChain.set(CorrelationContext.get()));

		assertThat(seenInsideChain.get()).isNotBlank();
		assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(seenInsideChain.get());
		// contexto limpo ao fim da requisição: não vaza entre requisições/threads
		assertThat(CorrelationContext.get()).isNull();
	}

	@Test
	void shouldReuseIncomingCorrelationId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationIdFilter.HEADER, "corr-abc");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> seenInsideChain = new AtomicReference<>();

		filter.doFilter(request, response, (req, res) -> seenInsideChain.set(CorrelationContext.get()));

		assertThat(seenInsideChain.get()).isEqualTo("corr-abc");
		assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("corr-abc");
	}

}
