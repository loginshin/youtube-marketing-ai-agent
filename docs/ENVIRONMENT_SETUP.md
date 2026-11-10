# Marketing AI Agent — 로컬 개발 환경 구성 가이드

> 작성일: 2026-11-09<br>
> 기준: [TRACK 2 과제 안내서](track-2-marketing-agent.pdf)

## 0. 목표 및 결정 범위

React + TypeScript + Vite 화면이 REST API로 로컬 Spring Boot 모놀리스에 요청한다. Java 21과 Gradle을 사용하고, 서버 내부는 도메인별 패키지로 구성한다. OpenAI API는 backend/ai 모듈의 RestClient를 통해 호출한다. Electron은 선택지에서 제외했다.

로컬 실행을 기본으로 한다. 로컬 DB는 파일 기반 H2를 사용하고 Chrome 제어 방식은 STEP 5에서 결정한다. Playwright Java는 검증 후보이며 Docker와 최종 배포 방식은 보류한다.

## 1. 과제 요구사항 중 환경 설계에 영향 주는 사항

| PDF 요구사항 | 환경/설계 반영 |
|---|---|
| ① 영상 불러오기 → ② 영상 선택 → ③ 댓글 작업 | 3단 UI 뼈대 |
| 영상 선택 시 연애 고민 요약·댓글 초안 | 분석 결과 타입과 Mock 데이터 |
| 로컬 Chrome 프로필 사용·한국어 YouTube UI | 향후 로컬 브라우저 연동 계층 필요 |
| 영상당 댓글 1개, 하루 기본 10건, 성공/실패 기록 | 향후 영속 저장 및 원자적 중복 방지 필요 |
| 2~3분 시연 영상, Git 저장소 선택 제출 | 재현 가능한 실행 명령과 문서화 필요 |

※ PDF는 특정 언어·프레임워크·AI 모델·DB를 지정하지 않는다. **React/TypeScript는 현재 개발 편의를 위한 선택**이다.

## 2. 사전 준비

| 도구 | 준비 사항 |
|---|---|
| Node.js | 공식 사이트에서 **현재 LTS** 설치. Vite 공식 요구 조건: Node.js 20.19+ 또는 22.12+ (더 높은 템플릿 요구 버전은 설치 시 안내 따름) |
| npm | Node.js와 함께 제공 |
| Git | Git CLI 설치 |
| IDE | VS Code 또는 기존 IDE |
| Chrome | 추후 로컬 브라우저 연동 테스트에 사용. 지금은 설치만 확인 |

공식 링크:
- Node.js: https://nodejs.org/en/download
- Vite: https://vite.dev/guide/
- Git: https://git-scm.com/downloads
- JDK 21 설치 후 IDE의 프로젝트 SDK와 Gradle JVM도 21로 설정한다.

터미널에서 확인:

```bash
node -v
npm -v
git --version
java -version
javac -version
```

Windows는 PowerShell, macOS/Linux는 기본 터미널을 사용한다. Node.js 설치 후 명령을 인식하지 못하면 터미널을 다시 실행한다.

## 3. React + TypeScript 프로젝트 생성

아래는 최초 생성 기록이다. 기존 체크아웃에서는 README의 `npm ci` 절차를 사용한다.

```sh
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm run dev
```

터미널에 표시된 로컬 주소(일반적으로 `http://localhost:5173`)에서 Vite 기본 화면이 보이면 성공이다.

스모크 테스트:

```bash
npm run lint
npm run build
```

- `lint`와 `build`가 오류 없이 완료되어야 한다.
- 개발 서버 실행 중 소스 파일 수정 시 화면이 갱신되어야 한다.
### Spring Boot 서버 생성 및 실행

