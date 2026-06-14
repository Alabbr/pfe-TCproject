package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserPresenceService {

    private final SimpMessagingTemplate messagingTemplate;
    
    // Map to keep track of connected users (email -> session count)
    // A user can have multiple tabs open, so we count sessions.
    private final Map<String, Integer> onlineUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        if (headers.getUser() != null) {
            String username = headers.getUser().getName(); // Email
            int count = onlineUsers.getOrDefault(username, 0);
            onlineUsers.put(username, count + 1);
            
            // If it's their first connection, broadcast they are online
            if (count == 0) {
                broadcastPresence(username, true);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        if (headers.getUser() != null) {
            String username = headers.getUser().getName(); // Email
            int count = onlineUsers.getOrDefault(username, 0);
            if (count <= 1) {
                onlineUsers.remove(username);
                broadcastPresence(username, false);
            } else {
                onlineUsers.put(username, count - 1);
            }
        }
    }

    public Set<String> getOnlineUsers() {
        return onlineUsers.keySet();
    }

    private void broadcastPresence(String username, boolean isOnline) {
        messagingTemplate.convertAndSend("/topic/presence", (Object) Map.of(
            "email", username,
            "isOnline", isOnline
        ));
    }
}
