/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.rsocket.itests;

import io.rsocket.RSocketFactory;
import io.rsocket.exceptions.ApplicationErrorException;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.rsocket.interceptor.PayloadSocketAcceptorInterceptor;
import org.springframework.security.rsocket.metadata.BasicAuthenticationEncoder;
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Rob Winch
 */
@ContextConfiguration
@RunWith(SpringRunner.class)
public class RSocketMessageHandlerITests {
	@Autowired
	RSocketMessageHandler handler;

	@Autowired
	PayloadSocketAcceptorInterceptor interceptor;

	@Autowired
	ServerController controller;

	private CloseableChannel server;

	private RSocketRequester requester;

	@Before
	public void setup() {
		this.server = RSocketFactory.receive()
				.frameDecoder(PayloadDecoder.ZERO_COPY)
				.addSocketAcceptorPlugin(this.interceptor)
				.acceptor(this.handler.responder())
				.transport(TcpServerTransport.create("localhost", 7000))
				.start()
				.block();

		this.requester = RSocketRequester.builder()
				//				.rsocketFactory(factory -> factory.addRequesterPlugin(payloadInterceptor))
				.rsocketStrategies(this.handler.getRSocketStrategies())
				.connectTcp("localhost", 7000)
				.block();
	}

	@After
	public void dispose() {
		this.requester.rsocket().dispose();
		this.server.dispose();
		this.controller.payloads.clear();
	}

	@Test
	public void retrieveMonoWhenSecureThenDenied() throws Exception {
		String data = "rob";
		assertThatCode(() -> this.requester.route("secure.retrieve-mono")
				.data(data)
				.retrieveMono(String.class)
				.block()
			).isInstanceOf(ApplicationErrorException.class);
		assertThat(this.controller.payloads).isEmpty();
	}

	@Test
	public void retrieveMonoWhenAuthenticationFailedThenException() throws Exception {
		String data = "rob";
		UsernamePasswordMetadata credentials = new UsernamePasswordMetadata("invalid", "password");
		assertThatCode(() -> this.requester.route("secure.retrieve-mono")
				.metadata(credentials, UsernamePasswordMetadata.BASIC_AUTHENTICATION_MIME_TYPE)
				.data(data)
				.retrieveMono(String.class)
				.block()
		).isInstanceOf(ApplicationErrorException.class);
		assertThat(this.controller.payloads).isEmpty();
	}

	@Test
	public void retrieveMonoWhenAuthorizedThenGranted() throws Exception {
		String data = "rob";
		UsernamePasswordMetadata credentials = new UsernamePasswordMetadata("rob", "password");
		String hiRob = this.requester.route("secure.retrieve-mono")
				.metadata(credentials, UsernamePasswordMetadata.BASIC_AUTHENTICATION_MIME_TYPE)
				.data(data)
				.retrieveMono(String.class)
				.block();

		assertThat(hiRob).isEqualTo("Hi rob");
		assertThat(this.controller.payloads).containsOnly(data);
	}

	@Test
	public void retrieveMonoWhenPublicThenGranted() throws Exception {
		String data = "rob";
		String hiRob = this.requester.route("retrieve-mono")
				.data(data)
				.retrieveMono(String.class)
				.block();

		assertThat(hiRob).isEqualTo("Hi rob");
		assertThat(this.controller.payloads).containsOnly(data);
	}

	@Test
	public void retrieveFluxWhenDataFluxAndSecureThenDenied() throws Exception {
		Flux<String> data = Flux.just("a", "b", "c");
		assertThatCode(() -> this.requester.route("secure.secure.retrieve-flux")
				.data(data, String.class)
				.retrieveFlux(String.class)
				.collectList()
				.block()).isInstanceOf(
				ApplicationErrorException.class);

		assertThat(this.controller.payloads).isEmpty();
	}

	@Test
	public void retrieveFluxWhenDataFluxAndPublicThenGranted() throws Exception {
		Flux<String> data = Flux.just("a", "b", "c");
		List<String> hi = this.requester.route("retrieve-flux")
				.data(data, String.class)
				.retrieveFlux(String.class)
				.collectList()
				.block();

		assertThat(hi).containsOnly("hello a", "hello b", "hello c");
		assertThat(this.controller.payloads).containsOnlyElementsOf(data.collectList().block());
	}

	@Test
	public void retrieveFluxWhenDataStringAndSecureThenDenied() throws Exception {
		String data = "a";
		assertThatCode(() -> this.requester.route("secure.hello")
				.data(data)
				.retrieveFlux(String.class)
				.collectList()
				.block()).isInstanceOf(
				ApplicationErrorException.class);

		assertThat(this.controller.payloads).isEmpty();
	}

	@Test
	public void retrieveFluxWhenDataStringAndPublicThenGranted() throws Exception {
		String data = "a";
		List<String> hi = this.requester.route("retrieve-flux")
				.data(data)
				.retrieveFlux(String.class)
				.collectList()
				.block();

		assertThat(hi).contains("hello a");
		assertThat(this.controller.payloads).containsOnly(data);
	}

	@Test
	public void sendWhenSecureThenDenied() throws Exception {
		String data = "hi";
		this.requester.route("secure.send")
				.data(data)
				.send()
				.block();

		assertThat(this.controller.payloads).isEmpty();
	}

	@Test
	public void sendWhenPublicThenGranted() throws Exception {
		String data = "hi";
		this.requester.route("send")
				.data(data)
				.send()
				.block();
		assertThat(this.controller.awaitPayloads()).containsOnly("hi");
	}

	@Configuration
	@EnableRSocketSecurity
	static class Config {

		@Bean
		public ServerController controller() {
			return new ServerController();
		}

		@Bean
		public RSocketMessageHandler messageHandler() {
			RSocketMessageHandler handler = new RSocketMessageHandler();
			handler.setRSocketStrategies(rsocketStrategies());
			return handler;
		}

		@Bean
		public RSocketStrategies rsocketStrategies() {
			return RSocketStrategies.builder()
					.encoder(new BasicAuthenticationEncoder())
					.build();
		}

		@Bean
		MapReactiveUserDetailsService uds() {
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
			return new MapReactiveUserDetailsService(rob, rossen);
		}

		@Bean
		PayloadSocketAcceptorInterceptor rsocketInterceptor(RSocketSecurity rsocket) {
			rsocket
					.authorizePayload(authorize -> {
						authorize
								.route("secure.*").authenticated()
								.anyRequest().permitAll();
					})
					.basicAuthentication(Customizer.withDefaults());
			return rsocket.build();
		}
	}

	@Controller
	static class ServerController {
		private List<String> payloads = new ArrayList<>();

		@MessageMapping({"secure.retrieve-mono", "retrieve-mono"})
		String retrieveMono(String payload) {
			add(payload);
			return "Hi " + payload;
		}

		@MessageMapping({"secure.retrieve-flux", "retrieve-flux"})
		Flux<String> retrieveFlux(Flux<String> payload) {
			return payload.doOnNext(this::add)
					.map(p -> "hello " + p);
		}

		@MessageMapping({"secure.send", "send"})
		Mono<Void> send(Flux<String> payload) {
			return payload
					.doOnNext(this::add)
					.then(Mono.fromRunnable(() -> {
						doNotifyAll();
					}));
		}

		private synchronized void doNotifyAll() {
			this.notifyAll();
		}

		private synchronized List<String> awaitPayloads() throws InterruptedException {
			this.wait();
			return this.payloads;
		}

		private void add(String p) {
			this.payloads.add(p);
		}
	}

}
