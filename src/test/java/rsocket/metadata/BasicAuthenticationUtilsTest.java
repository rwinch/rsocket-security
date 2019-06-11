package rsocket.metadata;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Rob Winch
 */
public class BasicAuthenticationUtilsTest {
	@Test
	public void go() {

		CompositeByteBuf metadata = ByteBufAllocator.DEFAULT.compositeBuffer();
		BasicAuthenticationUtils.writeBasic(metadata, "rob", "password");
	}

}