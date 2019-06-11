package security;

import io.rsocket.Payload;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.CompositeMetadataFlyweight;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import reactor.core.publisher.Mono;
import rsocket.interceptor.PayloadInterceptor;
import rsocket.metadata.BasicAuthenticationUtils;

import java.util.Base64;

/**
 * @author Rob Winch
 */
public class AuthenticationPayloadInterceptor implements PayloadInterceptor {

	private final ReactiveAuthenticationManager authenticationManager;

	private Converter<Payload, Authentication> authenticationConverter = payload -> {
		// FIXME: If error we should just return null
		String encoded = BasicAuthenticationUtils.readBasic(payload).orElse(null);
		if (encoded == null) {
			return null;
		}
		byte[] decoded = Base64.getDecoder().decode(encoded);
		String token = new String(decoded);
		int delim = token.indexOf(":");

		if (delim == -1) {
			throw new BadCredentialsException("Invalid basic authentication token");
		}
		String username = token.substring(0, delim);
		String password = token.substring(delim + 1);
		return new UsernamePasswordAuthenticationToken(username, password);
	};

	public AuthenticationPayloadInterceptor(ReactiveAuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	public Mono<Payload> intercept(Payload payload) {
		System.out.println("intercept!");
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
