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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import rsocket.interceptor.PayloadChain;

/**
 * @author Rob Winch
 */
class AuthenticationPayloadChain implements PayloadChain {
	private Authentication authentication;

	@Override
	public Mono<Void> next(Payload payload) {
		return ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
				.doOnNext(a -> this.setAuthentication(a)).then();
	}

	public Authentication getAuthentication() {
		return this.authentication;
	}

	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}
}
