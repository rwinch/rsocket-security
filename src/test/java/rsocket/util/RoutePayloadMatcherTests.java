/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rsocket.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.metadata.CompositeMetadataFlyweight;
import io.rsocket.util.DefaultPayload;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.rsocket.annotation.support.DefaultMetadataExtractor;
import org.springframework.messaging.rsocket.annotation.support.MetadataExtractor;
import org.springframework.util.RouteMatcher;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class RoutePayloadMatcherTests {
	@Mock
	private MetadataExtractor metadataExtractor;

	@Mock
	private RouteMatcher routeMatcher;

	@Mock
	private Payload payload;

	@Mock
	private RouteMatcher.Route route;

	private String pattern;

	private RoutePayloadMatcher matcher;

	@Before
	public void setup() {
		this.pattern = "a.b";
		this.matcher = new RoutePayloadMatcher(this.metadataExtractor, this.routeMatcher, this.pattern);
	}

	@Test
	public void matchesWhenNoRouteThenNotMatch() {
		when(this.metadataExtractor.extract(any(), any()))
				.thenReturn(Collections.emptyMap());
		PayloadMatcher.MatchResult result = this.matcher.matches(this.payload).block();
		assertThat(result.isMatch()).isFalse();
	}

	@Test
	public void matchesWhenNotMatchThenNotMatch() {
		String route = "route";
		when(this.metadataExtractor.extract(any(), any()))
				.thenReturn(Collections.singletonMap(MetadataExtractor.ROUTE_KEY, route));
		PayloadMatcher.MatchResult result = this.matcher.matches(this.payload).block();
		assertThat(result.isMatch()).isFalse();
	}

	@Test
	public void matchesWhenMatchAndNoVariablesThenMatch() {
		String route = "route";
		when(this.metadataExtractor.extract(any(), any()))
				.thenReturn(Collections.singletonMap(MetadataExtractor.ROUTE_KEY, route));
		when(this.routeMatcher.parseRoute(any())).thenReturn(this.route);
		when(this.routeMatcher.matchAndExtract(any(), any())).thenReturn(Collections.emptyMap());
		PayloadMatcher.MatchResult result = this.matcher.matches(this.payload).block();
		assertThat(result.isMatch()).isTrue();
	}

	@Test
	public void matchesWhenMatchAndVariablesThenMatchAndVariables() {
		String route = "route";
		Map<String, String> variables = Collections.singletonMap("a", "b");
		when(this.metadataExtractor.extract(any(), any()))
				.thenReturn(Collections.singletonMap(MetadataExtractor.ROUTE_KEY, route));
		when(this.routeMatcher.parseRoute(any())).thenReturn(this.route);
		when(this.routeMatcher.matchAndExtract(any(), any())).thenReturn(variables);
		PayloadMatcher.MatchResult result = this.matcher.matches(this.payload).block();
		assertThat(result.isMatch()).isTrue();
		assertThat(result.getVariables()).containsAllEntriesOf(variables);
	}
}