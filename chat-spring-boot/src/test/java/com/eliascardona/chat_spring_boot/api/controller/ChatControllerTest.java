package com.eliascardona.chat_spring_boot.api.controller;

import com.eliascardona.chat_spring_boot.api.command.NewChatMessageCommand;
import com.eliascardona.chat_spring_boot.api.dto.ChatDto;
import com.eliascardona.chat_spring_boot.api.service.ChatService;
import com.eliascardona.chat_spring_boot.core.exceptions.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /save-message con cuerpo válido debe retornar HTTP 200 y JSON con sender")
    void saveMessage_withValidPayload_returns200() throws Exception {
        UUID messageId = UUID.randomUUID();
        ChatDto.SavedMessageResponse responseDto = ChatDto.SavedMessageResponse.builder()
                .chatMessageId(messageId)
                .messageContent("Mensaje de prueba")
                .sender("Fulanito")
                .build();

        when(chatService.saveChatMessage(any(NewChatMessageCommand.class))).thenReturn(responseDto);

        NewChatMessageCommand command = new NewChatMessageCommand("Fulanito", null, "Mensaje de prueba");

        mockMvc.perform(post("/api/chat/save-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatMessageId").value(messageId.toString()))
                .andExpect(jsonPath("$.messageContent").value("Mensaje de prueba"))
                .andExpect(jsonPath("$.sender").value("Fulanito"));
    }

    @Test
    @DisplayName("POST /save-message con mensaje vacío debe retornar HTTP 400 VALIDATION_ERROR")
    void saveMessage_withBlankMessage_returns400ValidationError() throws Exception {
        NewChatMessageCommand command = new NewChatMessageCommand("Fulanito", null, "   ");

        mockMvc.perform(post("/api/chat/save-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.message").exists());
    }

    @Test
    @DisplayName("POST /save-message con mensaje > 1000 caracteres debe retornar HTTP 400 VALIDATION_ERROR")
    void saveMessage_withMessageOver1000Chars_returns400ValidationError() throws Exception {
        String longMessage = "a".repeat(1001);
        NewChatMessageCommand command = new NewChatMessageCommand("Fulanito", null, longMessage);

        mockMvc.perform(post("/api/chat/save-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.message").value("El mensaje no puede exceder los 1000 caracteres"));
    }

    @Test
    @DisplayName("GET /verify-message con UUID válido existente debe retornar HTTP 200")
    void verifyMessage_withValidUuid_returns200() throws Exception {
        UUID messageId = UUID.randomUUID();
        ChatDto.VerifiedMessageResponse responseDto = ChatDto.VerifiedMessageResponse.builder()
                .chatMessageId(messageId)
                .messageContent("Contenido verificado")
                .sender("Fulanita")
                .build();

        when(chatService.findChatMessageById(messageId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/chat/verify-message")
                        .param("chatMessageId", messageId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatMessageId").value(messageId.toString()))
                .andExpect(jsonPath("$.messageContent").value("Contenido verificado"))
                .andExpect(jsonPath("$.sender").value("Fulanita"));
    }

    @Test
    @DisplayName("GET /verify-message con formato UUID inválido debe retornar HTTP 400 INVALID_PARAMETER")
    void verifyMessage_withInvalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/chat/verify-message")
                        .param("chatMessageId", "uuid-no-valido-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.details.chatMessageId").exists());
    }

    @Test
    @DisplayName("GET /verify-message con mensaje no existente debe retornar HTTP 404 ENTITY_NOT_FOUND")
    void verifyMessage_withNonExistentId_returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(chatService.findChatMessageById(nonExistentId))
                .thenThrow(new EntityNotFoundException("Message: " + nonExistentId + " was not found"));

        mockMvc.perform(get("/api/chat/verify-message")
                        .param("chatMessageId", nonExistentId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("ENTITY_NOT_FOUND"));
    }
}

