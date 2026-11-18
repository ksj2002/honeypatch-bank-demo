# API 명세

> 이 문서가 **프론트와 백엔드 사이의 약속**이다. 개발은 이 문서를 기준으로 한다.
> 내용을 바꿔야 하면 **코드보다 이 문서를 먼저 수정**하고 팀에 알린다.

## 0. 공통 규칙

- 서버 주소: `http://localhost:8080` (Spring Boot, 포트 8080)
- 데이터 형식: **JSON** (`Content-Type: application/json`)
- 로그인 상태는 **서버 세션(쿠키)** 으로 유지한다.
  - 프론트의 **모든 `fetch`에 `credentials: "include"`** 를 넣는다. (로그인뿐 아니라 고객 목록 등 전부)
  - 프론트는 반드시 **`http://localhost:5500`** 으로 접속한다. `127.0.0.1`로 접속하면 쿠키가 안 붙는다.
  - 서버의 CORS 허용 주소는 `http://localhost:5500` 하나다.
- 에러 응답 공통 형식: `{ "message": "설명" }`
- 로그인 API와 모드 API는 **로그인 없이** 호출할 수 있다. 고객 목록은 로그인(세션)이 필요하다.

## 1. 서버 모드 (데모용 전환)

서버는 모드 하나를 전역 값으로 가진다. 시작할 때는 `VULNERABLE`이다.

| 모드 | 의미 |
|---|---|
| VULNERABLE | 취약 버전: 인젝션 공격이 통한다 |
| PATCHED | 일반 패치: 인젝션을 탐지하면 차단한다 |
| HONEY | 허니 패치: 인젝션을 탐지하면 가짜 관리자 세션으로 연결한다 |

- 모드를 바꿔도 **이미 만들어진 세션은 그대로**다. 모드를 바꾼 뒤에는 **로그아웃 후 다시 로그인**해서 테스트한다.

## 2. 모드 조회/변경

### GET /api/mode
응답 200
```json
{ "mode": "VULNERABLE" }
```

### PUT /api/mode
요청
```json
{ "mode": "HONEY" }
```
응답 200
```json
{ "mode": "HONEY" }
```
오류 400 (모드 값이 잘못됨)
```json
{ "message": "잘못된 모드" }
```

## 3. 로그인

### POST /api/login
요청
```json
{ "id": "admin", "password": "admin1234" }
```

**성공 200**
정상 로그인, 취약 모드에서 공격 성공, 허니 모드에서 공격 모두 **완전히 같은 모양**으로 응답한다.
```json
{ "success": true, "name": "관리자", "message": "관리자 로그인 성공" }
```

**실패 401** (계정 없음, 비밀번호 틀림)
```json
{ "success": false, "message": "아이디 또는 비밀번호가 올바르지 않습니다" }
```

**차단 403** (PATCHED 모드에서 인젝션 탐지)
```json
{ "success": false, "message": "허용되지 않는 입력입니다" }
```

**오류 400** (id 또는 password가 비어 있음)
```json
{ "message": "아이디와 비밀번호를 입력하세요" }
```

모드별 동작 (id 또는 password 에 인젝션 패턴이 있을 때)

| 모드 | 동작 |
|---|---|
| VULNERABLE | 검사 없이 취약 쿼리 실행 → 공격이 통하면 200, 실제 세션 |
| PATCHED | 탐지하면 403 |
| HONEY | 탐지하면 200, **허니 세션**(가짜 데이터용) 생성 |

## 4. 로그아웃

### POST /api/logout
응답 200
```json
{ "message": "로그아웃" }
```
세션을 무효화한다.

## 5. 고객 목록 (관리자 화면)

### GET /api/admin/customers
로그인 세션이 필요하다.

응답 200
```json
{
  "customers": [
    { "id": 1, "name": "홍길동", "balance": 1500000 },
    { "id": 2, "name": "김영희", "balance": 320000 }
  ]
}
```
미로그인 401
```json
{ "message": "로그인이 필요합니다" }
```

