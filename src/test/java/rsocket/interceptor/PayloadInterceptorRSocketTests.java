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

package rsocket.interceptor;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.RSocketProxy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class PayloadInterceptorRSocketTests {
	@Mock
	RSocket delegate;

	@Mock
	PayloadInterceptor interceptor;

	@Mock
	PayloadInterceptor interceptor2;

	@Mock
	Payload payload;

	@Mock
	Payload payload2;

	@Mock
	Payload payload3;

	@Test
	public void constructorWhenNullDelegateThenException() {
		this.delegate = null;
		List<PayloadInterceptor> interceptors = Arrays.asList(this.interceptor);
		assertThatCode(() -> {
			new PayloadInterceptorRSocket(this.delegate, interceptors);
		})
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenNullInterceptorsThenException() {
		List<PayloadInterceptor> interceptors = null;
		assertThatCode(() -> new PayloadInterceptorRSocket(this.delegate, interceptors))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenEmptyInterceptorsThenException() {
		List<PayloadInterceptor> interceptors = Collections.emptyList();
		assertThatCode(() -> new PayloadInterceptorRSocket(this.delegate, interceptors))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// single interceptor

	@Test
	public void fireAndForgetWhenInterceptorCompletesThenDelegateInvoked() {
		when(this.interceptor.intercept(any(), any())).thenAnswer(withPayload(this.payload));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor));

		interceptor.fireAndForget(this.payload).block();

		verify(this.interceptor).intercept(eq(this.payload), any());
		verify(this.delegate).fireAndForget(this.payload);
	}

	@Test
	public void fireAndForgetWhenInterceptorErrorsThenDelegateNotInvoked() {
		RuntimeException expected = new RuntimeException("Oops");
		when(this.interceptor.intercept(any(), any())).thenReturn(Mono.error(expected));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor));

		assertThatCode(() -> interceptor.fireAndForget(this.payload).block()).isEqualTo(expected);

		verify(this.interceptor).intercept(eq(this.payload), any());
		verifyZeroInteractions(this.delegate);
	}

	@Test
	public void fireAndForgetWhenSecurityContextThenDelegateContext() {
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password");
		when(this.interceptor.intercept(any(), any())).thenAnswer(invocation -> {
			PayloadChain c = (PayloadChain) invocation.getArguments()[1];
			return c.next(this.payload);
		});
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());
		RSocketProxy assertAuthentication = new RSocketProxy(this.delegate) {
			@Override
			public Mono<Void> fireAndForget(Payload payload) {
				return ReactiveSecurityContextHolder.getContext()
						.map(SecurityContext::getAuthentication)
						.doOnNext(a -> assertThat(a).isEqualTo(authentication))
						.flatMap(a -> super.fireAndForget(payload));
			}
		};

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(assertAuthentication,
				Arrays.asList(this.interceptor));

		interceptor.fireAndForget(this.payload).block();

		verify(this.interceptor).intercept(eq(this.payload), any());
		verify(this.delegate).fireAndForget(this.payload);
	}

