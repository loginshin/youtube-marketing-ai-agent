package com.marketing.agent.browser;

import com.microsoft.playwright.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "TEST_LOCAL_CHROME", matches = "true")
class ChromePreparationTests {
    @Test
    void checksAccountAndOpensEditorWithoutFillingIt(@TempDir Path profile) {
        try (var driver = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")))) {
            var context = driver.chromium().launchPersistentContext(profile,
                    new BrowserType.LaunchPersistentContextOptions().setChannel("chrome").setHeadless(true));
            var page = context.newPage();
            page.setDefaultTimeout(1500);
            page.setContent("<ytd-masthead><a href='/signin'>로그인</a></ytd-masthead>");
            assertEquals("LOGIN_REQUIRED", ChromePreparation.inspectControls(page).status());
            page.setContent("""
                    <ytd-masthead><button id="avatar-btn">프로필</button></ytd-masthead>
                    <ytd-comments id="comments" style="display:block">
                      <ytd-comment-simplebox-renderer style="display:block">
                        <button id="placeholder-area" onclick="this.nextElementSibling.hidden=false">댓글 추가</button>
                        <div style="min-height:20px" contenteditable="true" hidden></div>
                      </ytd-comment-simplebox-renderer>
                    </ytd-comments>
                    """);
            assertEquals("READY", ChromePreparation.inspectControls(page).status());
            assertEquals("", page.locator("[contenteditable]").innerText());
            // Intercept every URL: no request reaches YouTube or a real account.
            page.route("**/*", route -> {
                if (route.request().url().contains("/create_comment"))
                    route.fulfill(new Route.FulfillOptions().setContentType("application/json")
                            .setBody("{\"commentId\":\"fixtureId\"}"));
                else route.fulfill(new Route.FulfillOptions().setContentType("text/html; charset=utf-8").setBody("""
                        <ytd-comment-simplebox-renderer style="display:block">
                          <div contenteditable="true" style="min-height:20px"></div>
                          <div id="submit-button"><button onclick="fetch('/youtubei/v1/comment/create_comment?test=1', {method:'POST'})">게시</button></div>
                        </ytd-comment-simplebox-renderer>
                        """));
            });
            page.navigate("https://www.youtube.com/watch?v=abcdefghijk");
            var reservations = new java.util.concurrent.atomic.AtomicInteger();
            assertEquals("fixtureId", ChromePreparation.submitApproved(page, "abcdefghijk", "확인한 댓글입니다.",
                    reservations::incrementAndGet));
            assertEquals("확인한 댓글입니다.", page.locator("[contenteditable]").innerText());
            assertEquals(1, reservations.get());
        }
    }
}
