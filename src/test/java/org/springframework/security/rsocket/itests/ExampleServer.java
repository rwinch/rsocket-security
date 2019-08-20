/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.rsocket.itests;

import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.rsocket.interceptor.PayloadInterceptor;
import org.springframework.security.rsocket.interceptor.PayloadSocketAcceptorInterceptor;
import org.springframework.security.rsocket.interceptor.authentication.AuthenticationPayloadInterceptor;
import org.springframework.security.rsocket.interceptor.authorization.AuthorizationPayloadInterceptor;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasRole;

/**
 * @author Rob Winch
 */
public class ExampleServer {
	public static void start() {
		UserDetails rob = User.withDefaultPasswordEncoder()
			.username("rob")
			.password("password")
			.roles("USER", "ADMIN")
			.build();
		UserDetails rossen = User.withDefaultPasswordEncoder()
			.username("rossen")
			.password("password")
			.roles("USER")
			.build();
		MapReactiveUserDetailsService uds = new MapReactiveUserDetailsService(
				rob, rossen);
		ReactiveAuthenticationManager manager = new UserDetailsRepositoryReactiveAuthenticationManager(uds);
		HelloHandler helloHandler = new HelloHandler();
		List<PayloadInterceptor> payloadInterceptors = Arrays
				.asList(new AuthenticationPayloadInterceptor(manager), new AuthorizationPayloadInterceptor(hasRole("ADMIN")));
		RSocketFactory.receive()
				// Enable Zero Copy
				.frameDecoder(PayloadDecoder.ZERO_COPY)
				.addSocketAcceptorPlugin(new PayloadSocketAcceptorInterceptor(payloadInterceptors))
				.acceptor(helloHandler)
				.transport(TcpServerTransport.create(7878))
				.start()
				.block()
				.onClose()
				.block();
	}
}
