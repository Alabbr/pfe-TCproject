package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.response.ChatMessageDto;
import org.example.gestionrh.tcproject.Entities.ChatMessage;
import org.example.gestionrh.tcproject.Entities.MessageReaction;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.ChatMessageRepository;
import org.example.gestionrh.tcproject.Repositories.MessageReactionRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final MessageReactionRepository messageReactionRepository;

    // Sauvegarde un nouveau message (canal ou DM) à partir du DTO reçu via WebSocket
    public ChatMessageDto saveMessage(ChatMessageDto msgDto) {
        User sender = userRepository.findById(msgDto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        User receiver = null;
        if (msgDto.getReceiverId() != null) {
            receiver = userRepository.findById(msgDto.getReceiverId()).orElse(null);
        }

        ChatMessage msg = ChatMessage.builder()
                .content(msgDto.getContent() != null ? msgDto.getContent() : "")
                .sender(sender)
                .receiver(receiver)
                .channelId(msgDto.getChannelId())
                .attachmentUrl(msgDto.getAttachmentUrl())
                .attachmentName(msgDto.getAttachmentName())
                .attachmentType(msgDto.getAttachmentType())
                .isForwarded(msgDto.getIsForwarded() != null && msgDto.getIsForwarded())
                .build();

        ChatMessage savedMsg = chatMessageRepository.save(msg);
        return mapToDto(savedMsg);
    }

    // Récupère tous les messages d'un canal (ex: "general") triés par date croissante
    public List<ChatMessageDto> getChannelMessages(String channelId) {
        return chatMessageRepository.findByChannelIdOrderByTimestampAsc(channelId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Récupère l'historique des messages privés (DM) entre deux utilisateurs
    public List<ChatMessageDto> getDirectMessages(Long userId1, Long userId2) {
        return chatMessageRepository.findDirectMessages(userId1, userId2)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Modifie le contenu d'un message existant (seul l'expéditeur peut modifier son propre message)
    @Transactional
    public ChatMessageDto editMessage(Long messageId, String newContent, Long senderId) {
        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        // Only the sender can edit their own message
        if (!msg.getSender().getId().equals(senderId)) {
            throw new RuntimeException("You can only edit your own messages");
        }

        msg.setContent(newContent);
        msg.setEdited(true);
        ChatMessage savedMsg = chatMessageRepository.save(msg);
        return mapToDto(savedMsg);
    }

    // Marque tous les messages non lus d'un expéditeur comme lus pour le destinataire
    @Transactional
    public void markMessagesAsRead(Long senderId, Long readerId) {
        List<ChatMessage> unreadMessages = chatMessageRepository.findDirectMessages(senderId, readerId)
                .stream()
                .filter(m -> m.getSender().getId().equals(senderId) && !m.isRead())
                .collect(Collectors.toList());
        
        for (ChatMessage msg : unreadMessages) {
            msg.setRead(true);
            msg.setReadAt(java.time.LocalDateTime.now());
        }
        chatMessageRepository.saveAll(unreadMessages);
    }

    // Ajoute ou retire une réaction emoji sur un message (toggle on/off)
    @Transactional
    public ChatMessageDto toggleReaction(Long messageId, Long userId, String emoji) {
        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<MessageReaction> existingReaction = messageReactionRepository
                .findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

        if (existingReaction.isPresent()) {
            // Remove reaction (toggle off)
            msg.getReactions().remove(existingReaction.get());
            messageReactionRepository.delete(existingReaction.get());
        } else {
            // Add reaction
            MessageReaction reaction = MessageReaction.builder()
                    .message(msg)
                    .user(user)
                    .emoji(emoji)
                    .build();
            msg.getReactions().add(reaction);
            messageReactionRepository.save(reaction);
        }

        return mapToDto(chatMessageRepository.findById(messageId).orElse(msg));
    }

    // Convertit une entité ChatMessage en DTO pour l'envoi au frontend (inclut avatar, réactions, etc.)
    private ChatMessageDto mapToDto(ChatMessage msg) {
        String initials = msg.getSender().getFirstName().substring(0, 1) + 
                          msg.getSender().getLastName().substring(0, 1);

        // Build reactions map: { "👍": [1, 3], "❤️": [2] }
        Map<String, List<Long>> reactionsMap = new HashMap<>();
        if (msg.getReactions() != null) {
            for (MessageReaction r : msg.getReactions()) {
                reactionsMap.computeIfAbsent(r.getEmoji(), k -> new ArrayList<>())
                            .add(r.getUser().getId());
            }
        }

        return ChatMessageDto.builder()
                .id(msg.getId())
                .content(msg.getContent())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getFirstName() + " " + msg.getSender().getLastName())
                .senderAvatar(initials.toUpperCase())
                .senderAvatarUrl(msg.getSender().getProfilePictureUrl())
                .senderColor(msg.getSender().getProfilePictureUrl())
                .receiverId(msg.getReceiver() != null ? msg.getReceiver().getId() : null)
                .channelId(msg.getChannelId())
                .timestamp(msg.getTimestamp())
                .isRead(msg.isRead())
                .readAt(msg.getReadAt())
                .attachmentUrl(msg.getAttachmentUrl())
                .attachmentName(msg.getAttachmentName())
                .attachmentType(msg.getAttachmentType())
                .isEdited(msg.isEdited())
                .isForwarded(msg.isForwarded())
                .reactions(reactionsMap)
                .build();
    }
}

