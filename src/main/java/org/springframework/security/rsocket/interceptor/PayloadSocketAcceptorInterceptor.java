package org.springframework.security.rsocket.interceptor;

import io.rsocket.SocketAcceptor;
import io.rsocket.plugins.SocketAcceptorInterceptor;

import java.util.List;

/**
 * @author Rob Winch
 */
public class PayloadSocketAcceptorInterceptor implements SocketAcceptorInterceptor {

	private final List<PayloadInterceptor> interceptors;

	public PayloadSocketAcceptorInterceptor(List<PayloadInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	@Override
	public SocketAcceptor apply(SocketAcceptor socketAcceptor) {
		return new PayloadSocketAcceptor(socketAcceptor, this.interceptors);
	}
}
