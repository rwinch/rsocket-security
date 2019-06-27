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

package rsocket;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.RSocketProxy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import rsocket.interceptor.PayloadChain;
import rsocket.interceptor.PayloadInterceptor;
import rsocket.interceptor.PayloadInterceptorRSocketTests;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactorContextTests {
	private static final String USERNAME_KEY = "username";

	@Mock
	private RSocket delegate;

	@Mock
	private Payload payload;

	@Test
	public void simpleSuccess() {
		String r = Mono.just("hi")
				.subscriberContext(Context.of(USERNAME_KEY, "rob"))
				.flatMap(m -> Mono.subscriberContext()
						.map(context -> m + " " + context.getOrDefault(USERNAME_KEY, "null"))
				)
				.block();
		assertThat(r).isEqualTo("hi rob");
	}

	@Test
	public void simpleFail() {
		String r = Mono.just("hi")
				.subscriberContext(Context.of(USERNAME_KEY, "rob"))
				.flatMap(m -> Mono.subscriberContext()
						.map(context -> m + " " + context.getOrDefault(USERNAME_KEY, "null"))
				)
				.block();
		assertThat(r).isEqualTo("hi rob");
	}

	PayloadInterceptor authenticate = new PayloadInterceptor() {
		@Override
		public Mono<Payload> intercept(Payload payload, PayloadChain chain) {
			return chain.next(payload)
					.subscriberContext(Context.of(USERNAME_KEY, "rob"));
		}
	};

	@Test
	public void interceptorFail() {
		String r = this.authenticate.intercept(this.payload, payload -> Mono.just(payload))
				.flatMap(m -> Mono.subscriberContext()
						.map(context -> "hi " + context.getOrDefault(USERNAME_KEY, "null"))
				)
				.block();
		assertThat(r).isEqualTo("hi rob");
	}

	public class Authorize implements PayloadInterceptor {
		private String username;

		@Override
		public Mono<Payload> intercept(Payload payload, PayloadChain chain) {
			return Mono.subscriberContext()
					.doOnNext(context -> this.username = context.getOrDefault(USERNAME_KEY, "null"))
					.then(chain.next(payload));
		}
	};

	@Test
	public void interceptorSuccess() {
		Authorize authz = new Authorize();
		String r = this.authenticate.intercept(this.payload, payload -> authz.intercept(payload, p -> Mono.just(p)))
				.flatMap(m -> Mono.subscriberContext()
						.map(context -> m + " " + context.getOrDefault(USERNAME_KEY, "null"))
				)
				.block();
		assertThat(authz.username).isEqualTo("rob");
	}

	public class Controller extends RSocketProxy {
		private String username;

		public Controller(RSocket source) {
			super(source);
		}

		@Override
		public Mono<Payload> requestResponse(Payload payload) {
			return Mono.subscriberContext()
					.doOnNext(context -> this.username = context.getOrDefault(USERNAME_KEY, "null"))
					.thenReturn(payload);
		}
	}

	@Test
	public void interceptorWithControllerWhenRequestResponse() {
		Controller controller = new Controller(this.delegate);
		String r = this.authenticate.intercept(this.payload, payload -> controller.requestResponse(payload))
				.flatMap(m -> Mono.subscriberContext()
						.map(context -> m + " " + context.getOrDefault(USERNAME_KEY, "null"))
				)
				.block();
		assertThat(controller.username).isEqualTo("rob");
	}

//	@Test
//	public void interceptorWithControllerWhenRequestStream() {
//		Controller controller = new Controller(this.delegate);
//		String r = this.authenticate.intercept(this.payload, payload -> controller.requestStream(payload))
//				.flatMap(m -> Mono.subscriberContext()
//						.map(context -> m + " " + context.getOrDefault(USERNAME_KEY, "null"))
//				)
//				.block();
//		assertThat(controller.username).isEqualTo("rob");
//	}
}
