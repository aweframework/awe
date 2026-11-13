package com.almis.awe.autoconfigure;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.HttpResource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BrotliFirstAcceptEncodingFilter}: browsers that accept brotli get the {@code .br} copy of a
 * precompressed resource, although they list {@code gzip} first (issue #965).
 */
class BrotliFirstAcceptEncodingFilterTest {

  private static final String BROWSER_ACCEPT_ENCODING = "gzip, deflate, br, zstd";

  private final BrotliFirstAcceptEncodingFilter filter = new BrotliFirstAcceptEncodingFilter();

  @Test
  void aBrowserThatAcceptsBrotliGetsTheBrotliCopy(@TempDir Path folder) throws Exception {
    Files.writeString(folder.resolve("bundle.js"), "console.log('awe');");
    Files.writeString(folder.resolve("bundle.js.gz"), "gzip");
    Files.writeString(folder.resolve("bundle.js.br"), "brotli");

    HttpServletRequest seen = filter(request("GET", BROWSER_ACCEPT_ENCODING));
    Resource resolved = resolve(seen, "bundle.js", new FileSystemResource(folder.toFile() + "/"));

    assertThat(((HttpResource) resolved).getResponseHeaders().getFirst(HttpHeaders.CONTENT_ENCODING)).isEqualTo("br");
  }

  @Test
  void brotliIsMovedFirstAndTheOtherCodingsKeepTheirOrder() throws Exception {
    HttpServletRequest seen = filter(request("GET", BROWSER_ACCEPT_ENCODING));

    assertThat(seen.getHeader("accept-encoding")).isEqualTo("br, gzip, deflate, zstd");
    assertThat(Collections.list(seen.getHeaders(HttpHeaders.ACCEPT_ENCODING))).containsExactly("br, gzip, deflate, zstd");
  }

  @Test
  void requestsWithoutBrotliOrThatRefuseItAreUnchanged() throws Exception {
    for (String header : List.of("gzip, deflate", "gzip, br;q=0", "br, gzip")) {
      MockHttpServletRequest request = request("GET", header);

      assertThat(filter(request)).isSameAs(request);
    }
  }

  @Test
  void onlyReadRequestsAreChanged() throws Exception {
    MockHttpServletRequest request = request("POST", BROWSER_ACCEPT_ENCODING);

    assertThat(filter(request)).isSameAs(request);
  }

  @Test
  void isRegisteredWhenPrecompressedResourcesAreServed() {
    WebApplicationContextRunner runner = new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(PrecompressedResourcesConfig.class));

    runner.withPropertyValues("spring.web.resources.chain.compressed=true")
      .run(context -> assertThat(context).hasSingleBean(BrotliFirstAcceptEncodingFilter.class));
    runner.withPropertyValues("spring.web.resources.chain.compressed=false")
      .run(context -> assertThat(context).doesNotHaveBean(BrotliFirstAcceptEncodingFilter.class));
  }

  private static MockHttpServletRequest request(String method, String acceptEncoding) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, "/js/bundle.js");
    request.addHeader(HttpHeaders.ACCEPT_ENCODING, acceptEncoding);
    return request;
  }

  private HttpServletRequest filter(HttpServletRequest request) throws Exception {
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    return (HttpServletRequest) chain.getRequest();
  }

  private static Resource resolve(HttpServletRequest request, String path, Resource location) {
    PathResourceResolver pathResolver = new PathResourceResolver();
    ResourceResolverChain next = new ResourceResolverChain() {
      @Override
      public Resource resolveResource(HttpServletRequest req, String requestPath, List<? extends Resource> locations) {
        return pathResolver.resolveResource(req, requestPath, locations, this);
      }

      @Override
      public String resolveUrlPath(String resourcePath, List<? extends Resource> locations) {
        return null;
      }
    };
    return new EncodedResourceResolver().resolveResource(request, path, List.of(location), next);
  }
}
