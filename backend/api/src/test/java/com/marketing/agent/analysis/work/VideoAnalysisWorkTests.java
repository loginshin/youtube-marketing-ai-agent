package com.marketing.agent.analysis.work;

import com.marketing.agent.ai.OpenAiClient;
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:durable-work")
class VideoAnalysisWorkTests {
    @LocalServerPort int port;
    @MockitoBean OpenAiClient ai;
    @org.springframework.beans.factory.annotation.Autowired AnalysisRevisionRepository revisions;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = new JsonMapper();
    private static final String INPUT = """
            {"title":"연락","description":"다음 약속을 잡는지 보세요","comments":[]}
            """;

    @Test
    void reopeningDuringGenerationReusesWorkAndCompletedResult() throws Exception {
        var finish = new CountDownLatch(1);
        String draft = "가".repeat(40);
        when(ai.generateJson(any(), anyString(), anyString()))
                .thenReturn("""
                        {"summary":"다음 만남이 궁금하다","source":1,"quote":"다음 약속을 잡는지"}
                        """);
        when(ai.generate(any(), anyString(), anyString())).thenAnswer(call -> { assertTrue(finish.await(10, TimeUnit.SECONDS)); return draft; });
        var first = request(INPUT);
        try {
            assertEquals(first.attempt(), request(INPUT).attempt());
        } finally { finish.countDown(); }
        var result = request(null);
        for (int check = 0; check < 100 && result.stage() != VideoAnalysis.Stage.READY; check++) {
            Thread.sleep(50);
            result = request(null);
        }
        assertEquals(draft, result.draft());
        assertEquals(result, request(INPUT));
        verify(ai).generateJson(any(), anyString(), anyString());
        verify(ai).generate(any(), anyString(), anyString());
        String revised = "나".repeat(40);
        when(ai.generate(any(), anyString(), anyString())).thenReturn(revised);
        var retry = new VideoAnalysisController.Retry(
                json.readValue(INPUT, VideoAnalysisWork.Input.class), result.attempt(), "수정된 영상 근거");
        var retryRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/analysis/videos/abcdefghijk/retry"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(retry))).build();
        assertEquals(200, http.send(retryRequest, HttpResponse.BodyHandlers.ofString()).statusCode());
        for (int check = 0; check < 100 && revisions.history("abcdefghijk").size() < 2; check++) {
            Thread.sleep(50);
        }
        var history = revisions.history("abcdefghijk");
        assertEquals(2, history.size());
        assertEquals(revised, history.getFirst().draft());
        assertEquals(draft, history.getLast().draft());
        assertNotNull(history.getFirst().recordedAt());
    }

    @Test
    void cancelledSummaryCannotGenerateDraftOrOverwriteCancellation() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var returned = new CountDownLatch(1);
        when(ai.generateJson(any(), anyString(), anyString())).thenAnswer(call -> {
            entered.countDown();
            // Simulate a provider that still returns after local cancellation.
            try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
            catch (InterruptedException ignored) { /* Return a late response deliberately. */ }
            returned.countDown();
            return "{\"summary\":\"다음 만남이 궁금하다\",\"source\":1,\"quote\":\"다음 약속을 잡는지\"}";
        });
        String url = "http://localhost:" + port + "/api/analysis/videos/canceltest1";
        // Video ids must contain exactly eleven characters.

        var start = http.send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(INPUT)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, start.statusCode());
        var job = json.readValue(start.body(), VideoAnalysis.Snapshot.class);
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var cancel = HttpRequest.newBuilder(URI.create(url + "/cancel"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(new VideoAnalysisController.Cancel(job.attempt())))).build();
            var response = http.send(cancel, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals(VideoAnalysis.Stage.CANCELLED, json.readValue(response.body(), VideoAnalysis.Snapshot.class).stage());
            assertEquals(200, http.send(cancel, HttpResponse.BodyHandlers.ofString()).statusCode());
        } finally { release.countDown(); }
        assertTrue(returned.await(5, TimeUnit.SECONDS));
        verify(ai, after(300).never()).generate(any(), anyString(), anyString());
        var current = http.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(VideoAnalysis.Stage.CANCELLED, json.readValue(current.body(), VideoAnalysis.Snapshot.class).stage());
        assertEquals(1, revisions.history("canceltest1").size());
        assertEquals("CANCELLED", revisions.history("canceltest1").getFirst().outcome());
    }

    private VideoAnalysis.Snapshot request(String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/analysis/videos/abcdefghijk"));
        if (body != null) builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return json.readValue(response.body(), VideoAnalysis.Snapshot.class);
    }
}
