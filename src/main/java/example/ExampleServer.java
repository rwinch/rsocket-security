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

package example;

import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import rsocket.interceptor.PayloadInterceptor;
import rsocket.interceptor.PayloadRSocketInterceptor;
import security.AuthenticationPayloadInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * @author Rob Winch
 */
public class ExampleServer {
	public static void main(String[] args) {
		UserDetails rob = User.withDefaultPasswordEncoder()
			.username("rob")
			.password("password")
			.roles("USER")
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
				.asList(new AuthenticationPayloadInterceptor(manager));
		RSocketFactory.receive()
				// Enable Zero Copy
				.frameDecoder(PayloadDecoder.ZERO_COPY)
				.addResponderPlugin(new PayloadRSocketInterceptor(payloadInterceptors))
				.acceptor(helloHandler)
				.transport(TcpServerTransport.create(7878))
				.start()
				.block()
				.onClose()
				.block();
	}
}
