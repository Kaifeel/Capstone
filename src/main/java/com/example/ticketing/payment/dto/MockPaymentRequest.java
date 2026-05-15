package com.example.ticketing.payment.dto;

import jakarta.validation.constraints.NotNull;

public record MockPaymentRequest(
        @NotNull(message = "예매 ID는 필수입니다.")
        Long reservationId,

        @NotNull(message = "결제 결과는 필수입니다.")
        MockPaymentResult result
) {
}
