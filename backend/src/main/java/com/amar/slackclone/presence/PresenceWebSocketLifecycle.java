package com.amar.slackclone.presence;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

@Component
public class PresenceWebSocketLifecycle {
    private final PresenceService presence;
    public PresenceWebSocketLifecycle(PresenceService presence){this.presence=presence;}
    @EventListener public void connected(SessionConnectedEvent event){var a=event.getUser();if(a!=null)presence.connect(event.getMessage().getHeaders().get("simpSessionId",String.class),a.getName());}
    @EventListener public void disconnected(SessionDisconnectEvent event){presence.disconnect(event.getSessionId());}
}
