package com.marketing.agent.video.application;

public class VideoNotFoundException extends RuntimeException {
    public VideoNotFoundException() {
        super("영상을 찾을 수 없습니다.");
    }
}
