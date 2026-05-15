package com.example.ticketing.concert.controller;

import com.example.ticketing.common.response.ApiResponse;
import com.example.ticketing.common.response.PageResponse;
import com.example.ticketing.concert.dto.ConcertCreateRequest;
import com.example.ticketing.concert.dto.ConcertDetailResponse;
import com.example.ticketing.concert.dto.ConcertResponse;
import com.example.ticketing.concert.dto.ConcertUpdateRequest;
import com.example.ticketing.concert.service.ConcertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/concerts")
public class ConcertController {

    private final ConcertService concertService;

    @PostMapping
    public ApiResponse<ConcertResponse> create(@Valid @RequestBody ConcertCreateRequest request) {
        return ApiResponse.success("공연이 등록되었습니다.", concertService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ConcertResponse>> getConcerts(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(concertService.getConcerts(pageable));
    }

    @GetMapping("/{concertId}")
    public ApiResponse<ConcertDetailResponse> getConcert(@PathVariable Long concertId) {
        return ApiResponse.success(concertService.getConcert(concertId));
    }

    @PatchMapping("/{concertId}")
    public ApiResponse<ConcertResponse> update(@PathVariable Long concertId, @Valid @RequestBody ConcertUpdateRequest request) {
        return ApiResponse.success("공연이 수정되었습니다.", concertService.update(concertId, request));
    }

    @DeleteMapping("/{concertId}")
    public ApiResponse<Long> delete(@PathVariable Long concertId) {
        return ApiResponse.success("공연이 삭제되었습니다.", concertService.delete(concertId));
    }
}
