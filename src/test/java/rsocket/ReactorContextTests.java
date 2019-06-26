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

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.Optional;

/**
 * @author Rob Winch
 */
public class ReactorContextTests {
	@Test
	public void helloWorld() {
		String key = "message";
		Mono<String> r = Mono.just("Hello")
				.flatMap( s -> Mono.subscriberContext()
						.map( ctx -> s + " " + ctx.get(key)))
				.subscriberContext(ctx -> ctx.put(key, "World"));

		StepVerifier.create(r)
				.expectNext("Hello World")
				.verifyComplete();
	}

	String key = "username";

	@Test
	public void security() {
		Mono<String> r = Mono.just("Hello")
				.flatMap( s -> filter(s))
				.flatMap(s -> Mono.subscriberContext()
						.flatMap(ctx -> Mono.justOrEmpty((String) ctx.get(key))
						)
				)
				.subscriberContext(Context.of(key, "rob"));;

		StepVerifier.create(r)
				.expectNext("rob")
				.verifyComplete();
	}

	private Mono<String> filter(String s) {
		return Mono.just(s)
			.subscriberContext(Context.of(key, "rob"));
	}
}
