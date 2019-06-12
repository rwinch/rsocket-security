package rsocket.metadata;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.junit.Test;
import rsocket.metadata.SecurityMetadataFlyweight.UsernamePassword;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Winch
 */
public class SecurityMetadataFlyweightTest {
	@Test
	public void basicAuthentication() {
		UsernamePassword expectedCredentials = new UsernamePassword("rob", "password");
		CompositeByteBuf metadata = ByteBufAllocator.DEFAULT.compositeBuffer();

		SecurityMetadataFlyweight.writeBasic(metadata, expectedCredentials);

		UsernamePassword actualCredentials = SecurityMetadataFlyweight.readBasic(metadata)
				.orElse(null);

		assertThat(actualCredentials).isEqualToComparingFieldByField(expectedCredentials);
	}

}