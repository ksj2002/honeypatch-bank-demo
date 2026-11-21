# Honey-Patches 데모 (가상 은행 관리자 로그인)

소프트웨어 및 시스템 보안 · 논문 구현 프로젝트 (9팀)

> ⚠️ **개발 전 반드시 읽을 것: 이 README → [`docs/api.md`](docs/api.md)**
> ⚠️ **`main`에 직접 push 금지. 반드시 브랜치를 만들어서 작업하고 PR로 올린다.**

---

## 1. 프로젝트 소개

논문 **Honey-Patches**의 핵심 아이디어를 웹 데모로 구현한다.

기존 보안 패치는 공격을 차단하는 순간 공격자에게 "막혔다"는 신호를 준다.
허니 패치는 같은 탐지 지점에서 차단 대신 **가짜 환경으로 연결**해서, 공격자가 공격의 성공/실패를 구분하지 못하게 한다.

가상의 은행 관리자 로그인에 **의도적인 SQL 인젝션 취약점**을 두고, 같은 공격 입력이 세 가지 모드에서 어떻게 달라지는지 비교한다.

| 모드 | 공격 입력 결과 |
|---|---|
| VULNERABLE (취약) | 공격 성공, **실제** 관리자 화면 노출 |
| PATCHED (일반 패치) | 인젝션 탐지 후 로그인 **차단** |
| HONEY (허니 패치) | 성공한 것처럼 보이지만 **가짜 데이터**가 담긴 관리자 화면 |

> 화면은 **로그인 / 관리자 2개뿐**이다. 허니 패치용 화면을 따로 만들지 않는다.
> 서버가 모드에 따라 같은 화면에 **다른 데이터**를 내려줄 뿐이다.

## 2. 기술 스택

| 구분 | 기술 |
|---|---|
| Front | HTML, CSS, JavaScript (`fetch`로 API 호출) |
| Back | **Java 17**, **Spring Boot 4.1.2**, Gradle (Groovy) |
| DB | H2 인메모리 (서버를 끄면 초기화, 시작할 때 `schema.sql`/`data.sql` 실행) |

## 3. 폴더 구조와 담당

### 전체
```
honeypatch-bank-demo/
  frontend/        화면
  backend/         Spring Boot 서버 (포트 8080)
  docs/api.md      API 명세 (개발 기준 문서)
```

### backend
`backend/src/main/java/com/team9/honeypatchbankdemo/`
```
honeypatchbankdemo/
  HoneypatchBankDemoApplication.java   ← 아무도 안 건드림
  common/                              ← 팀장이 만들어 둠 (공유 약속)
    Mode.java                          모드 enum (VULNERABLE / PATCHED / HONEY)
    ModeHolder.java                    현재 모드 보관
    SessionKeys.java                   세션 키 이름 상수
    CorsConfig.java                    CORS 설정
  auth/                                ← 백엔드 A
    LoginController.java
    LoginRequest.java / LoginResponse.java
    AuthService.java                   (취약 쿼리 실행)
    InjectionDetector.java             (탐지 로직)
    InjectionFilter.java               (Spring 필터)
  admin/                               ← 백엔드 B
    CustomerController.java
    CustomerRepository.java            (JdbcTemplate으로 조회)
    ModeController.java                (모드 조회/변경 API)
```
`backend/src/main/resources/`
```
  schema.sql, data.sql                 ← 백엔드 B
  application.properties               ← 팀장
```

### frontend
```
frontend/
  index.html        로그인 화면      (프론트 A)
  admin.html        관리자 화면      (프론트 B)
  css/login.css     (프론트 A)
  css/admin.css     (프론트 B)
  js/config.js      API 주소 (팀장이 만들어 둠, 공유)
  js/login.js       (프론트 A)
  js/admin.js       (프론트 B)
```

### 역할 분담
| 담당 | 이름 | 맡은 일 |
|---|---|---|
| 팀장 | 김석준 | API 명세, `common/`, 설정 파일 관리, 리뷰/merge |
| 프론트 A | 박서연 | 로그인 화면, 모드 전환 UI|
| 프론트 B | 남영서| 관리자 화면 (고객 목록 표시, 로그아웃), 보고서 작성 |
| 백엔드 A | 김우석| 로그인 API, 취약 쿼리, 인젝션 탐지 필터 |
| 백엔드 B | 이근창| H2 데이터(진짜/가짜), 고객 목록 API, 모드 API |

### 파일 수정 규칙
- **자기 담당 폴더/파일에만** 새 파일을 만들고 수정한다.
- 다음 파일은 **팀장만 수정**한다. 고칠 일이 있으면 팀장에게 요청한다.
  - `build.gradle`, `application.properties`, `HoneypatchBankDemoApplication.java`
  - `common/` 안의 모든 파일, `js/config.js`
- 같은 파일을 두 명이 동시에 고치면 충돌이 난다. 담당을 지키는 게 충돌을 막는 가장 확실한 방법이다.

## 4. 개발 환경과 실행 방법

### 준비물
- **JDK 17** (`java -version`으로 확인)
- Git
- VS Code + 확장 **Live Server** (프론트 담당)

