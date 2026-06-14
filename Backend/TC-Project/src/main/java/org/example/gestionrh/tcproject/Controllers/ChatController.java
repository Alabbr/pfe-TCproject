package org.example.gestionrh.tcproject.Controllers;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.response.ChatMessageDto;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Services.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.example.gestionrh.tcproject.Repositories.UserRepository;

import java.util.List;
import java.util.Set;
import org.example.gestionrh.tcproject.Services.UserPresenceService;
import org.example.gestionrh.tcproject.Services.CloudinaryService;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final UserRepository userRepository;
    private final UserPresenceService userPresenceService;
    private final CloudinaryService cloudinaryService;

    @PostMapping("/api/chat/upload")
    public ResponseEntity<?> uploadAttachment(@RequestParam("file") MultipartFile file) {
        try {
            Map result = cloudinaryService.uploadFile(file);
            return ResponseEntity.ok(Map.of(
                "url", result.get("url").toString(),
                "name", file.getOriginalFilename(),
                "type", file.getContentType().startsWith("image/") ? "image" : "raw"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Erreur d'upload : " + e.getMessage()));
        }
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDto chatMessage, java.security.Principal principal) {
        System.out.println("Received message payload: " + chatMessage);
        if (principal == null) {
            System.out.println("Error: Principal is null. User is not authenticated.");
            return;
        }
        
        System.out.println("Principal name: " + principal.getName());
        
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            User sender = (User) auth.getPrincipal();
            chatMessage.setSenderId(sender.getId());
            
            System.out.println("Saving message from sender ID: " + sender.getId() + " to receiver ID: " + chatMessage.getReceiverId());
            ChatMessageDto savedMessage = chatMessageService.saveMessage(chatMessage);
            System.out.println("Saved message: " + savedMessage);
            
            // Si c'est un message de groupe (canal)
            if (savedMessage.getChannelId() != null) {
                messagingTemplate.convertAndSend("/topic/" + savedMessage.getChannelId(), savedMessage);
            } 
            // Si c'est un message privé (DM)
            else if (savedMessage.getReceiverId() != null) {
                userRepository.findById(savedMessage.getReceiverId()).ifPresent(receiver -> {
                    System.out.println("Sending to receiver email: " + receiver.getEmail());
                    messagingTemplate.convertAndSendToUser(
                            receiver.getEmail(), 
                            "/queue/messages", 
                            savedMessage
                    );
                });
                
                System.out.println("Sending back to sender email: " + sender.getEmail());
                messagingTemplate.convertAndSendToUser(
                        sender.getEmail(), 
                        "/queue/messages", 
                        savedMessage
                );
            }
        } else {
            System.out.println("Principal is not UsernamePasswordAuthenticationToken");
        }
    }

    // ---- Edit Message ----
    @MessageMapping("/chat.editMessage")
    public void editMessage(@Payload Map<String, Object> payload, java.security.Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            User sender = (User) auth.getPrincipal();
            Long messageId = Long.valueOf(payload.get("messageId").toString());
            String newContent = payload.get("content").toString();

            ChatMessageDto editedMessage = chatMessageService.editMessage(messageId, newContent, sender.getId());

            // Broadcast the edit to both parties
            if (editedMessage.getChannelId() != null) {
                messagingTemplate.convertAndSend("/topic/" + editedMessage.getChannelId(), editedMessage);
            } else if (editedMessage.getReceiverId() != null) {
                userRepository.findById(editedMessage.getReceiverId()).ifPresent(receiver ->
                    messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/messages", editedMessage)
                );
                messagingTemplate.convertAndSendToUser(sender.getEmail(), "/queue/messages", editedMessage);
            }
        }
    }

    // ---- Mark as Read ----
    @MessageMapping("/chat.markAsRead")
    public void markAsRead(@Payload Map<String, Object> payload, java.security.Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            User reader = (User) auth.getPrincipal();
            Long senderId = Long.valueOf(payload.get("senderId").toString());

            chatMessageService.markMessagesAsRead(senderId, reader.getId());

            // Notify the sender that their messages have been read
            userRepository.findById(senderId).ifPresent(sender -> {
                messagingTemplate.convertAndSendToUser(
                    sender.getEmail(),
                    "/queue/read-receipts",
                    Map.of(
                        "readBy", reader.getId(), 
                        "senderId", senderId,
                        "readAt", java.time.LocalDateTime.now().toString()
                    )
                );
            });
        }
    }

    // ---- React to Message ----
    @MessageMapping("/chat.react")
    public void reactToMessage(@Payload Map<String, Object> payload, java.security.Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            User user = (User) auth.getPrincipal();
            Long messageId = Long.valueOf(payload.get("messageId").toString());
            String emoji = payload.get("emoji").toString();

            ChatMessageDto updatedMessage = chatMessageService.toggleReaction(messageId, user.getId(), emoji);

            // Broadcast updated message to both parties
            if (updatedMessage.getChannelId() != null) {
                messagingTemplate.convertAndSend("/topic/" + updatedMessage.getChannelId(), updatedMessage);
            } else if (updatedMessage.getReceiverId() != null) {
                userRepository.findById(updatedMessage.getReceiverId()).ifPresent(receiver ->
                    messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/messages", updatedMessage)
                );
                // Also send to the sender of the original message
                userRepository.findById(updatedMessage.getSenderId()).ifPresent(origSender ->
                    messagingTemplate.convertAndSendToUser(origSender.getEmail(), "/queue/messages", updatedMessage)
                );
            }
        }
    }

    @GetMapping("/api/chat/channel/{channelId}")
    public ResponseEntity<List<ChatMessageDto>> getChannelMessages(@PathVariable String channelId) {
        return ResponseEntity.ok(chatMessageService.getChannelMessages(channelId));
    }

    @GetMapping("/api/chat/direct/{userId1}/{userId2}")
    public ResponseEntity<List<ChatMessageDto>> getDirectMessages(@PathVariable Long userId1, @PathVariable Long userId2) {
        return ResponseEntity.ok(chatMessageService.getDirectMessages(userId1, userId2));
    }

    @GetMapping("/api/chat/presence")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(userPresenceService.getOnlineUsers());
    }
}
