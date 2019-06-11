package rsocket.interceptor;

import io.rsocket.Payload;
import reactor.core.publisher.Mono;

/**
 * @author Rob Winch
 */
public interface PayloadInterceptor {
	Mono<Payload> intercept(Payload payload);
}
