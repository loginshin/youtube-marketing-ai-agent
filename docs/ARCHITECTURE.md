# 마케팅 AI 에이전트 아키텍처

STEP 0에서 로컬 개발 환경과 서버 연결을 검증했다. React가 Vite 개발 프록시를 통해 Spring Boot의 `GET /actuator/health`를 호출한다. STEP 1에서 3단 예시 대시보드를 구현했다. YouTube 검색 화면은 설명과 공개 댓글을 조회하고, 사용자가 확인한 근거로 AI 초안을 요청한다. [실행 가이드](ENVIRONMENT_SETUP.md) · [단계별 구현 계획](TODO.md)

## 1. 확정한 기술·아키텍처 결정

### 시스템 구성

- **Frontend:** React + TypeScript + Vite
- **Backend:** Spring Boot + Java 21 + Gradle Wrapper
- **AI 연동:** ai 모듈의 RestClient를 이용한 OpenAI Responses API 호출
- **통신:** Frontend ↔ Backend 사이 REST API
- **배포 단위:** 백엔드는 **단일 Spring Boot 애플리케이션**으로 유지한다. 도메인별 서버 분리/MSA는 하지 않는다.
- **코드 조직:** 프론트엔드는 **Feature-Based Architecture**, 백엔드는 **도메인별 패키지 기반 Modular Monolith + 필요한 범위의 Layered Architecture**를 사용한다.
- **외부 시스템:** OpenAI, YouTube, 로컬 Chrome/Playwright는 경계 인터페이스 또는 Adapter로 격리한다. 처음부터 모든 기능에 Port/Adapter를 만들지는 않는다.
- **로컬 실행 우선:** Chrome 프로필 접근이 필요하므로 우선 개발 PC에서 프론트엔드와 백엔드를 실행한다.

### 영속 저장

API 모듈에 JPA 엔티티가 있고 저장소 루트의 SQL 폴더에서 [초기 스키마](../sql/schema.sql)를 제공한다. 로컬 H2 파일에 영상별 작업 하나와 상태 변경 이력을 저장한다. 재시도는 같은 작업에 이력을 추가하는 방식으로 설계한다. 작업 상태 변경과 이력 추가는 한 트랜잭션에서 처리해야 한다.

### 현재 미확정 사항

- Docker 적용 여부 및 배포/패키징
- Chrome 자동화의 최종 실행 방식(Playwright Java 가능성 검증 필요)
- 인증·멀티유저·서버 외부 공개 여부(현재 로컬 단일 사용자 과제 기준)

STEP 4에서 작업 상태 전이, 재시도 및 일일 상한의 트랜잭션 경계를 구현한다. Chrome 연동 방식은 STEP 5에서 전용 프로필과 한국어 UI로 검증한다. Docker와 인증은 배포 대상이나 사용자 범위가 달라질 때 재검토한다.

## 2. 프론트엔드 설계 지침

STEP 0에는 features, components, shared가 있다. 아래는 대시보드 구현 시의 목표 구조다. pages와 app은 화면 조합이나 라우팅이 필요해질 때 추가하고, 공통 UI는 shared로 모은다.

```text
frontend/src/
├── app/                     # 앱 진입점, 전역 Provider, 라우팅(필요할 때)
├── pages/
│   └── dashboard/           # 영상/분석/댓글 작업 기능 조합
├── features/
│   ├── video/               # 영상 목록·선택·상세 조회
│   ├── analysis/            # 고민 요약·근거·초안·검토
│   └── comment/             # 작업 생성·상태·이력
└── shared/                  # 재사용 UI, API 클라이언트, 공통 유틸
```

**규칙**

- 각 feature가 자체 `components`, `api`, `hooks`, `types`를 필요에 따라 소유한다.
- `pages`는 feature를 조합하고, 도메인 로직을 중복 구현하지 않는다.
- `shared`에는 특정 feature에 종속된 비즈니스 코드를 두지 않는다.
- 다른 feature의 내부 경로를 직접 참조하지 않고 필요한 공개 인터페이스를 사용한다.
- 서버 데이터 상태와 UI 로컬 상태를 구분한다. Redux 등의 전역 상태 라이브러리는 필요가 검증되기 전까지 도입하지 않는다.
- 컴포넌트 수준에서 OpenAI/YouTube 비밀 키를 취급하지 않는다. 외부 API의 비밀 키는 백엔드에만 둔다.

## 3. 백엔드 설계 지침

다음 구조를 **설계 기준**으로 채택한다.

