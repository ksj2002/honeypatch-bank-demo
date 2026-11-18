# Honey-Patches 데모 (은행 관리자 로그인)

소프트웨어 및 시스템 보안 · 논문 구현 프로젝트 (9팀)

## 프로젝트 소개
논문 **Honey-Patches**의 핵심 아이디어를 웹 데모로 구현한다.
기존 보안 패치는 공격을 차단하면서 공격자에게 "막혔다"는 신호를 주지만,
허니 패치는 같은 탐지 지점에서 차단 대신 **가짜 환경으로 연결**해
공격자가 성공/실패를 구분하지 못하게 한다.

가상의 은행 관리자 로그인에 의도적인 SQL 인젝션 취약점을 두고,
같은 공격 입력이 세 가지 모드에서 어떻게 달라지는지 비교한다.

| 모드 | 공격 입력 결과 |
|---|---|
| VULNERABLE (취약) | 공격 성공, 실제 관리자 화면 노출 |
| PATCHED (일반 패치) | 인젝션 탐지 후 로그인 차단 |
| HONEY (허니 패치) | 성공한 것처럼 보이지만 가짜 데이터가 담긴 관리자 화면 |

## 기술 스택
- Front: HTML, CSS, JavaScript (fetch)
- Back: Java, Spring Boot (필터에서 입력 검사/미끼 전환)
- DB: H2 인메모리

## 폴더 구조
- `frontend/` : 화면 (로그인, 관리자)
- `backend/` : Spring Boot 서버 (포트 8080)
- `docs/api.md` : **API 명세 (개발 전 반드시 읽기)**

## 개발 순서
1. `docs/api.md`를 읽고 자기 담당 부분을 파악한다.
2. 담당 기능 브랜치를 만들어 작업한다.
3. PR을 올리고 리뷰 후 merge한다.

## 역할 분담
| 담당 | 이름 | 내용 |
|---|---|---|
| 프론트 A | | 로그인 화면, 모드 전환 UI |
| 프론트 B | | 관리자 화면 (고객 목록) |
| 백엔드 A | | 로그인 API, 취약 쿼리, 인젝션 탐지 필터 |
| 백엔드 B | | H2 데이터(진짜/가짜), 고객 목록 API, 세션/모드 |
| 팀장 | 김석준 | API 명세, 리뷰/merge |

## 실행 방법
- Backend: (TODO: 백엔드 담당이 채우기)
- Frontend: (TODO: 프론트 담당이 채우기)
- 확인 주소: 백엔드 http://localhost:8080

## 협업 규칙
### 브랜치
- `main`에 직접 push 금지. 항상 **브랜치 → PR → 리뷰 → merge**
- 브랜치명: `feature/front-이름-기능`, `feature/back-이름-기능`
  - 예: `feature/back-minsu-login`
- 같은 파일을 두 명이 동시에 고치지 않도록 담당을 나눈다.

### 커밋 메시지
- `feat: 로그인 API 추가`, `fix: 인젝션 오탐 수정`, `docs: 명세 수정`

### PR 규칙
- PR 제목에 기능을 적고, 무엇을 바꿨는지 2~3줄 설명한다.
- 리뷰는 서로 해도 되고, merge는 팀장이 한다.

### API가 바뀔 때
- 코드보다 `docs/api.md`를 먼저 수정하고 팀에 알린다.

## 처음 시작하는 방법
```bash
git clone https://github.com/ksj2002/저장소이름.git
cd 저장소이름
git checkout -b feature/back-이름-기능
# 작업 후
git add .
git commit -m "feat: ..."
git push origin feature/back-이름-기능
# GitHub에서 PR 생성
```
처음 한 번은 git 사용자 정보를 설정한다.
```bash
git config --global user.name "이름"
git config --global user.email "GitHub 가입 이메일"
```