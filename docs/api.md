# API 명세

- 서버: http://localhost:8080
- 데이터 형식: JSON (Content-Type: application/json)
- 로그인 상태: 서버 세션(쿠키). 프론트는 fetch에 `credentials: "include"` 필수
- 에러 공통 형식: { "message": "설명" }

## 서버 모드 (데모용 전환)
| 모드 | 의미 |
|---|---|
| VULNERABLE | 취약 버전: 인젝션 공격이 통함 |
| PATCHED | 일반 패치: 인젝션 탐지 시 차단 |
| HONEY | 허니 패치: 인젝션 탐지 시 가짜 관리자 세션으로 연결 |

## 1. 모드 조회/변경
### GET /api/mode
응답 200: { "mode": "VULNERABLE" }

### PUT /api/mode
요청: { "mode": "HONEY" }
응답 200: { "mode": "HONEY" }
오류 400: { "message": "잘못된 모드" }

## 2. 로그인
### POST /api/login
요청: { "id": "admin", "password": "admin1234" }

성공 200 (정상 로그인 / 취약 모드 공격 성공 / 허니 모드 공격 모두 동일한 모양):
{ "success": true, "name": "관리자", "message": "관리자 로그인 성공" }

실패 401 (계정 없음, 비밀번호 틀림):
{ "success": false, "message": "아이디 또는 비밀번호가 올바르지 않습니다" }

차단 403 (PATCHED 모드에서 인젝션 탐지):
{ "success": false, "message": "허용되지 않는 입력입니다" }

## 3. 로그아웃
### POST /api/logout
응답 200: { "message": "로그아웃" }

## 4. 고객 목록 (관리자 화면)
### GET /api/admin/customers
로그인 세션 필요.

응답 200:
{
  "customers": [
    { "id": 1, "name": "홍길동", "balance": 1500000 },
    { "id": 2, "name": "김영희", "balance": 320000 }
  ]
}
미로그인 401: { "message": "로그인이 필요합니다" }

※ 진짜 세션이면 실제 고객 데이터, 허니 세션이면 가짜 데이터가 같은 모양으로 내려온다.

## 검증 시나리오 (보고서 5번과 동일)
| 구분 | 입력 | 모드 | 기대 결과 |
|---|---|---|---|
| 정상 | admin / admin1234 | 전부 | 200, 실제 고객 데이터 |
| 위반 | id=`' OR '1'='1' --`, pw=x | VULNERABLE | 200, 실제 데이터 노출 |
| 위반 | 위와 동일 | PATCHED | 403 차단 |
| 위반 | 위와 동일 | HONEY | 200, 가짜 고객 데이터 |
| 경계 | id=`o'brien`, pw=x | 전부 | 401 (오탐 없이 일반 실패) |

## 백엔드 구현 참고

### 기본 방침
- 모드(VULNERABLE / PATCHED / HONEY)는 서버 전역 값 하나로 관리한다.
- 인젝션 탐지와 모드별 분기는 Spring 필터에서 처리한다.
- 인젝션 탐지는 작은따옴표 존재만으로 판단하지 않는다. (`o'brien`은 오탐하면 안 됨)
- 허니 세션 여부는 서버 세션에 저장하고, 고객 목록 API에서 이 값을 보고 진짜/가짜 데이터를 나눠서 준다.
- 진짜 세션과 허니 세션의 응답은 **같은 JSON 모양**이어야 한다. (필드명, 메시지 포함)

### DB (H2 인메모리)
앱 시작 시 `src/main/resources`의 `schema.sql`, `data.sql`로 테이블과 데이터를 자동 생성한다.

**users** (로그인용)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | VARCHAR | 로그인 아이디 (PK) |
| password | VARCHAR | 비밀번호 (데모라 평문 허용) |
| name | VARCHAR | 표시 이름 |

초기 데이터: `admin` / `admin1234` / `관리자`

**customers** (실제 고객 데이터)
| 컬럼 | 타입 |
|---|---|
| id | BIGINT (PK) |
| name | VARCHAR |
| balance | BIGINT |

**decoy_customers** (허니 세션용 가짜 데이터)
- customers와 같은 구조, 같은 행 수로 만든다.
- 이름과 잔액은 customers와 다른 값으로 채운다.
- 이름 형식과 잔액 자릿수는 진짜와 비슷하게 맞춘다.
- 값은 `data.sql`에 **고정값**으로 넣는다. 요청마다 랜덤 생성 금지
  (조회할 때마다 값이 바뀌면 가짜임이 드러남).

### 로그인 쿼리 (의도적으로 취약하게)
```java
"SELECT * FROM users WHERE id='" + id + "' AND password='" + password + "'"
```
※ 이 쿼리는 데모를 위해 일부러 취약하게 작성한다. 실제 서비스에서는 PreparedStatement를 쓰고 비밀번호는 해시로 저장해야 한다.

### 분기 지점
1. **로그인 시점**: 모드에 따라 통과 / 403 차단 / 허니 세션 생성
2. **고객 목록 조회 시점**: 세션이 허니 표시면 `decoy_customers`, 아니면 `customers`

### 작업 순서 (의존성)
1. 백엔드 B: `schema.sql`, `data.sql`을 먼저 작성하고 push한다.
2. 백엔드 A: DB가 올라오기 전에는 탐지 필터와 로그인 컨트롤러 뼈대를 먼저 작성한다.
3. DB가 올라오면 로그인 쿼리와 연결하고, 검증 시나리오(위 표)로 테스트한다.

## 프론트 구현 참고
- 화면: 로그인 화면(+ 모드 전환 UI), 관리자 화면(고객 목록)
- 백엔드가 준비되기 전엔 위 응답 JSON을 mock으로 사용
- CORS: 프론트를 Live Server(5500 등)로 띄우면 백에서 허용 설정 필요