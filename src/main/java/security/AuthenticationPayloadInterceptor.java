package security;

import io.rsocket.Payload;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import reactor.core.publisher.Mono;
import rsocket.interceptor.PayloadInterceptor;
import rsocket.metadata.SecurityMetadataFlyweight;

/**
 * @author Rob Winch
 */
public class AuthenticationPayloadInterceptor implements PayloadInterceptor {

	private final ReactiveAuthenticationManager authenticationManager;

	private Converter<Payload, Authentication> authenticationConverter = payload ->
		SecurityMetadataFlyweight.readBasic(payload.metadata())
			.map(credentials -> new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword()))
			.orElse(null);

	public AuthenticationPayloadInterceptor(ReactiveAuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public Mono<Payload> intercept(Payload payload) {
		return Mono.defer(() -> {
			Authentication authentication = this.authenticationConverter.convert(payload);
			return Mono.justOrEmpty(authentication)
					.flatMap(a -> this.authenticationManager.authenticate(authentication))
					.flatMap(a -> onAuthenticationSuccess(payload, a));
		});
	}

	private Mono<Payload> onAuthenticationSuccess(Payload payload, Authentication authentication) {
		SecurityContextImpl securityContext = new SecurityContextImpl(authentication);
		return Mono.just(payload)
				.subscriberContext(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
	}

}
