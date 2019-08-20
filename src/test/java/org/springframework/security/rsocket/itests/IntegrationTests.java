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
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.rsocket.interceptor.PayloadSocketAcceptorInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Rob Winch
 */
@ContextConfiguration
@RunWith(SpringRunner.class)
public class IntegrationTests {
	@Autowired
	RSocketMessageHandler handler;

	@Autowired
	PayloadSocketAcceptorInterceptor interceptor;

	private CloseableChannel server;

	private RSocketRequester requester;

	@Before
	public void setup() {
		this.server = RSocketFactory.receive()
				.frameDecoder(PayloadDecoder.ZERO_COPY)
				.addSocketAcceptorPlugin(this.interceptor)
				.acceptor(this.handler.serverResponder())
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
	}

	@Test
	public void retrieveMono() {
		assertThatCode(() -> this.requester.route("secure.hi").data("rob").retrieveMono(String.class).block()).isInstanceOf(
				ApplicationErrorException.class);

		String hiRob = this.requester.route("hi").data("rob").retrieveMono(String.class).block();

		assertThat(hiRob).isEqualTo("Hi rob");
	}

	@Test
	public void retrieveFlux() {
		List<String> hi = this.requester.route("hello")
				.data(Flux.just("a"), String.class)
				.retrieveFlux(String.class)
				.collectList()
				.block();

		assertThat(hi).contains("hello a");

//		assertThatCode(() -> this.requester.route("hello")
//				.data(Flux.just("a", "b", "c"), String.class)
//				.retrieveFlux(String.class)
//				.collectList()
//				.block()).isInstanceOf(
//				ApplicationErrorException.class);

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
			return RSocketStrategies.create();
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
								.anyExchange().permitAll();
					});
			return rsocket.build();
		}
	}



	@Controller
	static class ServerController {
		@MessageMapping({"secure.hi", "hi"})
		String hi(String payload) {
			return "Hi " + payload;
		}

		@MessageMapping({"secure.hello", "hello"})
		Flux<String> hi(Flux<String> payload) {
			return payload.map(p -> "hello " + p);
		}
	}

}
