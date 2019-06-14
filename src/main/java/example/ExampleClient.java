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

package example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.metadata.CompositeMetadataFlyweight;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Mono;
import rsocket.metadata.SecurityMetadataFlyweight;
import rsocket.metadata.SecurityMetadataFlyweight.UsernamePassword;

/**
 * @author Rob Winch
 */
// simon has a branch to implement https://github.com/rsocket/rsocket/blob/master/Extensions/CompositeMetadata.md
// RouteMatcher does matching within Spring
//		// https://github.com/rsocket/rsocket/blob/master/Extensions/Routing.md
public class ExampleClient {
	public static void main(String[] args) {
		RSocket client =
				RSocketFactory.connect()
						.frameDecoder(PayloadDecoder.ZERO_COPY)
						.transport(TcpClientTransport.create(7878))
						.start()
						.block();

		HelloClient helloClient = new HelloClient(client);

		helloClient.sayHiTo("Rob")
			.then(helloClient.sayHiTo("Rossen"))
			.block();

		helloClient.sayHiTo("Joe")
			.then(helloClient.sayHiTo("Josh"))
			.block();
	}

	private static class HelloClient {
		private final RSocket rSocket;

		private HelloClient(RSocket rSocket) {
			this.rSocket = rSocket;
		}

		public Mono<Void> sayHiTo(String name) {

			ByteBuf route = ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "hello");
			CompositeByteBuf metadata = ByteBufAllocator.DEFAULT.compositeBuffer();
			CompositeMetadataFlyweight.encodeAndAddMetadata(metadata, ByteBufAllocator.DEFAULT, WellKnownMimeType.MESSAGE_RSOCKET_ROUTING, route);

			UsernamePassword credentials = new UsernamePassword("rob", "password");
			SecurityMetadataFlyweight.writeBasic(metadata, credentials);
			ByteBuf data = ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, name);
			return this.rSocket.requestResponse(ByteBufPayload.create(data, metadata))
					.doOnNext(p -> System.out.println(p.getDataUtf8()))
					.then();
		}
	}
}

