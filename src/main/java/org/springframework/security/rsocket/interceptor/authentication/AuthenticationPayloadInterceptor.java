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

package org.springframework.security.rsocket.interceptor.authentication;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import org.springframework.security.rsocket.interceptor.PayloadInterceptorChain;
import org.springframework.security.rsocket.interceptor.PayloadExchange;
import org.springframework.security.rsocket.interceptor.PayloadInterceptor;
import org.springframework.security.rsocket.metadata.SecurityMetadataFlyweight;

/**
 * Uses the provided {@code ReactiveAuthenticationManager} to authenticate a Payload. If
 * authentication is successful, then the result is added to
 * {@link ReactiveSecurityContextHolder}.
 *
 * @author Rob Winch
 * @since 5.2
 */
public class AuthenticationPayloadInterceptor implements PayloadInterceptor {

	private final ReactiveAuthenticationManager authenticationManager;

	private Converter<PayloadExchange, Authentication> authenticationConverter = exchange ->
		SecurityMetadataFlyweight.readBasic(exchange.getPayload().metadata())
			.map(credentials -> new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword()))
			.orElse(null);

	/**
	 * Creates a new instance
	 * @param authenticationManager the manager to use. Cannot be null
	 */
	public AuthenticationPayloadInterceptor(ReactiveAuthenticationManager authenticationManager) {
		Assert.notNull(authenticationManager, "authenticationManager cannot be null");
		this.authenticationManager = authenticationManager;
	}

	public Mono<Void> intercept(PayloadExchange exchange, PayloadInterceptorChain chain) {
		return Mono.defer(() -> {
			Authentication authentication = this.authenticationConverter.convert(exchange);
			return Mono.justOrEmpty(authentication)
					.switchIfEmpty(chain.next(exchange).then(Mono.empty()))
					.flatMap(a -> this.authenticationManager.authenticate(authentication))
					.flatMap(a -> onAuthenticationSuccess(chain.next(exchange), a));
		});
	}

	private Mono<Void> onAuthenticationSuccess(Mono<Void> payload, Authentication authentication) {
		return payload
				.subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication));
	}

}
