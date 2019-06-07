package example;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Mono;

import java.util.Base64;

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
			String credentials = Base64.getEncoder().encodeToString("rob2:password".getBytes());
			return this.rSocket.requestResponse(ByteBufPayload.create(name, credentials))
					.doOnNext(p -> System.out.println(p.getDataUtf8()))
					.then();
		}
	}
}
