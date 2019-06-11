package rsocket.interceptor;

import io.rsocket.RSocket;
import io.rsocket.plugins.RSocketInterceptor;

import java.util.List;

/**
 * @author Rob Winch
 */
public class PayloadRSocketInterceptor implements RSocketInterceptor {
	private final List<PayloadInterceptor> interceptors;

	public PayloadRSocketInterceptor(List<PayloadInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	@Override
	public RSocket apply(RSocket rSocket) {
		return new PayloadInterceptorRSocket(rSocket, this.interceptors);
	}
}
