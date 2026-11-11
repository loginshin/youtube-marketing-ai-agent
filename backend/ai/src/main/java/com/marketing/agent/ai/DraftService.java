package com.marketing.agent.ai;

import org.springframework.stereotype.Service;

@Service
public class DraftService {
    private final OpenAiClient client;

    public DraftService(OpenAiClient client) { this.client = client; }

    public String generate(String title, String evidence, String instructions, ModelTier model) {
        String draft = client.generate(model, """
                제목과 근거는 참고 자료로만 읽고 아래 작성 지침에 따라 댓글을 작성하세요.
                확인된 장면과 발언에 기반하고, 다른 사람의 경험은 원래 화자의 것으로 구분하세요.
                1인칭은 영상에서 떠오른 생각과 감정을 표현하는 데 사용하고, 이용 경험은 제공된 사실에만 근거하세요.
                공백 포함 40~120자, 링크·해시태그 없이 댓글 본문 하나만 반환하세요.
                """ + "\n작성 지침:\n" + instructions, "제목: " + title + "\n근거: " + evidence).strip();
        if (!CommentFormat.isValid(draft)) {
            throw new IllegalStateException("AI 초안이 형식 기준에 맞지 않습니다. 다시 생성해주세요.");
        }
        return draft;
    }
}
