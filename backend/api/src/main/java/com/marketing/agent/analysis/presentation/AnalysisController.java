package com.marketing.agent.analysis.presentation;

import com.marketing.agent.ai.DraftService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {
    private final DraftService drafts;

    public AnalysisController(DraftService drafts) { this.drafts = drafts; }

    @PostMapping("/draft")
    public DraftResponse generate(@RequestBody DraftRequest request) {
        if (request.title() == null || request.title().isBlank() || request.title().length() > 300
                || request.evidence() == null || request.evidence().isBlank()
                || request.evidence().length() > 10000) {
            throw new IllegalArgumentException("제목(1~300자)과 확인 가능한 근거(1~10,000자)가 필요합니다.");
        }
        return new DraftResponse(drafts.generate(request.title(), request.evidence()));
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

    public record DraftRequest(String title, String evidence) {}
    public record DraftResponse(String draft) {}
    public record ErrorResponse(String message) {}
}
