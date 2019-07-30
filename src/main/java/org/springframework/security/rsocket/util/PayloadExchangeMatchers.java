package org.springframework.security.rsocket.util;

import org.springframework.security.rsocket.interceptor.PayloadExchange;
import reactor.core.publisher.Mono;

/**
 * @author Rob Winch
 */
public abstract class PayloadExchangeMatchers {

	public static PayloadExchangeMatcher anyExchange() {
		return new PayloadExchangeMatcher() {
			public Mono<MatchResult> matches(PayloadExchange exchange) {
				return MatchResult.match();
			}
		};
	}

	private PayloadExchangeMatchers() {}
}
