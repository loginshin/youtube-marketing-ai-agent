ALTER TABLE comment_prompt ADD COLUMN IF NOT EXISTS model_tier VARCHAR(16)
    DEFAULT 'STANDARD' NOT NULL;
ALTER TABLE comment_prompt DROP CONSTRAINT IF EXISTS ck_comment_prompt_model;
ALTER TABLE comment_prompt ADD CONSTRAINT ck_comment_prompt_model
    CHECK (model_tier IN ('PREMIUM', 'STANDARD', 'ECONOMY', 'ASTRA', 'SOL'));

-- Upgrade only the original, untouched seed. A settings save increments version.
UPDATE comment_prompt
SET instructions = '선택한 영상의 제목과 확인된 근거를 읽고, 영상 속 인물이 연애에서 무엇을 고민하는지 먼저 짚으세요. 그 고민이 드러나는 구체적인 장면이나 발언을 하나 이상 골라, 어느 대목이 왜 마음에 걸리는지 시청자의 자연스러운 한국어 말투로 표현하세요.
인기 댓글은 시청자의 반응으로만 참고하세요. 댓글의 추측을 영상 속 사실로 옮기거나, 근거에 없는 대사와 개인 경험을 만들어 내지 마세요.
그 장면에서 생긴 궁금증을 운명원에서 연애 사주를 본다면 묻고 싶은 점으로 연결하세요. 브랜드명은 한 번만 쓰고, 상담을 받은 후기나 적중률·효과를 지어내지 마세요. 누구에게나 붙일 수 있는 감탄사, 방문 권유, 매번 같은 마무리는 피하세요.
영상 제목을 다른 제목으로 바꿔도 그대로 통할 문장이면 근거를 다시 골라 쓰세요. 고민 요약과 작성 과정은 출력하지 말고, 링크와 해시태그 없이 공백 포함 40~120자의 댓글 본문 하나만 반환하세요.',
    version = version + 1
WHERE id = 1 AND version = 0
  AND instructions = '근거의 구체적인 내용을 반영하고 운명원 연애 사주에 대한 관심을 자연스럽게 표현하세요.';

-- Replace the former default paragraph while preserving other edits and the selected model.
UPDATE comment_prompt
SET instructions = REPLACE(instructions,
    '그 장면에서 생긴 궁금증을 운명원에서 연애 사주를 본다면 묻고 싶은 점으로 연결하세요. 브랜드명은 한 번만 쓰고, 상담을 받은 후기나 적중률·효과를 지어내지 마세요. 누구에게나 붙일 수 있는 감탄사, 방문 권유, 매번 같은 마무리는 피하세요.',
    '영상 속 한 대목에 공감한 이유부터 짚으세요. 작성자가 본인 경험으로 제공한 운명원 상담 내용이 있으면, 실제로 받은 피드백 한 가지와 그 말이 당시 고민을 이해하는 데 어떻게 도움이 됐는지를 담백한 1인칭으로 풀어내세요. 제공된 경험이 없으면 영상에서 느낀 점을 중심으로 쓰고, 운명원 연애 사주는 맥락에 맞을 때만 한 번 언급하세요. 매번 상담받고 싶다는 말이나 방문 권유로 끝내지 마세요.'),
    version = version + 1
WHERE id = 1 AND instructions LIKE '%그 장면에서 생긴 궁금증을 운명원에서 연애 사주를 본다면 묻고 싶은 점으로 연결하세요. 브랜드명은 한 번만 쓰고, 상담을 받은 후기나 적중률·효과를 지어내지 마세요. 누구에게나 붙일 수 있는 감탄사, 방문 권유, 매번 같은 마무리는 피하세요.%'
  AND LENGTH(instructions) + 85 <= 4000;
