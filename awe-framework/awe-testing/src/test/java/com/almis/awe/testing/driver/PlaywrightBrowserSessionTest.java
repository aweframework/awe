package com.almis.awe.testing.driver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Session of the Playwright tool: what the factory opens and how it is disposed of
 */
class PlaywrightBrowserSessionTest {

  @Test
  void closingTheSessionClosesTheContextTheBrowserAndPlaywrightInThatOrder() {
    Playwright playwright = mock(Playwright.class);
    Browser browser = mock(Browser.class);
    BrowserContext context = mock(BrowserContext.class);
    PlaywrightBrowserDriverFactory.PlaywrightBrowserSession session =
      new PlaywrightBrowserDriverFactory.PlaywrightBrowserSession(playwright, browser, context, null);

    session.close();

    org.mockito.InOrder order = inOrder(context, browser, playwright);
    order.verify(context).close();
    order.verify(browser).close();
    order.verify(playwright).close();
  }

  @Test
  void aFailureClosingOneResourceDoesNotSkipTheOthers() {
    Playwright playwright = mock(Playwright.class);
    Browser browser = mock(Browser.class);
    BrowserContext context = mock(BrowserContext.class);
    doThrow(new PlaywrightException("context is gone")).when(context).close();
    doThrow(new PlaywrightException("browser is gone")).when(browser).close();
    PlaywrightBrowserDriverFactory.PlaywrightBrowserSession session =
      new PlaywrightBrowserDriverFactory.PlaywrightBrowserSession(playwright, browser, context, null);

    assertThatThrownBy(session::close).isInstanceOf(PlaywrightException.class).hasMessageContaining("context is gone")
      .satisfies(failure -> assertThat(failure.getSuppressed()).hasSize(1));

    verify(browser).close();
    verify(playwright).close();
  }

  @Test
  void theVideoIsResolvedAfterTheContextIsClosedBecauseThatIsWhenItIsComplete() {
    Playwright playwright = mock(Playwright.class);
    Browser browser = mock(Browser.class);
    BrowserContext context = mock(BrowserContext.class);
    PlaywrightEvidence evidence = mock(PlaywrightEvidence.class);
    PlaywrightBrowserDriverFactory.PlaywrightBrowserSession session =
      new PlaywrightBrowserDriverFactory.PlaywrightBrowserSession(playwright, browser, context, null, evidence);

    session.close();

    org.mockito.InOrder order = inOrder(context, evidence, browser);
    order.verify(context).close();
    order.verify(evidence).finishVideo();
    order.verify(browser).close();
  }

  @Test
  void theLifecycleOfTheTestsGoesToTheEvidence() {
    PlaywrightEvidence evidence = mock(PlaywrightEvidence.class);
    PlaywrightBrowserDriverFactory.PlaywrightBrowserSession session =
      new PlaywrightBrowserDriverFactory.PlaywrightBrowserSession(mock(Playwright.class), mock(Browser.class),
        mock(BrowserContext.class), null, evidence);
    BrowserSession.EvidenceListener listener = (label, file, attach) -> { };

    session.testStarted("LoginIT", "t010_login");
    session.testFinished("t010_login", "name", true);
    session.onEvidence(listener);

    verify(evidence).testStarted("LoginIT", "t010_login");
    verify(evidence).testFinished("t010_login", "name", true);
    verify(evidence).setListener(listener);
  }

  @Test
  void aSessionWithoutEvidenceIgnoresTheLifecycle() {
    PlaywrightBrowserDriverFactory.PlaywrightBrowserSession session =
      new PlaywrightBrowserDriverFactory.PlaywrightBrowserSession(mock(Playwright.class), mock(Browser.class),
        mock(BrowserContext.class), null);

    org.assertj.core.api.Assertions.assertThatCode(() -> {
      session.testStarted("LoginIT", "t010_login");
      session.testFinished("t010_login", "name", true);
      session.onEvidence((label, file, attach) -> { });
    }).doesNotThrowAnyException();
  }
}
