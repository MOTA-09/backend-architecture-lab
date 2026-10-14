package com.eliascardona.chat_spring_boot.api.service;

import com.eliascardona.chat_spring_boot.api.command.NewChatMessageCommand;
import com.eliascardona.chat_spring_boot.api.dto.ChatDto;
import com.eliascardona.chat_spring_boot.api.entity.ChatMessage;
import com.eliascardona.chat_spring_boot.api.repository.ChatRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("saveChatMessage debe persistir y retornar el autor (sender) correctamente")
    void saveChatMessage_withSender_persistsAndReturnsSender() {
        // Arrange
        UUID generatedId = UUID.randomUUID();
        NewChatMessageCommand command = new NewChatMessageCommand("Fulanito", "Fulanita", "Hola mundo");

        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage entity = invocation.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });

        // Act
        ChatDto.SavedMessageResponse response = chatService.saveChatMessage(command);

        // Assert
        assertNotNull(response);
        assertEquals(generatedId, response.getChatMessageId());
        assertEquals("Hola mundo", response.getMessageContent());
        assertEquals("Fulanito", response.getSender());

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatRepository, times(1)).save(captor.capture());
        ChatMessage capturedEntity = captor.getValue();
        assertEquals("Fulanito", capturedEntity.getSender());
        assertEquals("Hola mundo", capturedEntity.getMessageContent());
    }

    @Test
    @DisplayName("saveChatMessage con autor nulo o vacío debe asignar 'Anónimo' por defecto")
    void saveChatMessage_withNullOrBlankSender_defaultsToAnonimo() {
        // Arrange
        UUID generatedId = UUID.randomUUID();
        NewChatMessageCommand command = new NewChatMessageCommand(null, null, "Mensaje sin autor");

        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage entity = invocation.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });

        // Act
        ChatDto.SavedMessageResponse response = chatService.saveChatMessage(command);

        // Assert
        assertNotNull(response);
        assertEquals("Anónimo", response.getSender());

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatRepository, times(1)).save(captor.capture());
        assertEquals("Anónimo", captor.getValue().getSender());
    }

    @Test
    @DisplayName("findChatMessageById debe retornar el mensaje verificado con su autor (sender)")
    void findChatMessageById_existingMessage_returnsVerifiedResponseWithSender() {
        // Arrange
        UUID messageId = UUID.randomUUID();
        ChatMessage existingMessage = new ChatMessage();
        existingMessage.setId(messageId);
        existingMessage.setMessageContent("Mensaje de prueba");
        existingMessage.setSender("Fulanita");

        when(chatRepository.findById(messageId)).thenReturn(Optional.of(existingMessage));

        // Act
        ChatDto.VerifiedMessageResponse response = chatService.findChatMessageById(messageId);

        // Assert
        assertNotNull(response);
        assertEquals(messageId, response.getChatMessageId());
        assertEquals("Mensaje de prueba", response.getMessageContent());
        assertEquals("Fulanita", response.getSender());
        verify(chatRepository, times(1)).findById(messageId);
    }

    @Test
    @DisplayName("findChatMessageById debe lanzar EntityNotFoundException si el mensaje no existe")
    void findChatMessageById_notFound_throwsEntityNotFoundException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(chatRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> chatService.findChatMessageById(nonExistentId)
        );

        assertTrue(exception.getMessage().contains(nonExistentId.toString()));
        verify(chatRepository, times(1)).findById(nonExistentId);
    }
}

