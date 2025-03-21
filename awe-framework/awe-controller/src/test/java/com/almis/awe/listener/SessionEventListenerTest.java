package com.almis.awe.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.events.SessionExpiredEvent;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionEventListenerTest {

    @InjectMocks
    private SessionEventListener sessionEventListener;

    @Mock
    private SessionCreatedEvent sessionCreatedEvent;

    @Mock
    private SessionDeletedEvent sessionDeletedEvent;

    @Mock
    private SessionDestroyedEvent sessionDestroyedEvent;

    @Mock
    private SessionExpiredEvent sessionExpiredEvent;

    @Test
    void processSessionCreatedEvent() {
        sessionEventListener.processSessionCreatedEvent(sessionCreatedEvent);
        verify(sessionCreatedEvent, times(1)).getSessionId();
    }

    @Test
    void processSessionDeletedEvent() {
        sessionEventListener.processSessionDeletedEvent(sessionDeletedEvent);
        verify(sessionDeletedEvent, times(1)).getSessionId();
    }

    @Test
    void processSessionDestroyedEvent() {
        sessionEventListener.processSessionDestroyedEvent(sessionDestroyedEvent);
        verify(sessionDestroyedEvent, times(1)).getSessionId();
    }

    @Test
    void processSessionExpiredEvent() {
        sessionEventListener.processSessionExpiredEvent(sessionExpiredEvent);
        verify(sessionExpiredEvent, times(1)).getSessionId();
    }
}