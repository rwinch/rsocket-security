package rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.CompositeMetadataFlyweight;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * @author Rob Winch
 */
public class BasicAuthenticationUtils {
	public static final String BASIC_AUTHENTICATION_MIME_TYPE = "x.spring-security/authentication/basic.v0";

	public static void writeBasic(CompositeByteBuf compositeMetaData, UsernamePassword usernamePassword) {
		String username = usernamePassword.getUsername();
		String password = usernamePassword.getPassword();
		ByteBuf metadata = Unpooled.buffer();
		byte[] credentials = Base64.getEncoder().encode((username + ":" + password).getBytes(StandardCharsets.US_ASCII));
		metadata.writeBytes(credentials);

		CompositeMetadataFlyweight.encodeAndAddMetadata(compositeMetaData, ByteBufAllocator.DEFAULT, BASIC_AUTHENTICATION_MIME_TYPE, metadata);
	}

	public static class UsernamePassword {
		private final String username;
		private final String password;

		public UsernamePassword(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return this.username;
		}

		public String getPassword() {
			return this.password;
		}
	}

	public static Optional<UsernamePassword> readBasic(Payload payload) {
		return findByType(payload, BASIC_AUTHENTICATION_MIME_TYPE)
			.map(b -> b.toString(StandardCharsets.UTF_8))
			.flatMap(BasicAuthenticationUtils::decode);
	}

	private static Optional<UsernamePassword> decode(String encoded) {
		byte[] decoded = Base64.getDecoder().decode(encoded);
		String token = new String(decoded);
		int delim = token.indexOf(":");

		if (delim == -1) {
			return Optional.empty();
		}
		String username = token.substring(0, delim);
		String password = token.substring(delim + 1);
		return Optional.of(new UsernamePassword(username, password));
	}

	private static Optional<ByteBuf> findByType(Payload payload, String type) {
		CompositeMetadata compositeMetadata = new CompositeMetadata(payload.metadata(),
				false);
		return StreamSupport.stream(compositeMetadata.spliterator(), false)
				.filter(entry -> type.equals(entry.getMimeType()))
				.map(CompositeMetadata.Entry::getContent)
				.findFirst();
	}
}
