/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.rsocket.util;

import org.springframework.security.rsocket.interceptor.PayloadExchange;
import org.springframework.security.rsocket.interceptor.PayloadExchangeType;
import reactor.core.publisher.Mono;

/**
 * @author Rob Winch
 */
public abstract class PayloadExchangeMatchers {

	public static PayloadExchangeMatcher setup() {
		return new PayloadExchangeMatcher() {
			public Mono<MatchResult> matches(PayloadExchange exchange) {
				return PayloadExchangeType.SETUP.equals(exchange.getType()) ?
						MatchResult.match() :
						MatchResult.notMatch();
			}
		};
	}

	public static PayloadExchangeMatcher anyRequest() {
		return new PayloadExchangeMatcher() {
			public Mono<MatchResult> matches(PayloadExchange exchange) {
				return exchange.getType().isRequest() ?
						MatchResult.match() :
						MatchResult.notMatch();
			}
		};
	}

	public static PayloadExchangeMatcher anyExchange() {
		return new PayloadExchangeMatcher() {
			public Mono<MatchResult> matches(PayloadExchange exchange) {
				return MatchResult.match();
			}
		};
	}

	private PayloadExchangeMatchers() {}
}
