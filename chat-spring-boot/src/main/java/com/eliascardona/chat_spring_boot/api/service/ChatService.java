package com.eliascardona.chat_spring_boot.api.service;

import com.eliascardona.chat_spring_boot.api.command.NewChatMessageCommand;
import com.eliascardona.chat_spring_boot.api.dto.ChatDto;
import com.eliascardona.chat_spring_boot.api.entity.ChatMessage;
import com.eliascardona.chat_spring_boot.api.repository.ChatRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ChatService {
    private final ChatRepository chatRepository;

    public ChatService(
        ChatRepository chatRepository
    ) {
        this.chatRepository = chatRepository;
    }

    @Transactional
    public ChatDto.SavedMessageResponse saveChatMessage(
        NewChatMessageCommand command
    ) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessageContent(command.message());
        String sender = (command.sender() != null && !command.sender().isBlank())
                ? command.sender()
                : "Anónimo";
        chatMessage.setSender(sender);
        ChatMessage newChatMessage = chatRepository.save(chatMessage);

        return ChatDto.SavedMessageResponse.builder()
                .chatMessageId(newChatMessage.getId())
                .messageContent(newChatMessage.getMessageContent())
                .sender(newChatMessage.getSender())
                .build();
    }

    @Transactional(readOnly = true)
    public ChatDto.VerifiedMessageResponse findChatMessageById(
            UUID chatMessageId
    ) {
        ChatMessage verifiedChatMessage = chatRepository
                .findById(chatMessageId)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Message: " + chatMessageId + " was not found"));

        return ChatDto.VerifiedMessageResponse.builder()
                .chatMessageId(verifiedChatMessage.getId())
                .messageContent(verifiedChatMessage.getMessageContent())
                .sender(verifiedChatMessage.getSender())
                .build();
    }
}