1. [Spring Initializr](https://start.spring.io/)에서 Gradle (Groovy), Java, Jar, Java 21을 선택한다. Group은 `com.marketing`, Artifact는 `backend`, Package name은 `com.marketing.agent`로 지정한다.
2. 기준 조합은 Spring Boot 4.1.2와 Java 21이다. AI는 별도 ai 모듈에서 OpenAI Responses API를 직접 호출한다.
3. STEP 0에는 Spring Web과 Actuator를 선택한다. ZIP을 풀어 루트 바로 아래 `backend/build.gradle`과 Gradle Wrapper가 위치하게 한다. 현재는 api, ai 멀티모듈이며 api가 실행 모듈이다. API 키 설정은 루트 README를 따른다.
4. 별도 터미널에서 서버를 실행한다. 아래는 Windows PowerShell 기준이며 macOS/Linux에서는 `./gradlew`를 사용한다.

```powershell
cd backend
.\gradlew.bat --version
.\gradlew.bat :api:bootRun
```

백엔드에서 bootRun을 실행하면 작업 디렉터리의 `data/`에 H2 파일이 생성된다. 시작할 때 루트 sql 폴더의 스키마 SQL로 테이블을 초기화하고 Hibernate가 매핑을 검증한다. 기존 테이블 구조 변경에는 별도 마이그레이션이 필요하다. 테스트는 메모리 DB를 사용한다.

### REST API 연결 검증

백엔드의 `http://localhost:8080/actuator/health`에서 HTTP 200과 JSON의 `status: UP`을 확인한다. STEP 0에서 Vite 개발 프록시가 `/actuator` 요청을 `http://localhost:8080`으로 전달하도록 설정한다. React 화면에서 상대 경로 `/actuator/health`를 호출해 상태를 표시하고, 브라우저 Network 탭에서 같은 JSON을 확인한다. 백엔드를 종료한 뒤 새로 고침하면 연결 실패가 표시되어야 한다. 이 검증에는 외부 API 키가 필요 없다.

## 4. 초기 코드 구조

```text
marketing-ai-agent/
├── sql/                 # 초기 스키마 및 조회 SQL
├── frontend/src/
│   ├── features/{video,analysis,comment}/
│   ├── components/
│   └── shared/
├── backend/api/src/main/java/com/marketing/agent/
│   ├── video/       # YouTube 영상 검색 및 데이터 조회
│   ├── analysis/    # 영상 맥락 분석 및 댓글 초안 생성
│   ├── comment/     # 댓글 검토, 작업 상태, 중복 방지, 이력
│   ├── browser/     # 로컬 Chrome 연동
│   └── global/      # 공통 설정 및 예외 처리
└── docs/
    ├── ARCHITECTURE.md
    ├── ENVIRONMENT_SETUP.md
    └── TODO.md
```

각 기능을 구현할 때 필요한 클래스만 추가한다. 빈 Controller, Service, Repository를 미리 생성하지 않는다.

초기 타입 예시(구현 단계에서 확정):

```ts
export type Video = {
  id: string;
  title: string;
  channel: string;
  publishedAt: string;
  thumbnailUrl: string;
};

export type AnalysisDraft = {
  videoId: string;
  concern: string;
  evidence: string;
  comment: string;
};

export type CommentJobStatus =
  | 'draft'
  | 'reviewed'
  | 'approved'
  | 'running'
  | 'completed'
  | 'confirmation_pending' // 게시 시도 후 결과 확인 필요
  | 'failed';
```

## 5. API 키·보안 준비 (실제 연동은 추후)

이번 단계에서는 **OpenAI API를 호출하지 않는다**. 다음 단계에서 사용자 자신의 OpenAI 프로젝트 키를 만든다.

- API 키는 **React 소스, `VITE_*` 환경변수, 빌드 산출물에 넣지 않는다**. Vite의 `VITE_*` 값은 브라우저 번들에 노출될 수 있다.
- STEP 2~3에서 백엔드 프로세스 환경변수로 `YOUTUBE_API_KEY`와 `SPRING_AI_OPENAI_API_KEY`를 전달한다. IDE 실행 설정을 사용한다면 키가 담긴 설정 파일은 로컬에만 보관한다.
- `.env`, `.env.local`, 로그, 화면 녹화, Git 저장소에 비밀 키를 남기지 않는다.

참고: https://developers.openai.com/api/reference/overview

## 6. Git 초기화

이 저장소는 루트에서 이미 초기화되어 있다. frontend와 backend를 같은 Git 저장소에서 관리하고 npm 잠금 파일과 Gradle Wrapper를 함께 커밋한다.

커밋 전 체크:

```bash
git status
git check-ignore .env.local
```

`.env.local` 무시 여부가 확인되지 않으면 `.gitignore`에 아래를 추가한다.

```gitignore
.env
.env.*
!.env.example
```

`package-lock.json`은 버전 재현성을 위해 커밋한다. 공개 저장소에 API 키·Chrome 쿠키·프로필 파일·실패 캡처를 올리지 않는다.

## 7. 완료 기준 (Definition of Done)

- [x] Node.js, npm, Git 및 Java 컴파일러 버전 확인(Java 21)
- [x] React 화면과 HMR 확인, 화면에서 백엔드 health JSON 수신
- [x] `frontend/`에서 `npm run lint`와 `npm run build` 성공
- [x] `backend/`에서 `.\gradlew.bat test`와 `.\gradlew.bat build` 성공, `api/build/libs/`에 JAR 생성 확인
- [x] 외부 API 키 없이 초기 테스트 통과, 로그·Chrome 프로필·비밀 값 제외 확인

이후 작업 순서와 시연 기준은 [개발 TODO](TODO.md)를 따른다.
