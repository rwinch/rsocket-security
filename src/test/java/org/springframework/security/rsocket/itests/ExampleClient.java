package org.springframework.security.rsocket.itests;

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
import org.springframework.security.rsocket.metadata.SecurityMetadataFlyweight;
import reactor.core.publisher.Mono;

/**
 * @author Rob Winch
 */
class HelloClient {
	private final RSocket rSocket;

	HelloClient(RSocket rSocket) {
		this.rSocket = rSocket;
	}

	public static HelloClient create(int port) {
		RSocket sender =
				RSocketFactory.connect()
						.frameDecoder(PayloadDecoder.ZERO_COPY)
						.transport(TcpClientTransport.create(port))
						.start()
						.block();
		return new HelloClient(sender);
	}

	public Mono<Void> sayHiTo(String name) {

		ByteBuf route = ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "hello");
		CompositeByteBuf metadata = ByteBufAllocator.DEFAULT.compositeBuffer();
		CompositeMetadataFlyweight.encodeAndAddMetadata(metadata, ByteBufAllocator.DEFAULT, WellKnownMimeType.MESSAGE_RSOCKET_ROUTING, route);

		SecurityMetadataFlyweight.UsernamePassword credentials = new SecurityMetadataFlyweight.UsernamePassword("rob", "password");
		SecurityMetadataFlyweight.writeBasic(metadata, credentials);
		ByteBuf data = ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, name);
		return this.rSocket.requestResponse(ByteBufPayload.create(data, metadata))
				.doOnNext(p -> System.out.println(p.getDataUtf8()))
				.then();
	}
}
