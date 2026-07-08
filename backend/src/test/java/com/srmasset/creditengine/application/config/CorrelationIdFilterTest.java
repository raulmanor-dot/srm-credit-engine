package com.srmasset.creditengine.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@Test
	void generatesRequestIdWhenNoneProvidedAndClearsMdcAfterwards() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = (req, res) -> assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNotBlank();

		filter.doFilter(request, response, chain);

		String echoed = response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER);
		assertThat(UUID.fromString(echoed)).isNotNull();
		assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNull();
	}

	@Test
	void reusesIncomingRequestIdHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "abc-123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = (req, res) -> { };

		filter.doFilter(request, response, chain);

		assertThat(response.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).isEqualTo("abc-123");
	}

	@Test
	void clearsMdcEvenWhenDownstreamThrows() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = (req, res) -> {
			throw new IllegalStateException("boom");
		};

		assertThatThrownBy(() -> filter.doFilter(request, response, chain))
				.isInstanceOf(IllegalStateException.class);
		assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY)).isNull();
	}
}
