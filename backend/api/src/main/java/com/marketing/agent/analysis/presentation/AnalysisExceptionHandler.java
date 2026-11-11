package com.marketing.agent.analysis.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AnalysisController.class)
class AnalysisExceptionHandler {
    private final JsonMapper json;

    AnalysisExceptionHandler(JsonMapper json) { this.json = json; }

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<ErrorResponse> upstreamResponse(RestClientResponseException exception) {
        String message = switch (exception.getStatusCode().value()) {
            case 429 -> quotaMessage(exception);
            case 401 -> "OpenAI가 API 키 인증을 거부했습니다. 백엔드의 OPENAI_API_KEY를 확인하세요.";
            case 403, 404 -> "선택한 OpenAI 모델과 프로젝트의 접근 권한을 확인하세요.";
            default -> "OpenAI가 생성 요청을 처리하지 못했습니다.";
        };
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(message));
    }

    private String quotaMessage(RestClientResponseException exception) {
        try {
            var error = json.readTree(exception.getResponseBodyAsByteArray()).path("error");
            if ("credit_balance_exhausted".equals(error.path("code").asString("")))
                return "OpenAI API 크레딧이 소진되었습니다. 결제 설정에서 잔액을 충전한 뒤 다시 생성하세요.";
            if ("insufficient_quota".equals(error.path("type").asString("")) ||
                    "insufficient_quota".equals(error.path("code").asString("")))
                return "OpenAI API 사용 한도가 부족합니다. 결제 설정과 사용 한도를 확인하세요.";
        } catch (tools.jackson.core.JacksonException ignored) {
            // An upstream proxy can return a non-JSON error page.
        }
        return "OpenAI 요청 한도에 도달했습니다. 잠시 후 다시 시도하세요.";
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorResponse> unavailable(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ErrorResponse> upstreamFailure(RestClientException exception) {
        // Never return upstream response bodies, credentials or request headers.
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(
                "AI 요청에 실패했습니다. API 키·사용 한도·네트워크를 확인하고 다시 시도해주세요."));
    }

    record ErrorResponse(String message) {}
}
