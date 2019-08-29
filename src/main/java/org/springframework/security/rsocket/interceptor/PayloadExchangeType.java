package org.springframework.security.rsocket.interceptor;

/**
 * @author Rob Winch
 */
public enum PayloadExchangeType {
	SETUP,
	FIRE_AND_FORGET,
	REQUEST_RESPONSE,
	REQUEST_STREAM,
	REQUEST_CHANNEL,
	METADATA_PUSH
}
