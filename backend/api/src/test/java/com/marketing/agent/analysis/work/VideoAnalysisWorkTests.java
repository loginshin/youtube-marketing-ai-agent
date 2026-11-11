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
    }

    private VideoAnalysis.Snapshot request(String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/analysis/videos/abcdefghijk"));
        if (body != null) builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return json.readValue(response.body(), VideoAnalysis.Snapshot.class);
    }
}
