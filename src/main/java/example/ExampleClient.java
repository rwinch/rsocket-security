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
import rsocket.metadata.BasicAuthenticationUtils;

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

 			BasicAuthenticationUtils.writeBasic(metadata, "rob", "password");
			ByteBuf data = ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, name);
			return this.rSocket.requestResponse(ByteBufPayload.create(data, metadata))
					.doOnNext(p -> System.out.println(p.getDataUtf8()))
					.then();
		}
	}
}