> 진짜 세션이면 실제 고객 데이터(`customers`), 허니 세션이면 가짜 데이터(`decoy_customers`)를 **같은 모양**으로 내려준다.
> 프론트는 진짜인지 가짜인지 구분하지 않고 받은 그대로 보여준다.

## 6. 검증 시나리오 (보고서 5번과 동일)

| 구분 | 입력 | 모드 | 기대 결과 |
|---|---|---|---|
| 정상 | id=`admin`, pw=`admin1234` | 전부 | 200, 실제 고객 데이터 |
| 위반 | id=`' OR '1'='1' --`, pw=`x` | VULNERABLE | 200, **실제** 데이터 노출 |
| 위반 | 위와 동일 | PATCHED | 403 차단 |
| 위반 | 위와 동일 | HONEY | 200, **가짜** 고객 데이터 |
| 경계 | id=`o'brien`, pw=`x` | 전부 | 401 (오탐 없이 일반 로그인 실패) |

---

## 7. 백엔드 구현 참고

### 기본 방침
- 모드는 서버 전역 값 하나(`ModeHolder`)로 관리한다.
- 인젝션 탐지와 모드별 분기는 Spring 필터에서 처리한다. (`auth/InjectionFilter`)
- 탐지는 **작은따옴표 하나만으로 판단하지 않는다.** (`o'brien`을 오탐하면 안 된다.) 탐지 방식은 백엔드 A가 정한다. (예: 항등식 패턴, SQL 주석 `--`, SQL 키워드 조합)
- id와 password **둘 다** 검사한다.
- 허니 세션 여부는 서버 세션에 저장하고, 고객 목록 API에서 이 값을 보고 진짜/가짜 데이터를 나눠서 준다.
- 진짜 세션과 허니 세션의 응답은 **같은 JSON 모양**이어야 한다. (필드명, 메시지 포함)

### 공유 클래스 (`common/`, 팀장 관리)
| 클래스 | 용도 |
|---|---|
| `Mode` | enum `VULNERABLE`, `PATCHED`, `HONEY` |
| `ModeHolder` | 현재 모드 보관. 백엔드 A는 `get()`으로 읽고, 백엔드 B의 `ModeController`는 `set()`으로 바꾼다 |
| `SessionKeys` | 세션 키 이름 상수. `LOGIN_USER`, `DECOY`(true면 허니 세션) |
| `CorsConfig` | `/api/**`에 대해 `http://localhost:5500` 허용, 쿠키 허용 |

백엔드 A가 세션에 `SessionKeys.DECOY`를 저장하고, 백엔드 B가 같은 키로 읽는다. **키 이름은 이 상수만 사용한다.**

### 파일 담당
| 담당 | 폴더 / 파일 |
|---|---|
| 백엔드 A | `auth/` (LoginController, LoginRequest/Response, AuthService, InjectionDetector, InjectionFilter) |
| 백엔드 B | `admin/` (CustomerController, CustomerRepository, ModeController), `schema.sql`, `data.sql` |
| 팀장 | `common/`, `build.gradle`, `application.properties`, `HoneypatchBankDemoApplication.java` |

### DB (H2 인메모리)
서버를 켤 때마다 `src/main/resources`의 `schema.sql`, `data.sql`이 실행되어 메모리 DB가 새로 만들어진다. (서버를 끄면 데이터는 사라진다.)

**users** (로그인용)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | VARCHAR(50) | 로그인 아이디 (PK) |
| password | VARCHAR(100) | 비밀번호 (데모라 평문 허용) |
| name | VARCHAR(50) | 표시 이름 |

초기 데이터: `admin` / `admin1234` / `관리자` (이미 `data.sql`에 들어 있음)

**customers** (실제 고객 데이터)
| 컬럼 | 타입 |
|---|---|
| id | BIGINT (PK) |
| name | VARCHAR(50) |
| balance | BIGINT |

