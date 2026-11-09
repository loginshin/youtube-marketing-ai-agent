# Marketing AI Agent — 로컬 개발 환경 구성 가이드

> 작성일: 2026-11-09<br>
> 상태: **개발 환경 1차안** / 서버·DB·데스크톱 런타임 최종 결정 보류<br>
> 기준: TRACK 2 「마케팅 AI 에이전트 개발」 과제 안내서 (7쪽)

## 0. 목표 및 결정 범위

### 이번 단계에서 결정하는 것
- **React + TypeScript + Vite** 기반의 3단 대시보드 개발 환경을 만든다.
- **Node.js LTS + npm + Git**으로 팀/개인 로컬 개발을 재현 가능하게 한다.
- 영상/AI/작업 상태는 우선 **Mock 데이터와 TypeScript 인터페이스**로 구분해 개발한다.
- 실제 외부 API와 브라우저 제어에 앞서 화면·개발 도구가 동작하는지 검증한다.

### 아직 결정하지 않는 것 — 인프라 Backlog
- Electron 로컬 앱 vs Spring Boot + React/로컬 서비스
- SQLite vs JSON 파일 등 영속 저장 방식
- Docker 도입 여부
- Chrome 프로필 연결·실행 방식, 최종 패키징/배포 방식

> **이유:** 과제는 '로컬 대시보드'를 요구하지만 구체적인 서버/DB 기술을 요구하지 않는다. React UI를 먼저 분리해 만들면 선택 변경 비용이 낮다.

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
- Electron(추후 검토): https://www.electronjs.org/docs/latest/

터미널에서 확인:

```bash
node -v
npm -v
git --version
```

Windows는 PowerShell, macOS/Linux는 기본 터미널을 사용한다. Node.js 설치 후 명령을 인식하지 못하면 터미널을 다시 실행한다.

## 3. React + TypeScript 프로젝트 생성

**새 디렉터리에서** 아래 명령 실행:

```bash
npm create vite@latest marketing-agent -- --template react-ts
cd marketing-agent
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
- 초기에는 Electron, Spring Boot, SQLite, Docker를 **설치하거나 연결할 필요가 없다**.

## 4. 초기 코드 구조 제안

Vite가 생성한 파일을 유지하면서 기능별 폴더를 점진적으로 추가한다.

```text
marketing-agent/
├── src/
│   ├── features/
│   │   ├── videos/          # 목록, 선택, 영상 타입
│   │   ├── analysis/        # 고민 요약, 댓글 초안 타입
│   │   └── comment-jobs/    # 작업 요청/상태 타입
│   ├── mocks/               # UI 검증용 가상 데이터
│   ├── shared/              # 공통 UI, 타입, 추상화
│   └── App.tsx              # 3단 레이아웃
├── public/
├── package.json
├── package-lock.json
└── index.html
```

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
- API를 호출할 신뢰 가능한 실행 계층이 확정되면 그 계층에서만 비밀 키를 사용한다.
- 로컬 개발에서는 해당 프로세스 환경변수 또는 OS 비밀 저장소를 고려한다.
- 데스크톱 앱을 제3자에게 배포한다면 앱에 공용 OpenAI 키를 동봉하지 않는다. 사용자별 키 입력/보안 저장 또는 서버 프록시를 별도 설계해야 한다.
- `.env`, `.env.local`, 로그, 화면 녹화, Git 저장소에 비밀 키를 남기지 않는다.

참고: https://developers.openai.com/api/reference/overview

## 6. Git 초기화

Vite 템플릿 생성 후 (아직 Git 저장소가 아니라면):

```bash
git init
git add .
git commit -m "chore: initialize react vite workspace"
```

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

- [ ] `node -v`, `npm -v`, `git --version` 정상 출력
- [ ] `npm run dev` 실행 후 기본 React 화면 노출
- [ ] 파일 수정 시 HMR 확인
- [ ] `npm run lint` 성공
- [ ] `npm run build` 성공
- [ ] Git 초기 커밋 완료
- [ ] 비밀 값 및 Chrome 프로필 데이터가 저장소에 없음
- [ ] 3단 대시보드용 폴더/타입 설계 방향 합의

## 8. 이후 작업과 보류 사항

1. **다음 개발 작업:** 3단 대시보드를 Mock 데이터로 구현.
2. **그다음:** YouTube 데이터 조회 계층과 OpenAI 분석 계층의 인터페이스 설계.
3. **인프라 결정 시점:** 로컬 Chrome 연동 및 영속성 구현 전에 Electron/Spring, SQLite/JSON, Docker 여부를 ADR로 확정.
4. **브라우저/댓글 관련 주의:** 과제의 자동 게시 요구는 플랫폼 정책 검토 대상이다. 개발·시연은 본인 관리 또는 허가받은 테스트 대상에서, 사용자 검토와 승인 중심으로 진행한다.

## 9. 공식 문서

- Vite Getting Started: https://vite.dev/guide/
- Node.js Downloads: https://nodejs.org/en/download
- OpenAI API Authentication: https://developers.openai.com/api/reference/overview
- Electron Security: https://www.electronjs.org/docs/latest/tutorial/security
- Playwright Persistent Context: https://playwright.dev/docs/api/class-browsertype#browser-type-launch-persistent-context

---

**정리:** 현재는 React + TypeScript 개발 환경만 구성한다. 서버/DB/데스크톱 런타임을 앞당겨 선택하지 않는다.
