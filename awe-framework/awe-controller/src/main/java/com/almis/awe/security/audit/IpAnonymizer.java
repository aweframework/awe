package com.almis.awe.security.audit;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Client address anonymization for the security audit: the last octet of IPv4 addresses and everything after the
 * first 48 bits (three groups) of IPv6 addresses are set to zero. Addresses that cannot be parsed are dropped, so a
 * value is never stored in clear by mistake.
 */
public final class IpAnonymizer {

  private static final Pattern IPV4 = Pattern.compile(
    "^(25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)$");
  private static final Pattern HEX_GROUP = Pattern.compile("^[0-9a-fA-F]{1,4}$");
  private static final String IPV4_MAPPED_PREFIX = "::ffff:";
  private static final int IPV6_GROUPS = 8;
  private static final int KEPT_IPV6_GROUPS = 3;

  private IpAnonymizer() {
  }

  /**
   * Anonymize an address
   *
   * @param address IPv4 or IPv6 literal address
   * @return Anonymized address, or null when the address cannot be parsed
   */
  public static String anonymize(String address) {
    if (address == null || address.isBlank()) {
      return null;
    }
    String value = address.trim();
    if (IPV4.matcher(value).matches()) {
      return value.substring(0, value.lastIndexOf('.')) + ".0";
    }
    if (value.toLowerCase(Locale.ROOT).startsWith(IPV4_MAPPED_PREFIX)) {
      String ipv4 = anonymizeIpv4(value.substring(IPV4_MAPPED_PREFIX.length()));
      return ipv4 == null ? null : IPV4_MAPPED_PREFIX + ipv4;
    }
    return value.contains(":") ? anonymizeIpv6(value) : null;
  }

  private static String anonymizeIpv4(String value) {
    return IPV4.matcher(value).matches() ? value.substring(0, value.lastIndexOf('.')) + ".0" : null;
  }

  private static String anonymizeIpv6(String value) {
    // Drop the zone (fe80::1%eth0)
    int zone = value.indexOf('%');
    String address = zone >= 0 ? value.substring(0, zone) : value;
    String[] groups = expand(address);
    if (groups == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < KEPT_IPV6_GROUPS; i++) {
      builder.append(Integer.toHexString(Integer.parseInt(groups[i], 16))).append(':');
    }
    return builder.append(':').toString();
  }

  private static String[] expand(String address) {
    int compressed = address.indexOf("::");
    if (compressed != address.lastIndexOf("::")) {
      return null;
    }
    String[] head = split(compressed >= 0 ? address.substring(0, compressed) : address);
    String[] tail = compressed >= 0 ? split(address.substring(compressed + 2)) : new String[0];
    if (head == null || tail == null) {
      return null;
    }
    int missing = IPV6_GROUPS - head.length - tail.length;
    if ((compressed >= 0 && missing < 1) || (compressed < 0 && missing != 0)) {
      return null;
    }
    String[] groups = new String[IPV6_GROUPS];
    System.arraycopy(head, 0, groups, 0, head.length);
    for (int i = 0; i < missing; i++) {
      groups[head.length + i] = "0";
    }
    System.arraycopy(tail, 0, groups, head.length + missing, tail.length);
    return groups;
  }

  private static String[] split(String part) {
    if (part.isEmpty()) {
      return new String[0];
    }
    String[] groups = part.split(":", -1);
    for (String group : groups) {
      if (!HEX_GROUP.matcher(group).matches()) {
        return null;
      }
    }
    return groups;
  }
}
