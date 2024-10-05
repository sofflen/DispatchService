package com.study.dispatchservice.messages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDispatchedEvent {
    private UUID orderId;
    private UUID processedById;
    private String notes;
}