**decoy_customers** (허니 세션용 가짜 데이터)
- `customers`와 **같은 구조, 같은 행 수**로 만든다.
- 이름과 잔액은 `customers`와 **다른 값**으로 채운다.
- 이름 형식과 잔액 자릿수는 진짜와 비슷하게 맞춘다.
- 값은 `data.sql`에 **고정값**으로 넣는다. **요청마다 랜덤 생성 금지.** (조회할 때마다 값이 바뀌면 가짜임이 드러난다.)

> `customers`, `decoy_customers`의 초기 데이터는 **백엔드 B**가 `data.sql`에 추가한다.
> (현재 `data.sql`에는 admin 계정만 있다. `schema.sql`에는 세 테이블이 이미 정의되어 있다.)

### 로그인 쿼리 (의도적으로 취약하게)
```java
"SELECT * FROM users WHERE id='" + id + "' AND password='" + password + "'"
```
이 쿼리는 데모를 위해 **일부러 취약하게** 작성한다. 실제 서비스에서는 PreparedStatement를 쓰고 비밀번호는 해시로 저장해야 한다.
JPA는 쓰지 않는다. (SQL을 직접 이어붙이기 위해 `JdbcTemplate`을 사용한다.)

### 분기 지점
1. **로그인 시점**: 모드에 따라 통과 / 403 차단 / 허니 세션 생성
2. **고객 목록 조회 시점**: 세션이 허니 표시면 `decoy_customers`, 아니면 `customers`

### 작업 순서 (의존성)
1. 백엔드 B: `data.sql`에 `customers`, `decoy_customers` 데이터를 먼저 추가해서 push한다.
2. 백엔드 A: 그동안 탐지 로직(`InjectionDetector`)과 로그인 컨트롤러 뼈대부터 작성한다.
3. DB가 올라오면 로그인 쿼리와 연결하고, **6. 검증 시나리오** 표로 테스트한다.

---

## 8. 프론트 구현 참고

### 파일 담당
| 담당 | 파일 |
|---|---|
| 프론트 A | `index.html`(로그인 화면), `css/login.css`, `js/login.js` |
| 프론트 B | `admin.html`(관리자 화면), `css/admin.css`, `js/admin.js` |
| 팀장 | `js/config.js` (API 주소) |

### 화면
- **로그인 화면**(`index.html`): 아이디/비밀번호 입력, 로그인 버튼, **모드 전환 UI**(데모용, `GET/PUT /api/mode` 사용)
  - 로그인 성공(200)이면 `message`("관리자 로그인 성공")를 보여주고 `admin.html`로 이동한다.
  - 실패(401), 차단(403)이면 응답의 `message`를 화면에 보여준다.
- **관리자 화면**(`admin.html`): 열릴 때 `GET /api/admin/customers`를 호출해서 고객 목록(이름, 잔액)을 표로 보여준다.
  - 401이면 `index.html`로 보낸다.
  - **허니 패치용 화면은 따로 없다.** 서버가 준 데이터를 그대로 보여준다.

### API 주소와 fetch
`js/config.js`에는 아래 한 줄만 있다.
```js
const API_BASE = "http://localhost:8080";
```
- HTML에서 **`config.js`를 다른 js보다 먼저** 불러온다.
  ```html
  <script src="js/config.js"></script>
  <script src="js/login.js"></script>
  ```
- 모든 요청에 `credentials: "include"`를 넣는다.
  ```js
  const res = await fetch(`${API_BASE}/api/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ id, password })
  });
  const data = await res.json();
  ```
  `GET` 요청에도 `credentials: "include"`를 넣는다.

### 백엔드가 준비되기 전
이 문서의 응답 JSON을 그대로 **mock(가짜 응답)** 으로 사용해서 먼저 화면을 만든다. 백엔드가 되면 mock 대신 실제 `fetch`로 바꾼다.

### 접속 주소
- 프론트는 **`http://localhost:5500`** 으로 접속한다. (`127.0.0.1` 금지)