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
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import rsocket.interceptor.PayloadChain;
import rsocket.interceptor.PayloadInterceptor;
import rsocket.metadata.SecurityMetadataFlyweight;

/**
 * @author Rob Winch
 */
public class AuthenticationPayloadInterceptor implements PayloadInterceptor {

	private final ReactiveAuthenticationManager authenticationManager;

	private Converter<Payload, Authentication> authenticationConverter = payload ->
		SecurityMetadataFlyweight.readBasic(payload.metadata())
			.map(credentials -> new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword()))
			.orElse(null);

	public AuthenticationPayloadInterceptor(ReactiveAuthenticationManager authenticationManager) {
		Assert.notNull(authenticationManager, "authenticationManager cannot be null");
		this.authenticationManager = authenticationManager;
	}

	public Mono<Void> intercept(Payload payload, PayloadChain chain) {
		return Mono.defer(() -> {
			Authentication authentication = this.authenticationConverter.convert(payload);
			return Mono.justOrEmpty(authentication)
					.switchIfEmpty(chain.next(payload).then(Mono.empty()))
					.flatMap(a -> this.authenticationManager.authenticate(authentication))
					.flatMap(a -> onAuthenticationSuccess(chain.next(payload), a));
		});
	}

	private Mono<Void> onAuthenticationSuccess(Mono<Void> payload, Authentication authentication) {
		return payload
				.subscriberContext(ReactiveSecurityContextHolder.withAuthentication(authentication));
	}

}
