package rsocket.interceptor;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.RSocketProxy;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ListIterator;

/**
 * @author Rob Winch
 */
public class PayloadInterceptorRSocket extends RSocketProxy {

	private final PayloadInterceptor currentInterceptor;

	private final PayloadInterceptorRSocket next;

	public PayloadInterceptorRSocket(RSocket delegate, List<PayloadInterceptor> interceptors) {
		super(delegate);
		if (interceptors == null) {
			throw new IllegalArgumentException("interceptors cannot be null");
		}
		if (interceptors.isEmpty()) {
			throw new IllegalArgumentException("interceptors cannot be empty");
		}
		PayloadInterceptorRSocket interceptor = init(interceptors, delegate);
		this.currentInterceptor = interceptor.currentInterceptor;
		this.next = interceptor.next;
	}

	private static PayloadInterceptorRSocket init(List<PayloadInterceptor> interceptors, RSocket delegate) {
		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(delegate, null, null);
		ListIterator<? extends PayloadInterceptor> iterator = interceptors.listIterator(interceptors.size());
		while (iterator.hasPrevious()) {
			interceptor = new PayloadInterceptorRSocket(delegate, iterator.previous(), interceptor);
		}
		return interceptor;
	}

	private PayloadInterceptorRSocket(RSocket delegate, PayloadInterceptor currentInterceptor, PayloadInterceptorRSocket next) {
		super(delegate);
		this.currentInterceptor = currentInterceptor;
		this.next = next;
	}

	@Override
	public Mono<Void> fireAndForget(Payload payload) {
		return  intercept(payload)
				.flatMap(p -> this.source.fireAndForget(p));
	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		return intercept(payload)
			.flatMap(p -> this.source.requestResponse(p));
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		return intercept(payload)
			.flatMapMany(p -> this.source.requestStream(p));
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
		return Flux.from(payloads)
			.flatMap(p -> intercept(p))
			.transform(this.source::requestChannel);
	}

	@Override
	public Mono<Void> metadataPush(Payload payload) {
		return intercept(payload)
			.flatMap(p -> this.source.metadataPush(p));
	}

	private Mono<Payload> intercept(Payload payload) {
		return Mono.defer(() ->
			shouldIntercept() ?
					this.currentInterceptor.intercept(payload)
							.flatMap(p -> this.next.intercept(p)) :
					Mono.just(payload)
		);
	}

	private boolean shouldIntercept() {
		return this.currentInterceptor != null && this.next != null;
	}

	@Override
	public String toString() {
		if (this.currentInterceptor != null) {
			return getClass().getSimpleName() + "[currentInterceptor=" + this.currentInterceptor + "]";
		}
		return getClass().getSimpleName() + "[source=" + this.source + "]";
	}
}
