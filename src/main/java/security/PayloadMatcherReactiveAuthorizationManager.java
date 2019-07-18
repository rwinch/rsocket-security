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

package security;

import io.rsocket.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rsocket.util.PayloadAuthorizationContext;
import rsocket.util.PayloadMatcher;
import rsocket.util.PayloadMatcherEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rob Winch
 */
public class PayloadMatcherReactiveAuthorizationManager implements ReactiveAuthorizationManager<Payload> {
	private final List<PayloadMatcherEntry<ReactiveAuthorizationManager<PayloadAuthorizationContext>>> mappings;

	private PayloadMatcherReactiveAuthorizationManager(List<PayloadMatcherEntry<ReactiveAuthorizationManager<PayloadAuthorizationContext>>> mappings) {
		this.mappings = mappings;
	}

	@Override
	public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, Payload payload) {
		return Flux.fromIterable(this.mappings)
				.concatMap(mapping -> mapping.getMatcher().matches(payload)
						.filter(PayloadMatcher.MatchResult::isMatch)
						.map(r -> r.getVariables())
						.flatMap(variables -> mapping.getEntry()
								.check(authentication, new PayloadAuthorizationContext(payload, variables))
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
