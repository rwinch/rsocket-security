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

package org.springframework.security.rsocket.metadata;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.junit.Test;
import org.springframework.security.rsocket.metadata.SecurityMetadataFlyweight.UsernamePassword;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Winch
 */
public class SecurityMetadataFlyweightTest {
	@Test
	public void basicAuthentication() {
		UsernamePassword expectedCredentials = new UsernamePassword("rob", "password");
		CompositeByteBuf metadata = ByteBufAllocator.DEFAULT.compositeBuffer();

		SecurityMetadataFlyweight.writeBasic(metadata, expectedCredentials);

		UsernamePassword actualCredentials = SecurityMetadataFlyweight.readBasic(metadata)
				.orElse(null);

		assertThat(actualCredentials).isEqualToComparingFieldByField(expectedCredentials);
	}

}