package org.springframework.security.rsocket.interceptor;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Rob Winch
 */
public class PayloadSocketAcceptor implements SocketAcceptor {
	private final SocketAcceptor delegate;

	private final List<PayloadInterceptor> interceptors;

	@Nullable
	private MimeType defaultDataMimeType;

	private MimeType defaultMetadataMimeType = MetadataExtractor.COMPOSITE_METADATA;

	public PayloadSocketAcceptor(SocketAcceptor delegate, List<PayloadInterceptor> interceptors) {
		Assert.notNull(delegate, "delegate cannot be null");
		if (interceptors == null) {
			throw new IllegalArgumentException("interceptors cannot be null");
		}
		if (interceptors.isEmpty()) {
			throw new IllegalArgumentException("interceptors cannot be empty");
		}
		this.delegate = delegate;
		this.interceptors = interceptors;
	}

	@Override
	public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
		MimeType dataMimeType = parseMimeType(setup.dataMimeType(), this.defaultDataMimeType);
		Assert.notNull(dataMimeType, "No `dataMimeType` in ConnectionSetupPayload and no default value");

		MimeType metadataMimeType = parseMimeType(setup.metadataMimeType(), this.defaultMetadataMimeType);
		Assert.notNull(metadataMimeType, "No `metadataMimeType` in ConnectionSetupPayload and no default value");
		// FIXME do we want to make the sendingSocket available in the PayloadExchange
		return this.delegate.accept(setup, sendingSocket)
			.map(acceptingSocket -> new PayloadInterceptorRSocket(acceptingSocket, this.interceptors, metadataMimeType, dataMimeType));
	}

	private MimeType parseMimeType(String str, MimeType defaultMimeType) {
		return StringUtils.hasText(str) ? MimeTypeUtils.parseMimeType(str) : defaultMimeType;
	}

	public void setDefaultDataMimeType(@Nullable MimeType defaultDataMimeType) {
		this.defaultDataMimeType = defaultDataMimeType;
	}

	public void setDefaultMetadataMimeType(MimeType defaultMetadataMimeType) {
		Assert.notNull(defaultMetadataMimeType, "defaultMetadataMimeType cannot be null");
		this.defaultMetadataMimeType = defaultMetadataMimeType;
	}
}
