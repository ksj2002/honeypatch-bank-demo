package com.team9.honeypatchbankdemo.auth;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.team9.honeypatchbankdemo.common.Mode;
import com.team9.honeypatchbankdemo.common.ModeHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
// Spring Boot 4는 Jackson 3을 쓴다. 컴파일 오류가 나면 아래 줄을
// import com.fasterxml.jackson.databind.json.JsonMapper; 로 바꾼다.
import tools.jackson.databind.json.JsonMapper;

/**
 * POST /api/login 요청에서 인젝션을 탐지하고, 모드에 따라 처리 방법(Decision)을 정한다.
 *
 * 필터가 직접 응답을 쓰지 않고, 결정만 요청 속성에 담아 컨트롤러로 넘긴다.
 * 이유:
 *  1) CORS 헤더는 Spring MVC(CorsConfig)가 컨트롤러 단계에서 붙인다.
 *     필터에서 바로 응답하면 CORS 헤더가 빠져서 브라우저가 응답을 못 읽는다.
 *  2) 허니 응답이 정상 로그인과 "같은 코드 경로"로 만들어져야
 *     헤더/JSON이 바이트 단위까지 같아져서 공격자가 구분할 수 없다.
 */
@Component
public class InjectionFilter extends OncePerRequestFilter {

    public static final String DECISION_ATTR = "injection.decision";

    public enum Decision {
        PASS,   // 그대로 로그인 처리 (취약 쿼리 실행)
        BLOCK,  // PATCHED: 403 차단
        DECOY   // HONEY: 가짜(허니) 세션으로 연결
    }

    private final InjectionDetector detector;
    private final ModeHolder modeHolder;
    private final JsonMapper mapper = new JsonMapper();

    public InjectionFilter(InjectionDetector detector, ModeHolder modeHolder) {
        this.detector = detector;
        this.modeHolder = modeHolder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 로그인 POST만 검사 (CORS 사전 요청 OPTIONS 등은 그냥 통과)
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/login".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 요청 본문은 한 번만 읽을 수 있으므로 복사해 두고, 컨트롤러가 다시 읽을 수 있게 감싼다.
        CachedBodyRequest wrapped = new CachedBodyRequest(request);
        wrapped.setAttribute(DECISION_ATTR, decide(wrapped.getBody()));
        chain.doFilter(wrapped, response);
    }

    private Decision decide(byte[] body) {
        Mode mode = modeHolder.get();
        if (mode == Mode.VULNERABLE) {
            return Decision.PASS;              // 취약 모드는 검사하지 않는다
        }

        LoginRequest req;
        try {
            req = mapper.readValue(body, LoginRequest.class);
        } catch (Exception e) {
            return Decision.PASS;              // JSON이 깨졌으면 컨트롤러가 400 처리
        }
        if (req == null) {
            return Decision.PASS;
        }

        boolean attack = detector.isInjection(req.id()) || detector.isInjection(req.password());
        if (!attack) {
            return Decision.PASS;
        }
        return (mode == Mode.PATCHED) ? Decision.BLOCK : Decision.DECOY;
    }

    /** 본문을 메모리에 보관해서 여러 번 읽을 수 있게 하는 요청 래퍼 */
    static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        byte[] getBody() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return in.read(); }
                @Override public int read(byte[] b, int off, int len) { return in.read(b, off, len); }
                @Override public boolean isFinished() { return in.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
