package com.marketing.agent.comment.application;

public class ApprovalRejected extends RuntimeException {
    public ApprovalRejected(String message) {
        super(message);
    }
}
