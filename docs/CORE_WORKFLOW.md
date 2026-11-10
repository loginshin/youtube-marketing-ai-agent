# 핵심 기능 워크플로우 — Marketing AI Agent

> 목적: TRACK 2 과제의 3단 대시보드 사용자 흐름과 프론트엔드·백엔드의 책임을 명확히 한다.
> 기준: `track-2-marketing-agent.pdf` 및 현재 합의한 `React + Spring Boot` 단일 백엔드 아키텍처.
> 상태: **설계 초안**. 로컬 H2·JPA와 초기 SQL은 구성됐다. 아래 사용자 흐름은 후속 구현 범위이며 Docker와 Chrome 연동 방식은 미정이다.

## 1. 핵심 사용자 워크플로우

```mermaid
flowchart TD
    A([시작]) --> B["① 영상 불러오기: 최신 목록 표시"]
    B --> C["② 영상 선택 (URL 직접 입력은 가점)"]
    C --> D["영상 정보 · 설명 · 공개 인기 댓글 조회"]
    D --> E{"사용 가능한 자막이 있는가?"}
    E -->|예| F["자막을 분석 입력에 추가"]
    E -->|아니오| G["제목 · 설명 · 댓글 기반 분석"]
    F --> H["OpenAI: 연애 고민 · 근거 · 댓글 초안 생성"]
    G --> H
    H --> I{"근거 · 형식 검증 통과?"}
    I -->|아니오| J["정보 부족 또는 수정 필요 표시"]
    J --> X["근거 보완 또는 초안 수정"]
    X --> I
    K["최종 본문 · 영상 · 프로필 검토"]
    I -->|예| K
    K --> L{"사용자가 댓글을 승인했는가?"}
    L -->|수정| X
    L -->|취소| W([게시 없이 종료])
    L -->|예| M{"서버 재검증 · 중복 검사 · 일일 한도 예약"}
    M -->|차단| N["작업 생성 없이 차단 사유 반환"]
    M -->|통과| O["③ 로컬 Chrome 전용 프로필 열기"]
    O --> P["영상 이동 · 댓글 입력 UI 확인"]
    P --> Q["승인된 본문 입력 · 게시"]
    Q --> R{"게시 결과가 확인되는가?"}
    R -->|확인| S["COMPLETED: 댓글 확인 · 기록"]
    R -->|미게시 확정| T["FAILED: 원인 · 스크린샷 기록"]
    R -->|확인 불가| U["CONFIRMATION_PENDING: 자동 재게시 금지"]
    O -. 실행 오류 .-> T
    P -. 화면 오류 .-> T
    N --> V["대시보드 상태 · 이력 표시"]
    S --> V
    T --> V
    U --> V
```

### 검증 규칙

- AI 초안: **40~120자**, 영상별로 다르게, 확인된 구체적인 장면 또는 발언을 최소 하나 반영한다.
- 금지: 링크, 해시태그, 직접 이용하지 않은 **운명원** 후기 조작. 확인되지 않은 영상 내용을 지어내지 않는다.
- 정보가 부족해 구체적인 근거를 확보할 수 없으면 **근거 부족** 상태로 표시하고, 자동으로 게시 가능 상태로 진행시키지 않는다.
- 영상별 **1개**의 댓글 게시, **일일 기본 최대 10건** 제한. 성공 여부가 불명확할 때는 재시도 전에 사람의 확인이 필요하다.
- 게시·프로필 사용은 **사용자 검토 및 명시적 승인**을 전제로 하고, 본인이 관리하거나 허가받은 테스트 환경에서 검증한다. 다중 계정 반복 홍보와 기만적 시청자 사칭을 자동화하지 않는다.

## 2. 프론트엔드 ↔ 백엔드 흐름

```mermaid
sequenceDiagram
    actor User as 사용자
    participant UI as React Dashboard
    participant Video as Spring video
    participant YT as YouTube API
    participant Analysis as Spring analysis
    participant AI as OpenAI API
    participant Comment as Spring comment
    participant Browser as Local Browser Adapter

    User->>UI: 영상 불러오기
    UI->>Video: 영상 검색 요청
    Video->>YT: 최신 영상 조회
    YT-->>Video: 목록 데이터
    Video-->>UI: 썸네일·제목·채널·날짜

    User->>UI: 영상 선택
    UI->>Video: 영상 맥락 조회
    Video->>YT: 설명·공개 댓글 등 조회
    YT-->>Video: 접근 가능한 정보
    Video-->>UI: 선택 영상과 접근 가능한 맥락
    UI->>Analysis: 선택 영상 분석 요청
    Analysis->>AI: 영상 근거 기반 분석·초안 생성
    AI-->>Analysis: 고민·근거·초안
    Analysis-->>UI: 검증된 결과 또는 근거 부족

    User->>UI: 초안 수정 및 승인
    UI->>Comment: 최종 영상·프로필·본문으로 승인 요청
    Comment->>Comment: 근거·형식 재검증 및 중복·일일 상한 원자적 검사와 예약
    alt 허용
        Comment->>Browser: 로컬 프로필/페이지 열기 요청
        Browser->>Browser: 승인된 본문 입력·게시 후 내 댓글 확인
        Browser-->>Comment: 완료 / 실패 / 확인 불가
        Comment-->>UI: 작업 상태 및 이력
    else 차단
        Comment-->>UI: 차단 사유
    end
```

