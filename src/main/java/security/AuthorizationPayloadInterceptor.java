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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import rsocket.interceptor.PayloadChain;
import rsocket.interceptor.PayloadInterceptor;

/**
 * @author Rob Winch
 */
public class AuthorizationPayloadInterceptor implements PayloadInterceptor {
	private final ReactiveAuthorizationManager<Payload> authorizationManager;

	public AuthorizationPayloadInterceptor(
			ReactiveAuthorizationManager<Payload> authorizationManager) {
		this.authorizationManager = authorizationManager;
	}

	@Override
	public Mono<Void> intercept(Payload payload, PayloadChain chain) {
		return ReactiveSecurityContextHolder.getContext()
				.filter(c -> c.getAuthentication() != null)
				.map(SecurityContext::getAuthentication)
				.switchIfEmpty(Mono.error(() -> new AuthenticationCredentialsNotFoundException("An Authentication (possibly AnonymousAuthenticationToken) is required.")))
				.as(authentication -> this.authorizationManager.verify(authentication, payload))
				.then(chain.next(payload));
	}
}
