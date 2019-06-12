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

	public static void writeBasic(CompositeByteBuf compositeMetaData, String username, String password) {
		ByteBuf metadata = Unpooled.buffer();
		byte[] credentials = Base64.getEncoder().encode((username + ":" + password).getBytes(StandardCharsets.US_ASCII));
		metadata.writeBytes(credentials);

		CompositeMetadataFlyweight.encodeAndAddMetadata(compositeMetaData, ByteBufAllocator.DEFAULT, BASIC_AUTHENTICATION_MIME_TYPE, metadata);
	}

	public static Optional<String> readBasic(Payload payload) {
		return findByType(payload, BASIC_AUTHENTICATION_MIME_TYPE).map(b -> b.toString(StandardCharsets.UTF_8));
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
