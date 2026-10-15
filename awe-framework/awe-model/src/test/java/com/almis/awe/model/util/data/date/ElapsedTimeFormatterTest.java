package com.almis.awe.model.util.data.date;

import com.almis.awe.model.component.AweElements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ElapsedTimeFormatterTest {

  private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

  @Mock
  private AweElements elements;

  @BeforeEach
  void setUp() {
    when(elements.getLanguage()).thenReturn("es-ES");
    doAnswer(invocation -> {
      Object[] arguments = invocation.getArguments();
      String tokens = Arrays.stream(arguments, 2, arguments.length).map(String::valueOf).collect(Collectors.joining(","));
      return arguments[0] + "@" + arguments[1] + "[" + tokens + "]";
    }).when(elements).getLocaleWithLanguage(anyString(), anyString(), any(Object[].class));
  }

  @Test
  void formatUsesTheLanguageOfTheElements() {
    assertThat(ElapsedTimeFormatter.format(Duration.ofMinutes(7), elements)).isEqualTo("MINUTES@es-ES[7]");
  }

  @Test
  void sinceMeasuresAgainstTheClock() {
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    assertThat(ElapsedTimeFormatter.since(NOW.minus(Duration.ofDays(3)), clock, elements))
      .isEqualTo("TIME_AGO@es-ES[DAYS@es-ES[3]]");
    assertThat(ElapsedTimeFormatter.since(NOW.minusSeconds(42), clock, elements))
      .isEqualTo("TIME_AGO@es-ES[SECONDS@es-ES[42]]");
  }
}
