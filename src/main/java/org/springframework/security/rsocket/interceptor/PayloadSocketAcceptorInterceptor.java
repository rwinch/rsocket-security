package org.springframework.security.rsocket.interceptor;

import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

/**
 * @author Rob Winch
 */
public class PayloadSocketAcceptorInterceptor implements SocketAcceptorInterceptor {

	private final List<PayloadInterceptor> interceptors;

	@Nullable
	private MimeType defaultDataMimeType;

	private MimeType defaultMetadataMimeType =
		MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());

	public PayloadSocketAcceptorInterceptor(List<PayloadInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	@Override
	public SocketAcceptor apply(SocketAcceptor socketAcceptor) {
		PayloadSocketAcceptor acceptor = new PayloadSocketAcceptor(
				socketAcceptor, this.interceptors);
		acceptor.setDefaultDataMimeType(this.defaultDataMimeType);
		acceptor.setDefaultMetadataMimeType(this.defaultMetadataMimeType);
		return acceptor;
	}

	public void setDefaultDataMimeType(@Nullable MimeType defaultDataMimeType) {
		this.defaultDataMimeType = defaultDataMimeType;
	}

	public void setDefaultMetadataMimeType(MimeType defaultMetadataMimeType) {
		Assert.notNull(defaultMetadataMimeType, "defaultMetadataMimeType cannot be null");
		this.defaultMetadataMimeType = defaultMetadataMimeType;
	}
}
