package org.springframework.security.config.rsocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Rob Winch
 */
@Configuration
class RSocketSecurityConfiguration {
	private ReactiveAuthenticationManager authenticationManager;

	private ReactiveUserDetailsService reactiveUserDetailsService;

	private PasswordEncoder passwordEncoder;

	@Autowired
	void setAuthenticationManager(
			ReactiveAuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	@Autowired
	void setUserDetailsService(ReactiveUserDetailsService userDetailsService) {
		this.reactiveUserDetailsService = userDetailsService;
	}

	@Autowired(required = false)
	void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@Bean
	@Scope("prototype")
	public RSocketSecurity rsocketSecurityBuilder(ApplicationContext context) {
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
