package org.springframework.security.config.annotation.rsocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Rob Winch
 */
@Configuration(proxyBeanMethods = false)
class RSocketSecurityConfiguration {

	private static final String BEAN_NAME_PREFIX = "org.springframework.security.config.annotation.rsocket.RSocketSecurityConfiguration.";
	private static final String RSOCKET_SECURITY_BEAN_NAME = BEAN_NAME_PREFIX + "rsocketSecurity";

	private ReactiveAuthenticationManager authenticationManager;

	private ReactiveUserDetailsService reactiveUserDetailsService;

	private PasswordEncoder passwordEncoder;

	@Autowired(required = false)
	void setAuthenticationManager(
			ReactiveAuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	@Autowired(required = false)
	void setUserDetailsService(ReactiveUserDetailsService userDetailsService) {
		this.reactiveUserDetailsService = userDetailsService;
	}

	@Autowired(required = false)
	void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@Bean(name = RSOCKET_SECURITY_BEAN_NAME)
	@Scope("prototype")
	public RSocketSecurity rsocketSecurity(ApplicationContext context) {
		RSocketSecurity security = new RSocketSecurity()
			.authenticationManager(authenticationManager());
		security.setApplicationContext(context);
		return security;
	}

	private ReactiveAuthenticationManager authenticationManager() {
		if (this.authenticationManager != null) {
			return this.authenticationManager;
		}
		if (this.reactiveUserDetailsService != null) {
			UserDetailsRepositoryReactiveAuthenticationManager manager =
					new UserDetailsRepositoryReactiveAuthenticationManager(this.reactiveUserDetailsService);
			if (this.passwordEncoder != null) {
				manager.setPasswordEncoder(this.passwordEncoder);
			}
			return manager;
		}
		return null;
	}
}
