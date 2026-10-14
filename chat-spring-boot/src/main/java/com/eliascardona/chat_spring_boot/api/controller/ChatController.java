package com.eliascardona.chat_spring_boot.api.controller;

import com.eliascardona.chat_spring_boot.api.command.NewChatMessageCommand;
import com.eliascardona.chat_spring_boot.api.dto.ChatDto;
import com.eliascardona.chat_spring_boot.api.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(
        ChatService chatService
    ) {
        this.chatService = chatService;
    }

    @PostMapping("/save-message")
    public ResponseEntity<?> saveMessage(
        @Validated @RequestBody NewChatMessageCommand command
    ) {
        try {
            ChatDto.SavedMessageResponse response = chatService
                    .saveChatMessage(command);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping("/verify-message")
    public ResponseEntity<?> verifyMessage(
            @Validated @RequestParam String chatMessageId
    ) {
        try {
            UUID messageId = UUID.fromString(chatMessageId);

            ChatDto.VerifiedMessageResponse response = chatService
                    .findChatMessageById(messageId);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}
