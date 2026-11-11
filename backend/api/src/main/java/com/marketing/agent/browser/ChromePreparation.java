package com.marketing.agent.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
class ChromePreparation {
    private final ExecutorService browserThread = Executors.newSingleThreadExecutor();
    private final AtomicBoolean checking = new AtomicBoolean();
    private Playwright playwright;
    private BrowserContext context;
    private Page page;

    record Result(String status, String message) {}

    CompletableFuture<String> publish(String videoId, String body, Runnable reserve) {
        if (!checking.compareAndSet(false, true))
            throw new IllegalStateException("다른 Chrome 작업이 진행 중입니다.");
        return CompletableFuture.supplyAsync(() -> {
            try {
                Result ready = inspect(videoId);
                if (!"READY".equals(ready.status())) throw new IllegalStateException(ready.message());
                return submitApproved(page, videoId, body, reserve);
            } finally { checking.set(false); }
        }, browserThread);
    }

    CompletableFuture<Result> prepare(String videoId) {
        if (!checking.compareAndSet(false, true))
            return CompletableFuture.completedFuture(new Result("BUSY", "다른 영상의 입력창을 확인하고 있습니다."));
        return CompletableFuture.supplyAsync(() -> {
            try {
                return inspect(videoId);
            } catch (PlaywrightException exception) {
                return new Result("UNAVAILABLE", "Chrome 설치와 인터넷 연결을 확인하세요. 전용 프로필이 다른 Chrome 창에서 열려 있다면 닫고 다시 확인하세요.");
            } finally {
                checking.set(false);
            }
        }, browserThread);
    }

    static String submitApproved(Page page, String videoId, String body, Runnable reserve) {
        var composer = page.locator("ytd-comment-simplebox-renderer:visible").first();
        var editor = composer.locator("[contenteditable='true']:visible").first();
        editor.fill("");
        for (var character : body.codePoints().toArray()) {
            page.keyboard().insertText(Character.toString(character));
            page.waitForTimeout(25);
        }
        var location = java.net.URI.create(page.url());
        boolean target = "www.youtube.com".equals(location.getHost()) &&
                (("/shorts/" + videoId).equals(location.getPath()) ||
                ("/watch".equals(location.getPath()) && location.getQuery() != null &&
                        java.util.Arrays.asList(location.getQuery().split("&")).contains("v=" + videoId)));
        if (!target || !editor.innerText().strip().equals(body))
            throw new IllegalStateException("영상 또는 입력한 본문이 달라 게시를 중단했습니다.");
        var submit = composer.locator("#submit-button button:visible").first();
        if (!submit.isEnabled()) throw new IllegalStateException("게시 버튼이 활성화되지 않았습니다.");
        reserve.run();
        var response = page.waitForResponse(r -> r.url().startsWith("https://www.youtube.com/youtubei/v1/comment/create_comment?")
                        || r.url().equals("https://www.youtube.com/youtubei/v1/comment/create_comment"),
                () -> submit.click());
        if (!response.ok()) return null;
        var json = new tools.jackson.databind.json.JsonMapper().readTree(response.body());
        var ids = json.findValuesAsString("commentId").stream().distinct().toList();
        return ids.size() == 1 && ids.getFirst().matches("[A-Za-z0-9_-]{1,256}") ? ids.getFirst() : null;
    }

    private Result inspect(String videoId) {
        if (playwright == null) playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        if (context == null) {
            Path profile = Path.of(System.getProperty("user.home"), ".clipdesk", "chrome-profiles", "youtube-1");
            context = playwright.chromium().launchPersistentContext(profile,
                    new BrowserType.LaunchPersistentContextOptions().setChannel("chrome")
                            .setHeadless(false).setLocale("ko-KR").setTimeout(15000));
            context.onClose(closed -> { if (closed == context) context = null; });
            page = null;
        }
        if (page == null || page.isClosed())
            page = context.pages().isEmpty() ? context.newPage() : context.pages().getFirst();
        page.bringToFront();
        page.setDefaultTimeout(8000);
        page.navigate("https://www.youtube.com/watch?v=" + videoId + "&hl=ko",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(20000));
        return inspectControls(page);
    }

    static Result inspectControls(Page page) {
        try {
            page.waitForSelector("ytd-masthead #avatar-btn:visible, ytd-masthead a[href*='accounts.google.com']:visible, ytd-masthead a[href*='/signin']:visible");
        } catch (TimeoutError timeout) {
            return new Result("ACCOUNT_UNKNOWN", "로그인 상태를 확인할 수 없습니다. 열린 Chrome에서 동의 화면이나 영상 접근 상태를 확인하세요.");
        }
        if (!page.locator("ytd-masthead #avatar-btn:visible").first().isVisible())
            return new Result("LOGIN_REQUIRED", "유튜브 1 전용 Chrome에서 로그인한 뒤 다시 확인하세요.");
        try {
            if (page.url().contains("/shorts/")) {
                var comments = page.locator("ytd-reel-video-renderer[is-active] button[aria-label*='댓글']").first();
                comments.click();
            } else {
                page.locator("ytd-comments#comments").first().scrollIntoViewIfNeeded();
            }
            var composer = page.locator("ytd-comment-simplebox-renderer:visible").first();
            var editor = composer.locator("[contenteditable='true']:visible").first();
            if (!editor.isVisible()) composer.locator("#placeholder-area, #simplebox-placeholder").first().click();
            editor.waitFor();
            if (editor.isEditable()) return new Result("READY", "유튜브 1 프로필에서 로그인과 댓글 입력창을 확인했습니다.");
        } catch (TimeoutError timeout) {
            // Restricted videos and disabled comments may never render a composer.
        }
        return new Result("EDITOR_UNAVAILABLE", "로그인은 확인했습니다. 댓글 사용 가능 여부와 열린 영상 화면을 확인하세요.");
    }

    @PreDestroy void close() {
        browserThread.execute(() -> { if (playwright != null) playwright.close(); });
        browserThread.shutdown();
    }
}
