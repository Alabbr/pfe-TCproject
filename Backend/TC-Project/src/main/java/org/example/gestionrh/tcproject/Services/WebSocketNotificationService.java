package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.DTOs.NotificationDTO;
import org.example.gestionrh.tcproject.Entities.SystemNotification;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.SystemNotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SystemNotificationRepository notificationRepository;

    /**
     * Envoie une notification à un utilisateur spécifique et la sauvegarde.
     */
    @Transactional
    public void sendToUser(User user, String title, String message, String type, String actionUrl) {
        SystemNotification notification = SystemNotification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .actionUrl(actionUrl)
                .isRead(false)
                .build();
                
        SystemNotification saved = notificationRepository.save(notification);
        NotificationDTO dto = NotificationDTO.fromEntity(saved);

        // Envoyer sur la queue STOMP privée de l'utilisateur
        messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                dto
        );
    }

    /**
     * Envoie une notification globale (broadcast) et la sauvegarde.
     */
    @Transactional
    public void sendGlobalNotification(String title, String message, String type, String actionUrl) {
        SystemNotification notification = SystemNotification.builder()
                .user(null) // null = all users
                .title(title)
                .message(message)
                .type(type)
                .actionUrl(actionUrl)
                .isRead(false)
                .build();

        SystemNotification saved = notificationRepository.save(notification);
        NotificationDTO dto = NotificationDTO.fromEntity(saved);

        // Envoyer sur le topic STOMP public
        messagingTemplate.convertAndSend("/topic/notifications", dto);
    }

    /**
     * Récupère l'historique des notifications pour un utilisateur
     */
    public List<NotificationDTO> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrUserIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Marque toutes les notifications comme lues pour un utilisateur
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
