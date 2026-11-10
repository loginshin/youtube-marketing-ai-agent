package com.marketing.agent.video.presentation;

import com.marketing.agent.video.application.VideoNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice(assignableTypes = VideoController.class)
class VideoExceptionHandler {
    @ExceptionHandler(VideoNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(VideoNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorResponse> missingKey(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ErrorResponse> upstreamFailure(RestClientException exception) {
        if (exception instanceof RestClientResponseException response
                && (response.getStatusCode().value() == 403 || response.getStatusCode().value() == 429)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                    "YouTube 요청이 제한되었습니다. 키·API 활성화·키 제한·할당량을 확인하세요."));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(
                "YouTube 조회에 실패했습니다. 네트워크와 API 설정을 확인하고 다시 시도하세요."));
    }

    record ErrorResponse(String message) {}
}
