package rsocket.interceptor;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.core.publisher.Mono;
import reactor.test.publisher.PublisherProbe;
import reactor.test.publisher.TestPublisher;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class PayloadInterceptorRSocketTests {
	@Mock
	RSocket delegate;

	@Mock
	PayloadInterceptor interceptor;

	@Mock
	PayloadInterceptor interceptor2;

	@Mock
	Payload payload;

	@Mock
	Payload payload2;

	@Mock
	Payload payload3;

	// single interceptor

	@Test
	public void fireAndForgetWhenInterceptorCompletesThenDelegateInvoked() {
		when(this.interceptor.intercept(any())).thenReturn(Mono.just(this.payload));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor));

		interceptor.fireAndForget(this.payload).block();

		verify(this.interceptor).intercept(this.payload);
		verify(this.delegate).fireAndForget(this.payload);
	}

	@Test
	public void fireAndForgetWhenInterceptorErrorsThenDelegateNotInvoked() {
		RuntimeException expected = new RuntimeException("Oops");
		when(this.interceptor.intercept(any())).thenReturn(Mono.error(expected));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor));

		assertThatCode(() -> interceptor.fireAndForget(this.payload).block()).isEqualTo(expected);

		verify(this.interceptor).intercept(this.payload);
		verifyZeroInteractions(this.delegate);
	}

	// multiple interceptors

	@Test
	public void fireAndForgetWhenInterceptorsCompleteThenDelegateInvoked() {
		when(this.interceptor.intercept(any())).thenReturn(Mono.just(this.payload));
		when(this.interceptor2.intercept(any())).thenReturn(Mono.just(this.payload2));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor, this.interceptor2));

		interceptor.fireAndForget(this.payload).block();

		verify(this.interceptor).intercept(this.payload);
		verify(this.delegate).fireAndForget(this.payload2);
	}

	@Test
	public void fireAndForgetWhenInterceptorsMutatesPayloadThenDelegateInvoked() {
		when(this.interceptor.intercept(any())).thenReturn(Mono.just(this.payload2));
		when(this.interceptor2.intercept(any())).thenReturn(Mono.just(this.payload3));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor, this.interceptor2));

		interceptor.fireAndForget(this.payload).block();

		verify(this.interceptor).intercept(this.payload);
		verify(this.interceptor2).intercept(this.payload2);
		verify(this.delegate).fireAndForget(this.payload3);
	}

	@Test
	public void fireAndForgetWhenInterceptor1ErrorsThenInterceptor2AndDelegateNotInvoked() {
		RuntimeException expected = new RuntimeException("Oops");
		when(this.interceptor.intercept(any())).thenReturn(Mono.error(expected));
		when(this.interceptor2.intercept(any())).thenReturn(Mono.just(this.payload));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor, this.interceptor2));

		assertThatCode(() -> interceptor.fireAndForget(this.payload).block()).isEqualTo(expected);

		verify(this.interceptor).intercept(this.payload);
		verifyZeroInteractions(this.interceptor2);
		verifyZeroInteractions(this.delegate);
	}

	@Test
	public void fireAndForgetWhenInterceptor2ErrorsThenInterceptor2AndDelegateNotInvoked() {
		RuntimeException expected = new RuntimeException("Oops");
		when(this.interceptor.intercept(any())).thenReturn(Mono.just(this.payload));
		when(this.interceptor2.intercept(any())).thenReturn(Mono.error(expected));
		when(this.delegate.fireAndForget(any())).thenReturn(Mono.empty());

		PayloadInterceptorRSocket interceptor = new PayloadInterceptorRSocket(this.delegate,
				Arrays.asList(this.interceptor, this.interceptor2));

		assertThatCode(() -> interceptor.fireAndForget(this.payload).block()).isEqualTo(expected);

		verify(this.interceptor).intercept(this.payload);
		verify(this.interceptor2).intercept(this.payload);
		verifyZeroInteractions(this.delegate);
	}
}