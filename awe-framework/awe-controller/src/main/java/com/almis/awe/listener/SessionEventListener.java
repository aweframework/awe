package com.almis.awe.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SessionEventListener {
    @EventListener
    public void processSessionCreatedEvent(SessionCreatedEvent event) {
        // do the necessary work
        String sessionId = event.getSessionId();
        // Registrar en logs
        log.debug("Session created: {}", sessionId);
    }

    @EventListener
    public void processSessionDeletedEvent(SessionDeletedEvent event) {
        // do the necessary work
        String sessionId = event.getSessionId();
        // Registrar en logs
        log.debug("Session deleted: {}", sessionId);
    }

    @EventListener
    public void processSessionDestroyedEvent(SessionDestroyedEvent event) {
        // Do the necessary work
        String sessionId = event.getSessionId();
        // Registrar en logs
        log.debug("Session destroyed: {}", sessionId);
    }


    @EventListener
    public void processSessionExpiredEvent(SessionExpiredEvent event) {
        // do the necessary work
        String sessionId = event.getSessionId();
        // Registrar en logs
        log.debug("Session expired: {}", sessionId);
    }
}