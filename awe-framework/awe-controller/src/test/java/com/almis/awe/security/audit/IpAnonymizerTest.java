package com.almis.awe.security.audit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IpAnonymizerTest {

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    "192.168.1.27, 192.168.1.0",
    "10.0.0.1, 10.0.0.0",
    "2001:db8:85a3:8d3:1319:8a2e:370:7348, 2001:db8:85a3::",
    "2001:0DB8:0000:0000:0000:0000:1428:57ab, 2001:db8:0::",
    "2001:db8::1, 2001:db8:0::",
    "::1, 0:0:0::",
    "fe80::1%eth0, fe80:0:0::",
    "::ffff:192.168.1.27, ::ffff:192.168.1.0"
  })
  void anonymizes(String address, String expected) {
    assertThat(IpAnonymizer.anonymize(address)).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"unknown", "300.1.1", "1:2:3:4:5:6:7:8:9", "abc::xyz", "example.com"})
  void unparseableAddressesAreDropped(String address) {
    assertThat(IpAnonymizer.anonymize(address)).isNull();
  }
}
