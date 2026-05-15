package com.example.ticketing.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "현재 상태에서 처리할 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    CONCERT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONCERT_NOT_FOUND", "공연을 찾을 수 없습니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "SEAT_NOT_FOUND", "좌석을 찾을 수 없습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "예매를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다."),
    SEAT_ALREADY_OCCUPIED(HttpStatus.CONFLICT, "SEAT_ALREADY_OCCUPIED", "이미 선택할 수 없는 좌석입니다."),
    ACTIVE_RESERVATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "ACTIVE_RESERVATION_ALREADY_EXISTS", "활성 예매가 이미 존재합니다."),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "동시에 같은 좌석을 예매하려는 요청이 발생했습니다."),
    WAITING_ROOM_REQUIRED(HttpStatus.LOCKED, "WAITING_ROOM_REQUIRED", "대기열 통과가 필요합니다."),
    WAITING_TOKEN_NOT_ALLOWED(HttpStatus.LOCKED, "WAITING_TOKEN_NOT_ALLOWED", "아직 입장 가능한 순서가 아닙니다."),
    WAITING_TOKEN_EXPIRED(HttpStatus.GONE, "WAITING_TOKEN_EXPIRED", "대기열 토큰이 만료되었습니다."),
    RESERVATION_EXPIRED(HttpStatus.GONE, "RESERVATION_EXPIRED", "예매 결제 시간이 만료되었습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
