package com.eliascardona.chat_spring_boot.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewChatMessageCommand (
    @Size(max = 100, message = "El remitente no puede exceder los 100 caracteres")
    String sender,

    @Size(max = 100, message = "El destinatario no puede exceder los 100 caracteres")
    String recipient,

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 1000, message = "El mensaje no puede exceder los 1000 caracteres")
    String message
) {}
