package org.springframework.security.rsocket.metadata;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.security.rsocket.metadata.SecurityMetadataFlyweight.UsernamePassword;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Rob Winch
 */
public class BasicAuthenticationEncoder extends
		AbstractEncoder<UsernamePassword> {

	public BasicAuthenticationEncoder() {
		super(SecurityMetadataFlyweight.BASIC_AUTHENTICATION_MIME_TYPE);
	}

	@Override
	public Flux<DataBuffer> encode(
			Publisher<? extends UsernamePassword> inputStream,
			DataBufferFactory bufferFactory, ResolvableType elementType,
			MimeType mimeType, Map<String, Object> hints) {
		return Flux.from(inputStream).map(credentials ->
				encodeValue(credentials, bufferFactory, elementType, mimeType, hints));
	}

	@Override
	public DataBuffer encodeValue(UsernamePassword credentials,
			DataBufferFactory bufferFactory, ResolvableType valueType, MimeType mimeType,
			Map<String, Object> hints) {
		// FIXME: Should leverage SecurityMetadataFlyweight
		String username = credentials.getUsername();
		String password = credentials.getPassword();
		DataBuffer metadata = bufferFactory.allocateBuffer();
		byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
		byte[] usernameBytesLengthBytes = ByteBuffer.allocate(4).putInt(usernameBytes.length).array();
		metadata.write(usernameBytesLengthBytes);
		metadata.write(usernameBytes);
		metadata.write(password.getBytes(StandardCharsets.UTF_8));
		return metadata;
	}
}
