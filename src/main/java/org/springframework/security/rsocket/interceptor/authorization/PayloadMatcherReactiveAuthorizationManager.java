/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.rsocket.interceptor.authorization;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.security.rsocket.interceptor.PayloadExchange;
import org.springframework.security.rsocket.util.PayloadAuthorizationContext;
import org.springframework.security.rsocket.util.PayloadExchangeMatcher;
import org.springframework.security.rsocket.util.PayloadMatcherEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rob Winch
 */
public class PayloadMatcherReactiveAuthorizationManager implements ReactiveAuthorizationManager<PayloadExchange> {
	private final List<PayloadMatcherEntry<ReactiveAuthorizationManager<PayloadAuthorizationContext>>> mappings;

	private PayloadMatcherReactiveAuthorizationManager(List<PayloadMatcherEntry<ReactiveAuthorizationManager<PayloadAuthorizationContext>>> mappings) {
		this.mappings = mappings;
	}

	@Override
	public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, PayloadExchange exchange) {
		return Flux.fromIterable(this.mappings)
				.concatMap(mapping -> mapping.getMatcher().matches(exchange)
						.filter(PayloadExchangeMatcher.MatchResult::isMatch)
						.map(r -> r.getVariables())
						.flatMap(variables -> mapping.getEntry()
								.check(authentication, new PayloadAuthorizationContext(exchange, variables))
						)
				)
				.next()
				.switchIfEmpty(Mono.fromCallable(() -> new AuthorizationDecision(false)));
	}

	public static PayloadMatcherReactiveAuthorizationManager.Builder builder() {
		return new PayloadMatcherReactiveAuthorizationManager.Builder();
	}

	public static class Builder {
		private final List<PayloadMatcherEntry<ReactiveAuthorizationManager<PayloadAuthorizationContext>>> mappings = new ArrayList<>();

		private Builder() {
		}

		public PayloadMatcherReactiveAuthorizationManager.Builder add(PayloadMatcherEntry<ReactiveAuthorizationManager<PayloadAuthorizationContext>> entry) {
			this.mappings.add(entry);
			return this;
		}

		public PayloadMatcherReactiveAuthorizationManager build() {
			return new PayloadMatcherReactiveAuthorizationManager(this.mappings);
		}
	}
}
