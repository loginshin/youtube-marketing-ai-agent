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
        }
    }
}
