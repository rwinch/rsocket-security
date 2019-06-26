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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;

/**
 * @author Rob Winch
 */
public class PayloadInterceptorRSocket extends RSocketProxy {

	private final List<PayloadInterceptor> interceptors;

	public PayloadInterceptorRSocket(RSocket delegate, List<PayloadInterceptor> interceptors) {
		super(delegate);
		if (delegate == null) {
			throw new IllegalArgumentException("delegate cannot be null");
		}
		if (interceptors == null) {
			throw new IllegalArgumentException("interceptors cannot be null");
		}
		if (interceptors.isEmpty()) {
			throw new IllegalArgumentException("interceptors cannot be empty");
		}
		this.interceptors = interceptors;
	}

	@Override
	public Mono<Void> fireAndForget(Payload payload) {
		return intercept(payload)
			.flatMap(context ->
				this.source.fireAndForget(payload)
					.subscriberContext(context)
			);
	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		return intercept(payload)
			.flatMap(context ->
				this.source.requestResponse(payload)
					.subscriberContext(context)
			);
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		return intercept(payload)
			.flatMapMany(context ->
				this.source.requestStream(payload)
					.subscriberContext(context)
			);
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
		return Flux.from(payloads)
			.flatMap(p -> intercept(p)
				.flatMapMany(context -> this.source
					.requestChannel(payloads)
					.subscriberContext(context)
				)
			);
	}

	@Override
	public Mono<Void> metadataPush(Payload payload) {
		return intercept(payload)
			.flatMap(c -> this.source
					.metadataPush(payload)
					.subscriberContext(c)
			);
	}

	private Mono<Context> intercept(Payload payload) {
		return Mono.defer(() -> {
			ContextPayloadChain chain = new ContextPayloadChain(this.interceptors);
			return chain.next(payload)
				.then(Mono.fromCallable(() -> chain.getContext()))
				.defaultIfEmpty(Context.empty());
		});
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[source=" + this.source + ",interceptors="
				+ this.interceptors + "]";
	}
}
