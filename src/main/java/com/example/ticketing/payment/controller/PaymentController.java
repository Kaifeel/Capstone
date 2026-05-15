package com.example.ticketing.payment.controller;

import com.example.ticketing.common.response.ApiResponse;
import com.example.ticketing.payment.dto.MockPaymentRequest;
import com.example.ticketing.payment.dto.MockPaymentResponse;
import com.example.ticketing.payment.service.PaymentService;
import com.example.ticketing.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/mock")
    public ApiResponse<MockPaymentResponse> mockPay(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MockPaymentRequest request
    ) {
        MockPaymentResponse response = paymentService.mockPay(userDetails.getUserId(), request);
        String message = response.paymentStatus().name().equals("SUCCESS")
                ? "결제가 완료되었습니다."
                : "결제가 실패했습니다.";
        return ApiResponse.success(message, response);
    }
}