> 위 다이어그램은 **논리적 상호작용**을 표현한다. 영상 맥락의 `video → analysis` 전달 방식과 브라우저의 프로세스 간 연결 방식 등은 구현 단계에서 결정한다.

## 3. 최소 작업 상태

```mermaid
stateDiagram-v2
    [*] --> APPROVED: 승인 및 작업 등록
    APPROVED --> RUNNING: 규칙 검사 통과
    RUNNING --> COMPLETED: 게시·댓글 확인
    RUNNING --> FAILED: 명확한 실패
    RUNNING --> CONFIRMATION_PENDING: 결과 확인 불가
    FAILED --> APPROVED: 미게시 확인 후 재승인 및 한도 확보
    CONFIRMATION_PENDING --> COMPLETED: 게시 확인
    CONFIRMATION_PENDING --> FAILED: 미게시 확인
```

초안 작성·검토와 요청 차단은 UI 단계 또는 요청 결과다. DB 작업은 승인과 한도 확보 후 `APPROVED`부터 기록한다. 재시도는 같은 영상의 기존 작업을 사용한다. 상태와 이력은 한 트랜잭션으로 갱신한다.

- **RUNNING:** 로컬 브라우저 작업 진행 중. 화면 상태(프로필 열기·입력 중 등)는 세부 이벤트로 기록한다.
- **COMPLETED:** 화면에서 본인 댓글 확인까지 완료.
- **FAILED:** 명확한 오류 원인과 가능하면 스크린샷을 남긴다.
- **CONFIRMATION_PENDING:** 게시 성공 여부를 확인하지 못한 상태. 자동 재게시하지 않는다.

## 4. 책임 경계

| 영역 | 책임 |
| --- | --- |
| `frontend/src/features/video` | 영상 목록, 선택, 검색 UI |
| `frontend/src/features/analysis` | 고민·근거·초안 표시, 수정·검토 |
| `frontend/src/features/comment` | 승인, 작업 상태, 이력 UI |
| `backend/video` | 영상 검색, 접근 가능한 맥락 수집 및 정규화 |
| `backend/analysis` | OpenAI 호출, 근거 확인, 초안 규칙 검증 |
| `backend/comment` | 중복·일일 한도, 승인 상태, 작업 이력, 결과 정합성 |
| `browser adapter` | 로컬 브라우저 실행/상태 확인. 실행 위치는 별도 기술 검증 필요 |

## 5. 후속 결정 사항

- STEP 4에서 일일 상한의 날짜 기준과 예약·해제 트랜잭션을 결정한다. 9건 사용 시 동시 요청 두 건 중 하나만 게시 단계에 진입해야 한다.
- Chrome 연결: 로컬 Spring Boot 프로세스와 OS별 Chrome 프로필/Playwright 연동 가능성 검증.
- 자막 접근: 모든 공개 영상의 자막이 YouTube Data API로 제공된다고 가정하지 않음.
- 작업 상태 전파: 초기 폴링 vs SSE (요구가 생기면 결정).
- API 명세와 추가 저장 필드는 화면 계약에 따라 결정한다. 현재 기준은 [작업 상태](../backend/api/src/main/java/com/marketing/agent/db/entity/JobStatus.java)와 [초기 SQL](../sql/schema.sql)이다.

## 6. 완료 기준 (설계 검토)

- [ ] 3단 UI의 각 버튼에 대응하는 요청·결과·실패 흐름이 설명된다.
- [ ] AI 결과 검증과 사용자의 최종 승인 지점이 구분된다.
- [ ] 영상당 1개 및 하루 10건 제한이 작업 실행 전에 검사된다.
- [ ] 실제 게시 확인 실패 시 `CONFIRMATION_PENDING`으로 분기하고 중복 재게시를 막는다.
- [ ] 등록된 작업의 상태 변경은 이력으로 남고, 등록 전 차단 사유는 UI에서 확인된다.
- [ ] 미확정 인프라 선택을 구현이 이미 확정된 것처럼 표현하지 않는다.
