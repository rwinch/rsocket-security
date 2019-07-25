package org.springframework.security.config.rsocket;

import org.springframework.context.annotation.Import;

/**
 * @author Rob Winch
 */
@Import(RSocketSecurity.class)
public @interface EnableRSocketSecurity { }
