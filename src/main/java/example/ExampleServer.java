package example;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import rsocket.PayloadInterceptorSocketAcceptor;
import security.ReactiveAuthenticationManagerPayloadInterceptor;

import java.util.Arrays;

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
		RSocketFactory.receive()
				// Enable Zero Copy
				.frameDecoder(PayloadDecoder.ZERO_COPY)
//				.addConnectionPlugin(new DuplexConnectionInterceptor() {
//					@Override
//					public DuplexConnection apply(Type type,
//							DuplexConnection duplexConnection) {
//						System.out.println("Connection intercepted");
//						return duplexConnection;
//					}
//				})
				.addRequesterPlugin(new RSocketInterceptor() {
					@Override
					public RSocket apply(RSocket rSocket) {
						System.out.println("Intercepted request");
						return rSocket;
					}
				})
//				.addResponderPlugin(new RSocketInterceptor() {
//					@Override
//					public RSocket apply(RSocket rSocket) {
//						System.out.println("Intercepted Response");
//						return rSocket;
//					}
//				})
				.acceptor(new PayloadInterceptorSocketAcceptor(new HelloHandler(),
						Arrays.asList(new ReactiveAuthenticationManagerPayloadInterceptor(manager))))
				.transport(TcpServerTransport.create(7878))
				.start()
				.block()
				.onClose()
				.block();
	}
}
