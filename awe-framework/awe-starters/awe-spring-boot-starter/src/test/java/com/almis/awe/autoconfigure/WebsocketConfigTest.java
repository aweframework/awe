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
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.StompBrokerRelayRegistration;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
     * Test configuration with STOMP broker relay - verifies properties are correctly loaded
     * Note: We only test that properties are correctly loaded, not the full broker initialization
     * which would require an actual broker server running.
     */
    @Test
    void testStompBrokerRelayConfiguration() {
        new WebApplicationContextRunner()
                .withInitializer(context -> context.getBeanFactory().registerSingleton("websocketStompConfigProperties",
                        createWebsocketStompConfigProperties()))
                .run(context -> {
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

    private WebsocketStompConfigProperties createWebsocketStompConfigProperties() {
        WebsocketStompConfigProperties properties = new WebsocketStompConfigProperties();
        properties.setEnableStompBrokerRelay(true);
        properties.setRelayHost("test-host");
        properties.setRelayPort(61614);
        properties.setClientLogin("test-client");
        properties.setClientPasscode("test-client-pass");
        properties.setSystemLogin("test-system");
        properties.setSystemPasscode("test-system-pass");
        return properties;
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
    /**
     * Test that the virtual host is applied to the broker relay when it is configured
     */
    @Test
    void testStompBrokerRelayAppliesVirtualHostWhenConfigured() {
        StompBrokerRelayRegistration relayRegistration = mock(StompBrokerRelayRegistration.class, RETURNS_SELF);
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        when(registry.enableStompBrokerRelay(any(String[].class))).thenReturn(relayRegistration);

        WebsocketStompConfigProperties properties = createWebsocketStompConfigProperties();
        properties.setVirtualHost("tenant-a");

        websocketConfig(properties).configureMessageBroker(registry);

        verify(relayRegistration).setVirtualHost("tenant-a");
    }

    /**
     * Test that the virtual host is left untouched when it is not configured, so the broker
     * keeps applying its own default (the relay host)
     */
    @Test
    void testStompBrokerRelayDoesNotApplyVirtualHostWhenNotConfigured() {
        StompBrokerRelayRegistration relayRegistration = mock(StompBrokerRelayRegistration.class, RETURNS_SELF);
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        when(registry.enableStompBrokerRelay(any(String[].class))).thenReturn(relayRegistration);

        websocketConfig(createWebsocketStompConfigProperties()).configureMessageBroker(registry);

        verify(relayRegistration, never()).setVirtualHost(any());
    }

    /**
     * Test that a blank virtual host is treated as not configured
     */
    @Test
    void testStompBrokerRelayIgnoresBlankVirtualHost() {
        StompBrokerRelayRegistration relayRegistration = mock(StompBrokerRelayRegistration.class, RETURNS_SELF);
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        when(registry.enableStompBrokerRelay(any(String[].class))).thenReturn(relayRegistration);

        WebsocketStompConfigProperties properties = createWebsocketStompConfigProperties();
        properties.setVirtualHost("   ");

        websocketConfig(properties).configureMessageBroker(registry);

        verify(relayRegistration, never()).setVirtualHost(any());
    }

    /**
     * Test that the virtual host property is bound from the configuration
     */
    @Test
    void testVirtualHostPropertyIsBound() {
        contextRunner
                .withPropertyValues("awe.websocket.stomp.virtual-host=tenant-b")
                .run(context -> {
                    WebsocketStompConfigProperties properties = context.getBean(WebsocketStompConfigProperties.class);
                    assertThat(properties.getVirtualHost()).isEqualTo("tenant-b");
                });
    }

    /**
     * Test that the virtual host is not set by default
     */
    @Test
    void testVirtualHostIsNullByDefault() {
        contextRunner.run(context -> {
            WebsocketStompConfigProperties properties = context.getBean(WebsocketStompConfigProperties.class);
            assertThat(properties.getVirtualHost()).isNull();
        });
    }

    private WebsocketConfig websocketConfig(WebsocketStompConfigProperties stompProperties) {
        BaseConfigProperties base = new BaseConfigProperties();
        base.setAcronym("awe");
        return new WebsocketConfig(base, new SecurityConfigProperties(), stompProperties);
    }
}
