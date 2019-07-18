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

package org.springframework.security.rsocket.authentication;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.rsocket.interceptor.authentication.AuthenticationPayloadInterceptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;
import org.springframework.security.rsocket.interceptor.DefaultPayloadExchange;
import org.springframework.security.rsocket.interceptor.PayloadInterceptorChain;
import org.springframework.security.rsocket.interceptor.PayloadExchange;
import org.springframework.security.rsocket.metadata.SecurityMetadataFlyweight;
import org.springframework.security.rsocket.metadata.SecurityMetadataFlyweight.UsernamePassword;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationPayloadInterceptorTests {
	@Mock
	ReactiveAuthenticationManager authenticationManager;

	@Captor
	ArgumentCaptor<Authentication> authenticationArg;

	@Test
	public void constructorWhenAuthenticationManagerNullThenException() {
		assertThatCode(() -> new AuthenticationPayloadInterceptor(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void interceptWhenBasicCredentialsThenAuthenticates() {
		AuthenticationPayloadInterceptor interceptor = new AuthenticationPayloadInterceptor(
				this.authenticationManager);
		PayloadExchange exchange = createExchange();
		TestingAuthenticationToken expectedAuthentication =
				new TestingAuthenticationToken("user","password");
		when(this.authenticationManager.authenticate(any())).thenReturn(Mono.just(
				expectedAuthentication));

		AuthenticationPayloadInterceptorChain authenticationPayloadChain = new AuthenticationPayloadInterceptorChain();
		interceptor.intercept(exchange, authenticationPayloadChain)
			.block();

		Authentication authentication = authenticationPayloadChain.getAuthentication();

		verify(this.authenticationManager).authenticate(this.authenticationArg.capture());
		assertThat(this.authenticationArg.getValue()).isEqualToComparingFieldByField(new UsernamePasswordAuthenticationToken("user", "password"));
		assertThat(authentication).isEqualTo(expectedAuthentication);
	}

	@Test
	public void interceptWhenAuthenticationSuccessThenChainSubscribedOnce() {
		AuthenticationPayloadInterceptor interceptor = new AuthenticationPayloadInterceptor(
				this.authenticationManager);

		PayloadExchange exchange = createExchange();
		TestingAuthenticationToken expectedAuthentication =
				new TestingAuthenticationToken("user","password");
		when(this.authenticationManager.authenticate(any())).thenReturn(Mono.just(
				expectedAuthentication));

		PublisherProbe<Void> voidResult = PublisherProbe.empty();
		PayloadInterceptorChain chain = mock(PayloadInterceptorChain.class);
		when(chain.next(any())).thenReturn(voidResult.mono());


		StepVerifier.create(interceptor.intercept(exchange, chain))
			.then(() -> assertThat(voidResult.subscribeCount()).isEqualTo(1))
			.verifyComplete();
	}

	private Payload createRequestPayload() {
		CompositeByteBuf metadata = ByteBufAllocator.DEFAULT.compositeBuffer();
		UsernamePassword credentials = new UsernamePassword("user", "password");
		SecurityMetadataFlyweight.writeBasic(metadata, credentials);
		return DefaultPayload.create(ByteBufAllocator.DEFAULT.buffer(), metadata);
	}

	private PayloadExchange createExchange() {
		return new DefaultPayloadExchange(createRequestPayload(), null, null);
	}

}