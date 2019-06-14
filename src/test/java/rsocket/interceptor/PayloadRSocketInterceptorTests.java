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

import io.rsocket.RSocket;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class PayloadRSocketInterceptorTests {
	@Mock
	RSocket delegate;

	@Mock
	PayloadInterceptor interceptor;

	@Test
	public void constructorWhenNullInterceptorsThenException() {
		assertThatCode(() -> new PayloadRSocketInterceptor(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void constructorWhenEmptyInterceptorsThenException() {
		assertThatCode(() -> new PayloadRSocketInterceptor(Arrays.asList()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void applyThenReturnsPayloadInterceptorRSocket() {
		List<PayloadInterceptor> interceptors = Arrays.asList(this.interceptor);
		PayloadRSocketInterceptor rsocket =
				new PayloadRSocketInterceptor(interceptors);

		RSocket result = rsocket.apply(this.delegate);

		assertThat(result).isInstanceOf(PayloadInterceptorRSocket.class);
	}
}