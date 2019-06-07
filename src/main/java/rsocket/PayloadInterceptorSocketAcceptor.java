package rsocket;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Rob Winch
 */
public class PayloadInterceptorSocketAcceptor implements SocketAcceptor {

	private final SocketAcceptor delegate;

	private final List<PayloadInterceptor> interceptors;

	public PayloadInterceptorSocketAcceptor(SocketAcceptor delegate,
			List<PayloadInterceptor> interceptors) {
		this.delegate = delegate;
		this.interceptors = interceptors;
	}

	@Override
	public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
		return this.delegate.accept(setup, sendingSocket)
			.map(r -> new PayloadInterceptorRSocket(r, this.interceptors));
	}
}
