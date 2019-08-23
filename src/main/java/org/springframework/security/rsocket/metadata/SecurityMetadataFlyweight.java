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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.CompositeMetadataFlyweight;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * FIXME: Consider adding integration with MetadataExtractor
 * @author Rob Winch
 */
public class SecurityMetadataFlyweight {
	public static final String BASIC_AUTHENTICATION_MIME_TYPE = "message/x.rsocket.authentication.basic.v0";

	public static void writeBasic(CompositeByteBuf compositeMetaData, UsernamePassword usernamePassword) {
		String username = usernamePassword.getUsername();
		String password = usernamePassword.getPassword();
		ByteBuf metadata = Unpooled.buffer();
		byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
		metadata.writeInt(usernameBytes.length);
		metadata.writeBytes(usernameBytes);
		metadata.writeBytes(password.getBytes(StandardCharsets.UTF_8));
		CompositeMetadataFlyweight.encodeAndAddMetadata(compositeMetaData, ByteBufAllocator.DEFAULT, BASIC_AUTHENTICATION_MIME_TYPE, metadata);
	}

	public static class UsernamePassword {
		private final String username;
		private final String password;

		public UsernamePassword(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return this.username;
		}

		public String getPassword() {
			return this.password;
		}
	}

	public static Optional<UsernamePassword> readBasic(ByteBuf metadata) {
		return findByType(metadata, BASIC_AUTHENTICATION_MIME_TYPE)
			.map(b -> {
				int usernameSize = b.readInt();
				ByteBuf usernameBuf = b.readBytes(usernameSize);
				ByteBuf passwordBuf = b.readBytes(b.readableBytes());
				String username = usernameBuf.toString(StandardCharsets.UTF_8);
				String password = passwordBuf.toString(StandardCharsets.UTF_8);
				return new UsernamePassword(username, password);
			});
	}

	public static Optional<ByteBuf> findByType(ByteBuf metadata, String type) {
		CompositeMetadata compositeMetadata = new CompositeMetadata(metadata,
				false);
		return StreamSupport.stream(compositeMetadata.spliterator(), false)
				.filter(entry -> type.equals(entry.getMimeType()))
				.map(CompositeMetadata.Entry::getContent)
				.findFirst();
	}
}