//	@Test
//	public void requestResponseWhenInterceptorCompletesThenDelegateInvoked() {
//		when(this.interceptor.intercept(any())).thenReturn(Mono.just(this.payload));
//		when(this.delegate.requestResponse(any())).thenReturn(Mono.just(this.payload));
//
//		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
//				Arrays.asList(this.interceptor));
//
//		assertThat(interceptor.requestResponse(this.payload).block()).isEqualTo(this.payload);
//
//		verify(this.interceptor).intercept(this.payload);
//		verify(this.delegate).requestResponse(this.payload);
//	}
//
//	@Test
//	public void requestResponseWhenInterceptorErrorsThenDelegateNotInvoked() {
//		RuntimeException expected = new RuntimeException("Oops");
//		when(this.interceptor.intercept(any())).thenReturn(Mono.error(expected));
//		when(this.delegate.requestResponse(any())).thenReturn(Mono.empty());
//
//		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
//				Arrays.asList(this.interceptor));
//
//		assertThatCode(() -> interceptor.requestResponse(this.payload).block()).isEqualTo(expected);
//
//		verify(this.interceptor).intercept(this.payload);
//		verifyZeroInteractions(this.delegate);
//	}
//
//	@Test
//	public void requestStreamWhenInterceptorCompletesThenDelegateInvoked() {
//		when(this.interceptor.intercept(any())).thenReturn(Mono.just(this.payload));
//		when(this.delegate.requestStream(any())).thenReturn(Flux.just(this.payload));
//
//		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
//				Arrays.asList(this.interceptor));
//
//		assertThat(interceptor.requestStream(this.payload).collectList().block()).containsOnly(this.payload);
//
//		verify(this.interceptor).intercept(this.payload);
//		verify(this.delegate).requestStream(this.payload);
//	}
//
//	@Test
//	public void requestStreamWhenInterceptorErrorsThenDelegateNotInvoked() {
//		RuntimeException expected = new RuntimeException("Oops");
//		when(this.interceptor.intercept(any())).thenReturn(Mono.error(expected));
//		when(this.delegate.requestStream(any())).thenReturn(Flux.empty());
//
//		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
//				Arrays.asList(this.interceptor));
//
//		assertThatCode(() -> interceptor.requestStream(this.payload).collectList().block()).isEqualTo(expected);
//
//		verify(this.interceptor).intercept(this.payload);
//		verifyZeroInteractions(this.delegate);
//	}
//
//	@Test
//	public void requestChannelWhenInterceptorCompletesThenDelegateInvoked() {
//		when(this.interceptor.intercept(any())).thenReturn(Mono.just(this.payload));
//		when(this.delegate.requestChannel(any())).thenAnswer(a -> a.getArguments()[0]);
//
//		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
//				Arrays.asList(this.interceptor));
//
//		assertThat(interceptor.requestChannel(Flux.just(this.payload)).collectList().block()).containsOnly(this.payload);
//
//		verify(this.interceptor).intercept(this.payload);
//		verify(this.delegate).requestChannel(any());
//	}
//
//	@Test
//	public void requestChannelWhenInterceptorErrorsThenDelegateNotSubscribed() {
//		TestPublisher<Payload> testPayload = TestPublisher.<Payload>create();
//		Mono<Payload> p = testPayload.mono();
//		RuntimeException expected = new RuntimeException("Oops");
//		when(this.interceptor.intercept(any())).thenReturn(Mono.error(expected));
//		when(this.delegate.requestChannel(any())).thenAnswer(a -> {
//			Flux<Payload> input = (Flux<Payload>) a.getArguments()[0];
//			return input.flatMap(i -> p);
//		});
//
//		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
//				Arrays.asList(this.interceptor));
//
//		assertThatCode(() -> interceptor.requestChannel(Flux.just(this.payload)).collectList().block()).isEqualTo(expected);
//
//		verify(this.interceptor).intercept(this.payload);
//		testPayload.assertNoSubscribers();
//	}
//
//	@Test
//	public void metadataPushWhenInterceptorCompletesThenDelegateInvoked() {
//		when(this.interceptor.intercept(any())).thenReturn(Mono.just(this.payload));
//		when(this.delegate.metadataPush(any())).thenReturn(Mono.empty());
//
//		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
//				Arrays.asList(this.interceptor));
//
//		assertThat(interceptor.metadataPush(this.payload).block());
//
//		verify(this.interceptor).intercept(this.payload);
//		verify(this.delegate).metadataPush(this.payload);
//	}
//
//	@Test
//	public void metadataPushWhenInterceptorErrorsThenDelegateNotInvoked() {
//		RuntimeException expected = new RuntimeException("Oops");
//		when(this.interceptor.intercept(any())).thenReturn(Mono.error(expected));
//		when(this.delegate.metadataPush(any())).thenReturn(Mono.empty());
//
//		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
//				Arrays.asList(this.interceptor));
//
//		assertThatCode(() -> interceptor.metadataPush(this.payload).block())
//			.isEqualTo(expected);
//
//		verify(this.interceptor).intercept(this.payload);
//		verifyZeroInteractions(this.delegate);
//	}
//
//	// multiple interceptors

	@Test
	public void fireAndForgetWhenInterceptorsCompleteThenDelegateInvoked() {
		when(this.interceptor.intercept(any(), any())).thenAnswer(withPayload(this.payload));
		when(this.interceptor2.intercept(any(), any())).thenReturn(Mono.just(this.payload2));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor, this.interceptor2));

		interceptor.fireAndForget(this.payload).block();

		verify(this.interceptor).intercept(eq(this.payload), any());
		verify(this.delegate).fireAndForget(this.payload2);
	}


	@Test
	public void fireAndForgetWhenInterceptorsMutatesPayloadThenDelegateInvoked() {
		when(this.interceptor.intercept(any(), any())).thenAnswer(withPayload(this.payload2));
		when(this.interceptor2.intercept(any(), any())).thenAnswer(withPayload(this.payload3));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor, this.interceptor2));

		interceptor.fireAndForget(this.payload).block();

		verify(this.interceptor).intercept(eq(this.payload), any());
		verify(this.interceptor2).intercept(eq(this.payload2), any());
		verify(this.delegate).fireAndForget(eq(this.payload3));
	}

	@Test
	public void fireAndForgetWhenInterceptor1ErrorsThenInterceptor2AndDelegateNotInvoked() {
		RuntimeException expected = new RuntimeException("Oops");
		when(this.interceptor.intercept(any(), any())).thenReturn(Mono.error(expected));
		when(this.interceptor2.intercept(any(), any())).thenAnswer(withPayload(this.payload));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor, this.interceptor2));

		assertThatCode(() -> interceptor.fireAndForget(this.payload).block()).isEqualTo(expected);

		verify(this.interceptor).intercept(eq(this.payload), any());
		verifyZeroInteractions(this.interceptor2);
		verifyZeroInteractions(this.delegate);
	}

	@Test
	public void fireAndForgetWhenInterceptor2ErrorsThenInterceptor2AndDelegateNotInvoked() {
		RuntimeException expected = new RuntimeException("Oops");
		when(this.interceptor.intercept(any(), any())).thenAnswer(withPayload(this.payload));
		when(this.interceptor2.intercept(any(), any())).thenReturn(Mono.error(expected));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor, this.interceptor2));

		assertThatCode(() -> interceptor.fireAndForget(this.payload).block()).isEqualTo(expected);

		verify(this.interceptor).intercept(eq(this.payload), any());
		verify(this.interceptor2).intercept(eq(this.payload), any());
		verifyZeroInteractions(this.delegate);
	}

	private static Answer<Mono<Payload>> withChainNext() {
		return invocation -> {
			Payload p = (Payload) invocation.getArguments()[0];
			return withPayload(p).answer(invocation);
		};
	}

	private static Answer<Mono<Payload>> withPayload(Payload p) {
		return invocation -> {
			PayloadChain c = (PayloadChain) invocation.getArguments()[1];
			return c.next(p);
		};
	}
}