package com.eliascardona.chat_spring_boot.api.controller;

import com.eliascardona.chat_spring_boot.api.command.NewChatMessageCommand;
import com.eliascardona.chat_spring_boot.api.dto.ChatDto;
import com.eliascardona.chat_spring_boot.api.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ChatDto.SavedMessageResponse> saveMessage(
        @Valid @RequestBody NewChatMessageCommand command
    ) {
        ChatDto.SavedMessageResponse response = chatService
                .saveChatMessage(command);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-message")
    public ResponseEntity<ChatDto.VerifiedMessageResponse> verifyMessage(
            @RequestParam UUID chatMessageId
    ) {
        ChatDto.VerifiedMessageResponse response = chatService
                .findChatMessageById(chatMessageId);

        return ResponseEntity.ok(response);
    }
}