```text
backend/api/src/main/java/com/marketing/agent/
├── video/
│   ├── presentation/        # REST API 및 DTO
│   ├── application/         # 영상 검색/조회 유스케이스
│   ├── domain/              # 영상 관련 모델·규칙
│   └── infrastructure/      # YouTube API 연동
├── analysis/
│   ├── presentation/
│   ├── application/
│   ├── domain/
│   └── infrastructure/      # ai 모듈 호출
├── comment/
│   ├── presentation/
│   ├── application/
│   ├── domain/              # 작업 상태·중복/상한 정책
│   └── infrastructure/      # 작업 이력 저장 연동
├── browser/                 # 승인된 작업의 로컬 Chrome 조작
└── global/
    ├── config/
    └── exception/
```

**도메인 책임**

- `video`: YouTube 영상 검색·조회·영상 메타데이터·접근 가능한 맥락 데이터 수집
- `analysis`: 영상 근거 기반 고민 요약 및 AI 댓글 초안 생성·검증
- `comment`: 초안 검토/승인, 댓글 작업 상태, 영상당 중복 방지, 일일 상한, 이력
- `browser`: 현재 최상위 기술 연동 패키지다. `comment`가 승인·중복·상한을 판단한 뒤 실행을 요청하면 Chrome 조작 결과를 반환한다. 구현 도구와 패키지 이동 여부는 STEP 5에서 결정한다.
- `global`: 로깅·설정·공통 예외 등 **진짜 공통 요소만** 배치한다.

**의존성 및 계층 규칙**

1. Controller는 유스케이스(Application)를 호출하고, 외부 API SDK 또는 Repository에 직접 의존하지 않는다.
2. Application은 작업 흐름을 조정하고, Domain은 비즈니스 규칙을 표현한다.
3. Domain은 Spring AI, YouTube SDK, Playwright, JDBC/JPA 등에 의존하지 않는다.
4. 외부 서비스 교체 가능성이나 테스트 필요성이 있는 경계에 한해 인터페이스(Port)를 둔다.
5. 다른 도메인의 `repository`/`infrastructure`를 직접 호출하지 않는다. 필요하면 명시적 서비스 경계 또는 공개 API로 협력한다.
6. 빈 패키지, 추상 클래스, 인터페이스, DTO를 미리 대량 생성하지 않는다.
7. Spring Modulith는 **검토 대상**으로만 기록한다. 현재 단계에서 의존성을 추가하지 않는다.

## 4. 주요 사용자 흐름 및 데이터 경계

다음은 STEP 1 이후 구현할 흐름이다.

1. React 대시보드에서 영상 목록 검색/선택
2. Backend `video`가 YouTube의 사용 가능한 메타데이터·설명·공개 댓글 수집
3. `analysis`가 OpenAI를 호출하고 **고민 요약, 실제 확인 가능한 근거, 40~120자 댓글 초안** 반환
4. 사용자가 초안을 확인·편집·승인
5. `comment`가 영상 중복/일일 한도/작업 상태를 관리
6. 사용자의 허가를 받은 로컬 브라우저 작업 및 완료/실패 결과 기록
7. React가 작업 상태·실패 이유·이력을 표시

**운영 제약**

- YouTube Data API로 모든 공개 영상 자막에 접근할 수 있다고 가정하지 않는다.
- AI가 실제로 확인하지 못한 영상 장면/대사를 지어내지 않도록 한다.
- 사용자에게 보이는 진행 상태와 DB/저장소의 실제 작업 상태가 일치해야 한다.
- 네트워크 실패, 게시 결과 확인 실패처럼 **성공 여부가 불확실한 상태**를 따로 설계하고 무조건 재시도해 중복 게시되지 않게 한다.
- 과제의 로컬 Chrome 요구를 고려해 **원격 서버/Docker 내부 브라우저가 사용자의 로컬 Chrome 프로필에 바로 접근할 수 있다고 가정하지 않는다**.
- 댓글은 사용자의 명시적 검토·승인을 전제로 설계하고, 기만적 홍보·시청자 사칭·다중 계정 반복 게시를 자동화하지 않는다. 실제 게시 검증은 본인 관리/허가된 테스트 환경을 대상으로 한다.

게시 확인에 실패한 작업은 결과 확인 대기로 보관한다. 확인 전에는 같은 영상에 새 작업을 허용하지 않는다. 구체적인 상태 전이와 재시도 정책은 STEP 4에서 확정한다.

백엔드 Gradle 모듈은 api, ai로 나눈다. api만 실행 애플리케이션이며 ai에 의존한다. 키 설정과 실행법은 루트 README를 따른다.


영상 조회는 `VideoController`에서 HTTP 요청을 받고 `VideoQueryService`에서 검색 입력 검증과 영상·댓글 조회를 조합한다. `YouTubeClient`가 외부 API 통신을 담당한다. 각 기능의 HTTP 오류 응답은 해당 컨트롤러에 적용되는 예외 처리기에서 정의한다.

`backend/api`의 `CommentJobRepository`와 `CommentJobEventRepository`는 기존 작업·이력 엔티티에 대한 Spring Data JPA 저장소다.
