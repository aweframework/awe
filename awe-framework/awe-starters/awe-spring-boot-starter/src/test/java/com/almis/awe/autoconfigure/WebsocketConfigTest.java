package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.config.WebsocketStompConfigProperties;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.SessionConfigProperties;
import com.almis.awe.listener.WebSocketEventListener;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.InitService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for WebsocketConfig class
 */
@ExtendWith(SpringExtension.class)
class WebsocketConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebsocketConfig.class))
            .withPropertyValues("awe.application.module-list=awe")
            .withBean(BaseConfigProperties.class)
            .withBean(SecurityConfigProperties.class, () -> {
                SecurityConfigProperties properties = new SecurityConfigProperties();
                properties.setAllowedOriginPatterns(new String[]{"*"});
                return properties;
            })
            .withBean(SessionConfigProperties.class)
            .withBean(SimpMessagingTemplate.class, () -> mock(SimpMessagingTemplate.class))
            .withBean(InitService.class, () -> mock(InitService.class))
            .withBean(SessionRepository.class, () -> {
                @SuppressWarnings("unchecked")
                SessionRepository<Session> sessionRepository = mock(SessionRepository.class);
                when(sessionRepository.findById(anyString())).thenReturn(new MapSession());
                return sessionRepository;
            })
            .withBean(AweClientTracker.class, AweClientTracker::new)
            .withBean(QueryService.class, () -> mock(QueryService.class))
            .withBean(SessionService.class, SessionService::new);

    /**
     * Test that the beans are created correctly with the default configuration
     */
    @Test
    void testDefaultConfiguration() {
        contextRunner.run(context -> {
            // Verify that the beans are created
            assertThat(context).hasSingleBean(AweClientTracker.class);
            assertThat(context).hasSingleBean(AweConnectionTracker.class);
            assertThat(context).hasSingleBean(BroadcastService.class);
            assertThat(context).hasSingleBean(WebSocketEventListener.class);
        });
    }

    /**
     * Test configuration with a simple broker (default)
     */
    @Test
    void testSimpleBrokerConfiguration() {
        contextRunner
                .withPropertyValues("awe.websocket.stomp.enable-stomp-broker-relay=false")
                .run(context -> {
                    // Verify that the WebsocketConfig bean is created
                    assertThat(context).hasSingleBean(WebsocketConfig.class);

                    // Verify that the WebsocketStompConfigProperties bean is created with the expected values
                    WebsocketStompConfigProperties properties = context.getBean(WebsocketStompConfigProperties.class);
                    assertThat(properties.isEnableStompBrokerRelay()).isFalse();
                });
    }

    /**
     * Test configuration with STOMP broker relay
     */
    @Test
    void testStompBrokerRelayConfiguration() {
        contextRunner
                .withPropertyValues(
                        "awe.websocket.stomp.enable-stomp-broker-relay=true",
                        "awe.websocket.stomp.relay-host=test-host",
                        "awe.websocket.stomp.relay-port=61614",
                        "awe.websocket.stomp.client-login=test-client",
                        "awe.websocket.stomp.client-passcode=test-client-pass",
                        "awe.websocket.stomp.system-login=test-system",
                        "awe.websocket.stomp.system-passcode=test-system-pass"
                )
                .run(context -> {
                    // Verify that the WebsocketConfig bean is created
                    assertThat(context).hasSingleBean(WebsocketConfig.class);

                    // Verify that the WebsocketStompConfigProperties bean is created with the expected values
                    WebsocketStompConfigProperties properties = context.getBean(WebsocketStompConfigProperties.class);
                    assertThat(properties.isEnableStompBrokerRelay()).isTrue();
                    assertThat(properties.getRelayHost()).isEqualTo("test-host");
                    assertThat(properties.getRelayPort()).isEqualTo(61614);
                    assertThat(properties.getClientLogin()).isEqualTo("test-client");
                    assertThat(properties.getClientPasscode()).isEqualTo("test-client-pass");
                    assertThat(properties.getSystemLogin()).isEqualTo("test-system");
                    assertThat(properties.getSystemPasscode()).isEqualTo("test-system-pass");
                });
    }

    /**
     * Test endpoint configuration
     */
    @Test
    void testEndpointConfiguration() {
        contextRunner.run(context -> {
            // Verify that the WebsocketConfig bean is created
            assertThat(context).hasSingleBean(WebsocketConfig.class);

            // Verify that the SecurityConfigProperties bean is created with the expected values
            SecurityConfigProperties properties = context.getBean(SecurityConfigProperties.class);
            assertThat(properties.getAllowedOriginPatterns()).containsExactly("*");
        });
    }

    /**
     * Test custom destination prefixes
     */
    @Test
    void testCustomDestinationPrefixes() {
        contextRunner
                .withPropertyValues("awe.websocket.stomp.destination-prefixes=/custom-topic,/custom-queue")
                .run(context -> {
                    // Verify that the WebsocketConfig bean is created
                    assertThat(context).hasSingleBean(WebsocketConfig.class);

                    // Get the WebsocketStompConfigProperties bean
                    WebsocketStompConfigProperties properties = context.getBean(WebsocketStompConfigProperties.class);

                    // Verify the configuration
                    assertThat(properties.getDestinationPrefixes()).contains("/custom-topic", "/custom-queue");
                });
    }
}
