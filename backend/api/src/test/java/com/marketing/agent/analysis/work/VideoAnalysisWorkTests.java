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
        when(ai.generate(any(), anyString(), anyString()))
                .thenReturn("""
                        {"summary":"다음 만남이 궁금하다","source":1,"quote":"다음 약속을 잡는지"}
                        """)
                .thenAnswer(call -> { assertTrue(finish.await(10, TimeUnit.SECONDS)); return draft; });
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
        verify(ai, times(2)).generate(any(), anyString(), anyString());
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

    private VideoAnalysis.Snapshot request(String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/analysis/videos/abcdefghijk"));
        if (body != null) builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return json.readValue(response.body(), VideoAnalysis.Snapshot.class);
    }
}
