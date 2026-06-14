package org.example.gestionrh.tcproject.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private String content;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String senderAvatarUrl;
    private String senderColor; // On peut générer ça côté frontend ou ici
    private Long receiverId;
    private String channelId;
    private LocalDateTime timestamp;
    private Boolean isRead;
    private LocalDateTime readAt;
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentType;
    private Boolean isEdited;
    private Boolean isForwarded;
    private java.util.Map<String, java.util.List<Long>> reactions;
}
