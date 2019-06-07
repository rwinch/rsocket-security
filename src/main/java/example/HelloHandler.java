package example;

import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Mono;

public class HelloHandler implements SocketAcceptor {

	@Override
	public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
		return Mono.just(
				new AbstractRSocket() {
					@Override
					public Mono<Payload> requestResponse(Payload payload) {
						String data = payload.getDataUtf8();
						payload.release();
						System.out.println("Got " + data);
						return Mono.just(ByteBufPayload.create("Hello " + data));
					}
				});
	}
}