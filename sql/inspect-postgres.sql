-- 클립데스크 PostgreSQL 조회 모음
-- DBeaver에서 marketing_agent 연결을 선택하고 원하는 쿼리 안에서 Ctrl+Enter를 누르세요.

-- 1. 현재 연결과 public 스키마의 테이블 목록
SELECT current_database() AS database_name, table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- 2. 테이블별 컬럼과 자료형
SELECT table_name, column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
ORDER BY table_name, ordinal_position;

-- 3. 최근 승인 작업 100건과 최종 본문
SELECT id, video_id, profile_name, body, status,
       created_at AT TIME ZONE 'Asia/Seoul' AS created_at_kst
FROM public.comment_job
ORDER BY created_at DESC, id DESC
LIMIT 100;

-- 4. 특정 영상의 승인 본문과 시간순 이력
-- 아래 문자열을 확인하려는 YouTube 영상 ID로 바꾸세요.
SELECT j.id AS job_id, j.video_id, j.profile_name, j.body, j.status AS current_status,
       e.id AS event_id, e.status AS event_status,
       e.occurred_at AT TIME ZONE 'Asia/Seoul' AS occurred_at_kst,
       e.failure_reason, e.screenshot_path
FROM public.comment_job AS j
LEFT JOIN public.comment_job_event AS e ON e.job_id = j.id
WHERE j.video_id = 'YOUR_VIDEO_ID'
ORDER BY e.occurred_at, e.id;

-- 5. 오늘의 전체 프로필 승인 수와 남은 승인 수 (한국 시간, 하루 한도 10건)
WITH today AS (
    SELECT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date AS kst_date
)
SELECT COUNT(*) AS approved_today, GREATEST(10 - COUNT(*), 0) AS remaining_today
FROM public.comment_job AS j
CROSS JOIN today
WHERE j.created_at >= (today.kst_date::timestamp AT TIME ZONE 'Asia/Seoul')
  AND j.created_at < ((today.kst_date + 1)::timestamp AT TIME ZONE 'Asia/Seoul');

-- 6. 최근 작업 이력 100건
SELECT id, job_id, status, occurred_at AT TIME ZONE 'Asia/Seoul' AS occurred_at_kst,
       failure_reason, screenshot_path
FROM public.comment_job_event
ORDER BY occurred_at DESC, id DESC
LIMIT 100;

-- 7. 동시 승인 제어용 행 확인 (초기화 후 id = 1 한 행)
SELECT id FROM public.approval_guard ORDER BY id;

-- 8. 다음 댓글 초안 생성에 적용되는 작성 지침과 수정 버전
SELECT model_tier, instructions, version FROM public.comment_prompt WHERE id = 1;

-- 9. 최근 API 호출과 제공자가 반환한 토큰 수
SELECT occurred_at AT TIME ZONE 'Asia/Seoul' AS time_kst, provider, operation, input_tokens, output_tokens
FROM public.api_usage_event ORDER BY occurred_at DESC, id DESC LIMIT 100;

-- 10. 영상별 AI 생성 상태와 저장된 초안
SELECT video_id, stage, concern, evidence, draft FROM public.video_analysis ORDER BY video_id;

-- 11. 영상별 AI 생성 이력 (과거 데이터의 기록 시각은 NULL)
SELECT video_id, recorded_at AT TIME ZONE 'Asia/Seoul' AS time_kst, outcome, evidence, draft
FROM public.analysis_revision ORDER BY recorded_at DESC NULLS LAST, video_id;
