package org.springframework.security.config.annotation.rsocket;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.rsocket.interceptor.PayloadInterceptor;
import org.springframework.security.rsocket.interceptor.PayloadRSocketInterceptor;
import org.springframework.security.rsocket.interceptor.authentication.AnonymousPayloadInterceptor;
import org.springframework.security.rsocket.interceptor.authentication.AuthenticationPayloadInterceptor;
import org.springframework.security.rsocket.interceptor.authorization.AuthorizationPayloadInterceptor;
import org.springframework.security.rsocket.interceptor.authorization.PayloadMatcherReactiveAuthorizationManager;
import org.springframework.security.rsocket.util.PayloadAuthorizationContext;
import org.springframework.security.rsocket.util.PayloadExchangeMatcher;
import org.springframework.security.rsocket.util.PayloadMatcherEntry;
import org.springframework.security.rsocket.util.RoutePayloadExchangeMatcher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rob Winch
 */
public class RSocketSecurity {

	private ReactiveAuthenticationManager authenticationManager;

	private AuthorizePayloadsSpec authorizePayload;

	private ApplicationContext context;

	public RSocketSecurity authorizePayload(Customizer<AuthorizePayloadsSpec> authorize) {
		if (this.authorizePayload == null) {
			this.authorizePayload = new AuthorizePayloadsSpec();
		}
		try {
			authorize.customize(this.authorizePayload);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public PayloadRSocketInterceptor build() {
		List<PayloadInterceptor> payloadInterceptors = new ArrayList<>();

		payloadInterceptors.add(new AuthenticationPayloadInterceptor(this.authenticationManager));
		payloadInterceptors.add(new AnonymousPayloadInterceptor("anonymousUser"));

		if (this.authorizePayload != null) {
			payloadInterceptors.add(this.authorizePayload.build());
		}
		return new PayloadRSocketInterceptor(payloadInterceptors);
	}

	public RSocketSecurity authenticationManager(ReactiveAuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
		return this;
	}

	public class AuthorizePayloadsSpec {

		private PayloadMatcherReactiveAuthorizationManager.Builder authzBuilder =
				PayloadMatcherReactiveAuthorizationManager.builder();

		public Access anyPayload() {
			return matcher(p -> RoutePayloadExchangeMatcher.MatchResult.match());
		}

		protected AuthorizationPayloadInterceptor build() {
			return new AuthorizationPayloadInterceptor(this.authzBuilder.build());
		}

		public Access route(String pattern) {
			RSocketMessageHandler handler = getBean(RSocketMessageHandler.class);
			PayloadExchangeMatcher matcher = new RoutePayloadExchangeMatcher(
					handler.getMetadataExtractor(),
					handler.getRouteMatcher(),
					pattern);
			return matcher(matcher);
		}

		public Access matcher(PayloadExchangeMatcher matcher) {
			return new Access(matcher);
		}

		public class Access {

			private final PayloadExchangeMatcher matcher;

			private Access(PayloadExchangeMatcher matcher) {
				this.matcher = matcher;
			}

			public AuthorizePayloadsSpec authenticated() {
				return access(AuthenticatedReactiveAuthorizationManager.authenticated());
			}

			public AuthorizePayloadsSpec permitAll() {
				return access((a,ctx) -> Mono
						.just(new AuthorizationDecision(true)));
			}

			public AuthorizePayloadsSpec access(
					ReactiveAuthorizationManager<PayloadAuthorizationContext> authorization) {
				AuthorizePayloadsSpec.this.authzBuilder.add(new PayloadMatcherEntry<>(this.matcher, authorization));
				return AuthorizePayloadsSpec.this;
			}
		}
	}

	private <T> T getBean(Class<T> beanClass) {
		if (this.context == null) {
			return null;
		}
		return this.context.getBean(beanClass);
	}

	private <T> T getBeanOrNull(Class<T> beanClass) {
		return getBeanOrNull(ResolvableType.forClass(beanClass));
	}

	private <T> T getBeanOrNull(ResolvableType type) {
		if (this.context == null) {
			return null;
		}
		String[] names =  this.context.getBeanNamesForType(type);
		if (names.length == 1) {
			return (T) this.context.getBean(names[0]);
		}
		return null;
	}

	protected void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context = applicationContext;
	}
}
