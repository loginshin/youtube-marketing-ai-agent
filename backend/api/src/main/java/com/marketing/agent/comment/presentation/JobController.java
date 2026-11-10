package com.marketing.agent.comment.presentation;

import com.marketing.agent.comment.application.ApprovalRejected;
import com.marketing.agent.comment.application.ApprovalService;
import com.marketing.agent.comment.application.JobHistoryService;
import com.marketing.agent.comment.application.JobHistoryService.JobHistory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos/{videoId}/job")
public class JobController {
    private final ApprovalService approvals;
    private final JobHistoryService history;

    public JobController(ApprovalService approvals, JobHistoryService history) {
        this.approvals = approvals;
        this.history = history;
    }

    @GetMapping
    public ResponseEntity<JobHistory> find(@PathVariable String videoId) {
        return history.find(videoId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void approve(@PathVariable String videoId, @RequestBody ApprovalRequest request) {
        approvals.approve(videoId, request.profile(), request.body());
    }

    @ExceptionHandler(ApprovalRejected.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse rejected(ApprovalRejected exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse invalid(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    public record ApprovalRequest(String profile, String body) {}
    public record ErrorResponse(String message) {}
}
