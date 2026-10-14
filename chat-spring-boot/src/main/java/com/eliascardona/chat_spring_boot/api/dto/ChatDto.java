package com.eliascardona.chat_spring_boot.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class ChatDto {
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SavedMessageResponse {
        private UUID chatMessageId;
        private String messageContent;
        private String sender;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VerifiedMessageResponse {
        private UUID chatMessageId;
        private String messageContent;
        private String sender;
    }
}