### Backend
```powershell
cd backend
.\gradlew bootRun        # Windows
./gradlew bootRun        # Mac, Linux
```
- `Started HoneypatchBankDemoApplication`이 보이면 성공
- 종료는 `Ctrl+C`
- 처음 실행은 라이브러리 다운로드로 몇 분 걸릴 수 있다.
- **VS Code에서 F5(디버그 실행)로 서버를 켜지 않는다.** 터미널에서 위 명령으로 켠다.
- 컴파일만 확인: `.\gradlew build`

### DB 확인 (선택)
서버 실행 중 http://localhost:8080/h2-console
- JDBC URL: 서버 실행 로그의 `Database available at 'jdbc:h2:mem:...'` 값을 복사
- User Name: `SA`, Password: 비움

### Frontend
1. VS Code에서 `index.html`을 열고 우클릭 → **Open with Live Server**
2. 브라우저 주소가 `127.0.0.1:5500`으로 열리면 **`localhost:5500`으로 바꿔서 접속한다.**
   - 서버의 CORS 허용 주소가 `http://localhost:5500` 하나뿐이다.
   - `127.0.0.1`로 접속하면 **로그인 상태(세션 쿠키)가 유지되지 않는다.**
3. Live Server를 저장소 루트에서 열면 주소가 `localhost:5500/frontend/index.html`이 된다. 동작에는 문제없다.

## 5. 협업 규칙

### ⚠️ 브랜치 (필수)
- **`main`에 직접 push하지 않는다.** 항상 **브랜치 → PR → 리뷰 → merge** 순서로 한다.
- 브랜치 이름은 영문으로: `feature/front-이름-기능`, `feature/back-이름-기능`
  - 예: `feature/back-minsu-login`, `feature/front-jiwoo-adminpage`
- 작업 시작 전에는 항상 최신 `main`을 받는다.
  ```powershell
  git checkout main
  git pull
  git checkout -b feature/back-이름-기능
  ```

### 커밋 메시지
`타입: 내용` 형식으로 쓴다.

| 타입 | 용도 | 예시 |
|---|---|---|
| feat | 새 기능 | `feat: 로그인 API 추가` |
| fix | 버그 수정 | `fix: 인젝션 오탐 수정` |
| docs | 문서 수정 | `docs: API 명세 수정` |
| chore | 설정, 뼈대, 정리 | `chore: CORS 설정 추가` |
| refactor | 동작은 같고 코드만 정리 | `refactor: 필터 코드 분리` |
| test | 테스트 | `test: 로그인 테스트 추가` |

### PR
1. 내 브랜치를 push하면 GitHub 저장소에 **Compare & pull request** 배너가 뜬다. 눌러서 PR을 만든다.
2. 제목에 기능을 쓰고, 무엇을 바꿨는지 2~3줄로 적는다.
3. 리뷰는 서로 해도 된다. **merge는 팀장이 한다.**
4. 본인 PR은 본인이 승인할 수 없다. (승인 1명 필요)

### API가 바뀔 때
- 코드보다 **`docs/api.md`를 먼저 수정**하고 팀에 알린다. 그다음 각자 코드를 고친다.

### 충돌이 났을 때
```powershell
git checkout main
git pull
git checkout feature/내브랜치
git merge main
# 충돌 표시(<<<<<<<, =======, >>>>>>>)를 직접 고치고 저장
git add .
git commit -m "fix: main과 충돌 해결"
git push
```

## 6. 처음 시작하는 방법

1. GitHub 초대를 **수락**한다. (수락하지 않으면 push가 거부된다.)
2. 사용자 정보를 설정한다. (이메일은 GitHub 가입 이메일)
   ```powershell
   git config --global user.name "이름"
   git config --global user.email "GitHub 가입 이메일"
   ```
3. 내려받는다.
   ```powershell
   git clone https://github.com/ksj2002/honeypatch-bank-demo.git
   cd honeypatch-bank-demo
   ```
4. 브랜치를 만들어서 작업한다.
   ```powershell
   git checkout -b feature/back-이름-기능
   # 코드 작성 후 저장
   git add .
   git commit -m "feat: ..."
   git push -u origin feature/back-이름-기능
   ```
   - `-u origin 브랜치명`은 **그 브랜치를 처음 올릴 때만** 쓴다. 이후엔 `git push`만 쓴다.
   - 첫 push 때 브라우저 로그인 창이 뜨면 GitHub 계정으로 승인한다.
5. GitHub에서 PR을 만든다.

## 7. 자주 겪는 문제

| 증상 | 원인과 해결 |
|---|---|
| 브라우저 콘솔에 CORS 에러 | 프론트를 `localhost:5500`이 아닌 다른 주소/포트로 열었을 때. `localhost:5500`으로 접속 |
| 로그인했는데 바로 풀림 (401) | `127.0.0.1`로 접속했거나, `fetch`에 `credentials: "include"`를 빼먹음 |
| `Port 8080 was already in use` | 이전에 켠 서버가 살아 있음. 그 터미널에서 `Ctrl+C`, 또는 F5로 켠 디버그 세션 중지 (Shift+F5) |
| `API_BASE is not defined` | HTML에서 `config.js`를 다른 js보다 **먼저** 불러오지 않음 |
| `main`에 push했더니 거부됨 | 정상. 브랜치를 만들어서 올리고 PR을 만든다 (`git checkout -b 새브랜치` 후 push) |