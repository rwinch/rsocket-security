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

package org.springframework.security.rsocket.interceptor.authorization;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.rsocket.interceptor.PayloadExchange;
import org.springframework.security.rsocket.interceptor.PayloadExchangeType;
import org.springframework.security.rsocket.util.PayloadExchangeAuthorizationContext;
import org.springframework.security.rsocket.util.PayloadExchangeMatcher;
import org.springframework.security.rsocket.util.PayloadExchangeMatcherEntry;
import org.springframework.security.rsocket.util.PayloadExchangeMatchers;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class PayloadExchangeMatcherReactiveAuthorizationManagerTest {

	@Mock
	private ReactiveAuthorizationManager<PayloadExchangeAuthorizationContext> authz;

	@Mock
	private ReactiveAuthorizationManager<PayloadExchangeAuthorizationContext> authz2;

	@Mock
	private PayloadExchange exchange;

	@Before
	public void setup() {
		when(this.exchange.getType()).thenReturn(PayloadExchangeType.REQUEST_RESPONSE);
	}

	@Test
	public void checkWhenGrantedThenGranted() {
		AuthorizationDecision expected = new AuthorizationDecision(true);
		when(this.authz.check(any(), any())).thenReturn(Mono.just(
				expected));
		PayloadExchangeMatcherReactiveAuthorizationManager manager =
				PayloadExchangeMatcherReactiveAuthorizationManager.builder()
						.add(new PayloadExchangeMatcherEntry<>(PayloadExchangeMatchers.anyExchange(), this.authz))
						.build();

		assertThat(manager.check(Mono.empty(), this.exchange).block())
				.isEqualTo(expected);
	}

	@Test
	public void checkWhenDeniedThenDenied() {
		AuthorizationDecision expected = new AuthorizationDecision(false);
		when(this.authz.check(any(), any())).thenReturn(Mono.just(
				expected));
		PayloadExchangeMatcherReactiveAuthorizationManager manager =
				PayloadExchangeMatcherReactiveAuthorizationManager.builder()
						.add(new PayloadExchangeMatcherEntry<>(PayloadExchangeMatchers.anyExchange(), this.authz))
						.build();

		assertThat(manager.check(Mono.empty(), this.exchange).block())
				.isEqualTo(expected);
	}

	@Test
	public void checkWhenFirstMatchThenSecondUsed() {
		AuthorizationDecision expected = new AuthorizationDecision(true);
		when(this.authz.check(any(), any())).thenReturn(Mono.just(
				expected));
		PayloadExchangeMatcherReactiveAuthorizationManager manager =
				PayloadExchangeMatcherReactiveAuthorizationManager.builder()
						.add(new PayloadExchangeMatcherEntry<>(PayloadExchangeMatchers.anyExchange(), this.authz))
						.add(new PayloadExchangeMatcherEntry<>(e -> PayloadExchangeMatcher.MatchResult.notMatch(), this.authz2))
						.build();

		assertThat(manager.check(Mono.empty(), this.exchange).block())
				.isEqualTo(expected);
	}

	@Test
	public void checkWhenSecondMatchThenSecondUsed() {
		AuthorizationDecision expected = new AuthorizationDecision(true);
		when(this.authz2.check(any(), any())).thenReturn(Mono.just(
				expected));
		PayloadExchangeMatcherReactiveAuthorizationManager manager =
				PayloadExchangeMatcherReactiveAuthorizationManager.builder()
						.add(new PayloadExchangeMatcherEntry<>(e -> PayloadExchangeMatcher.MatchResult.notMatch(), this.authz))
						.add(new PayloadExchangeMatcherEntry<>(PayloadExchangeMatchers.anyExchange(), this.authz2))
						.build();

		assertThat(manager.check(Mono.empty(), this.exchange).block())
				.isEqualTo(expected);
	}
}