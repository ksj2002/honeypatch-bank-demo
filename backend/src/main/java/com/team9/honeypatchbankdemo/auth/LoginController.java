package com.team9.honeypatchbankdemo.auth;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team9.honeypatchbankdemo.auth.InjectionFilter.Decision;
import com.team9.honeypatchbankdemo.common.SessionKeys;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class LoginController {

    /** 허니 세션에서 보여줄 이름 (data.sql의 admin 표시 이름과 맞춘다) */
    private static final String DECOY_USER_ID = "admin";
    private static final String DECOY_USER_NAME = "관리자";

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest http) {
        if (req == null || isBlank(req.id()) || isBlank(req.password())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "아이디와 비밀번호를 입력하세요"));
        }

        Decision decision = (Decision) http.getAttribute(InjectionFilter.DECISION_ATTR);

        // PATCHED 모드 + 인젝션 탐지 → 403 차단
        if (decision == Decision.BLOCK) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoginResponse.fail("허용되지 않는 입력입니다"));
        }

        // HONEY 모드 + 인젝션 탐지 → 쿼리는 실행하지 않고 허니 세션 생성.
        // 응답은 정상 로그인과 똑같이 만든다.
        if (decision == Decision.DECOY) {
            startSession(http, DECOY_USER_ID, true);
            return ResponseEntity.ok(LoginResponse.success(DECOY_USER_NAME));
        }

        // 그 외: 취약 쿼리로 로그인 시도
        Optional<AuthService.User> user = authService.login(req.id(), req.password());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponse.fail("아이디 또는 비밀번호가 올바르지 않습니다"));
        }

        startSession(http, user.get().id(), false);
        return ResponseEntity.ok(LoginResponse.success(user.get().name()));
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Map.of("message", "로그아웃");
    }

    /** 진짜/허니 세션 모두 같은 방식으로 만든다. (DECOY 값만 다름) */
    private void startSession(HttpServletRequest http, String userId, boolean decoy) {
        HttpSession old = http.getSession(false);
        if (old != null) {
            old.invalidate();                  // 세션 고정 공격 방지: 로그인할 때마다 새 세션
        }
        HttpSession session = http.getSession(true);
        session.setAttribute(SessionKeys.LOGIN_USER, userId);
        session.setAttribute(SessionKeys.DECOY, decoy);   // Boolean으로 저장 → 백엔드 B가 읽음
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
