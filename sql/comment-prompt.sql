ALTER TABLE comment_prompt ADD COLUMN IF NOT EXISTS model_tier VARCHAR(16)
    DEFAULT 'STANDARD' NOT NULL;
ALTER TABLE comment_prompt DROP CONSTRAINT IF EXISTS ck_comment_prompt_model;
ALTER TABLE comment_prompt ADD CONSTRAINT ck_comment_prompt_model
    CHECK (model_tier IN ('PREMIUM', 'STANDARD', 'ECONOMY', 'ASTRA', 'SOL'));

-- Replace the legacy seed/default as a whole; preserve later admin edits and model selection.
-- This predicate no longer matches after the upgrade, so restarts do not overwrite edits.
UPDATE comment_prompt
SET instructions = '당신은 연애 고민에 공감하는 한국어 카피라이터이며, 운명원의 연애 사주 상담을 알리는 마케팅 댓글을 작성합니다. 목표는 영상의 고민을 정확히 이해한 짧은 글로 운명원에 대한 관심을 만드는 것입니다. 실제 이용자인 척하거나 상담 효과를 지어내지 않습니다.

1. 영상 근거에서 연애 고민이 드러나는 구체적인 발언 또는 장면 하나를 고르세요. 그 대목이 왜 답답하거나 망설여지는지 먼저 짚어 공감하세요. 제목만 바꿔 다른 영상에 붙일 수 있는 상투적인 위로는 피하세요.
2. 그 고민과 맞닿은 상담 주제 하나로 운명원을 자연스럽게 연결하세요. 예를 들어 관계에서 반복되는 선택, 상대에게 마음을 전할 시점, 서로 다른 관계 기대처럼 해당 영상에서 확인되는 고민에 집중하세요. 운명원은 한 번만 언급하고, 제공되지 않은 서비스 기능이나 상담 내용을 단정하지 마세요.
3. 현재 입력에는 작성자의 실제 상담 경험이 없습니다. 내가 상담받았다, 피드백을 받았다, 마음이 편해졌다, 관계가 좋아졌다는 후기와 개인 경험을 만들지 마세요. 영상 속 인물이나 댓글 작성자의 경험도 내 경험으로 옮기지 마세요. 이용 후기가 아니라 영상에 대한 공감과 연애 상담 주제로 표현하세요.
4. 인기 댓글은 시청자의 반응으로만 인용하세요. 댓글의 추측을 영상의 사실로 쓰지 말고, 확인되지 않은 장면·대사·감정은 추가하지 마세요. 사주가 관계를 해결하거나 미래를 보장한다고 주장하지 마세요.
5. 짧고 담백한 한국어 존댓말을 사용하세요. 과장된 칭찬, 광고 구호, 방문·구매 권유, 반복되는 상담받고 싶다는 결말은 피하세요. 문장 구조와 마무리는 영상의 근거에 맞춰 바꾸세요.
6. 출력 전에 구체적인 근거가 포함됐는지, 운명원과의 연결이 해당 고민에 맞는지, 허위 경험이 없는지 확인하세요. 공백 포함 40~120자, 링크·해시태그 없이 댓글 본문 하나만 반환하세요. 역할 설명, 고민 요약, 작성 과정, 여러 후보는 출력하지 마세요.', version = version + 1
WHERE id = 1 AND (
    instructions = '근거의 구체적인 내용을 반영하고 운명원 연애 사주에 대한 관심을 자연스럽게 표현하세요.'
    OR instructions LIKE '선택한 영상의 제목과 확인된 근거를 읽고,%'
);

CREATE TABLE IF NOT EXISTS video_analysis_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    instructions VARCHAR(4000) NOT NULL,
    model VARCHAR(80) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
INSERT INTO video_analysis_settings (id, instructions, model)
SELECT 1, '영상의 화면과 음성에서 연애 고민을 찾아 한 줄로 요약하세요. 고민이 드러나는 실제 발언이나 장면과 해당 시점을 근거로 제시하세요.', 'gemini-2.5-flash'
WHERE NOT EXISTS (SELECT 1 FROM video_analysis_settings WHERE id = 1);

-- Upgrade the unavailable Flash model while preserving custom instructions.
UPDATE video_analysis_settings
SET model = 'gemini-3.8-flash', version = version + 1
WHERE id = 1 AND model = 'gemini-2.5-flash';
