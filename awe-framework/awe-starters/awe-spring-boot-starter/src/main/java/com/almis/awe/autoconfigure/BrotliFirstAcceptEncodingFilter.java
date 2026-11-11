package com.almis.awe.autoconfigure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lists {@code br} first in the {@code Accept-Encoding} header of read requests that accept brotli.
 *
 * <p>Since Spring Framework 7 the {@code EncodedResourceResolver} picks the precompressed copy of a resource in the
 * order of the {@code Accept-Encoding} header. Browsers send {@code gzip, deflate, br, zstd}, so they always got the
 * {@code .gz} copy although the {@code .br} one is about 23% smaller. The order of the header carries no preference
 * (RFC 9110, only quality values do), so the server is free to choose: this filter makes it choose brotli, as Spring
 * Framework 6 did (issue #965). A request that refuses brotli ({@code br;q=0}) is left unchanged.</p>
 */
public class BrotliFirstAcceptEncodingFilter extends OncePerRequestFilter {

  private static final String BROTLI = "br";
  private static final Pattern ZERO_QUALITY = Pattern.compile(";\\s*q\\s*=\\s*0(\\.0{0,3})?\\s*$");

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
    throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
    String reordered = isRead(request) ? brotliFirst(header) : header;
    chain.doFilter(reordered == null || reordered.equals(header) ? request : new Wrapper(request, reordered), response);
  }

  private static boolean isRead(HttpServletRequest request) {
    return "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
  }

  /**
   * Move an accepted {@code br} coding to the front of an {@code Accept-Encoding} value.
   *
   * @param header Header value (may be null)
   * @return The header with {@code br} first, or the same value when brotli is absent, refused or already first
   */
  static String brotliFirst(String header) {
    if (header == null) {
      return null;
    }
    List<String> codings = new ArrayList<>();
    for (String token : header.split(",")) {
      if (!token.isBlank()) {
        codings.add(token.trim());
      }
    }
    for (int index = 1; index < codings.size(); index++) {
      String coding = codings.get(index);
      String name = coding.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
      if (BROTLI.equals(name) && !ZERO_QUALITY.matcher(coding).find()) {
        codings.add(0, codings.remove(index));
        return String.join(", ", codings);
      }
    }
    return header;
  }

  private static final class Wrapper extends HttpServletRequestWrapper {

    private final String acceptEncoding;

    private Wrapper(HttpServletRequest request, String acceptEncoding) {
      super(request);
      this.acceptEncoding = acceptEncoding;
    }

    @Override
    public String getHeader(String name) {
      return HttpHeaders.ACCEPT_ENCODING.equalsIgnoreCase(name) ? acceptEncoding : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      return HttpHeaders.ACCEPT_ENCODING.equalsIgnoreCase(name)
        ? Collections.enumeration(List.of(acceptEncoding))
        : super.getHeaders(name);
    }
  }
}
