package com.team9.honeypatchbankdemo.admin;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team9.honeypatchbankdemo.common.Mode;
import com.team9.honeypatchbankdemo.common.ModeHolder;

/** GET/PUT /api/mode : 데모용 서버 모드 조회/변경 (로그인 불필요) */
@RestController
@RequestMapping("/api/mode")
public class ModeController {

    private final ModeHolder modeHolder;

    public ModeController(ModeHolder modeHolder) {
        this.modeHolder = modeHolder;
    }

    @GetMapping
    public Map<String, String> get() {
        return Map.of("mode", modeHolder.get().name());
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> set(@RequestBody(required = false) Map<String, Object> body) {
        Object value = (body == null) ? null : body.get("mode");
        Mode mode;
        try {
            mode = Mode.valueOf(String.valueOf(value));   // null이나 없는 값이면 예외
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "잘못된 모드"));
        }
        modeHolder.set(mode);
        return ResponseEntity.ok(Map.of("mode", mode.name()));
    }

    /** 깨진 JSON, 배열 등 본문을 읽을 수 없을 때도 공통 에러 형식 { "message": ... } 으로 응답한다. (이 컨트롤러에만 적용) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable() {
        return ResponseEntity.badRequest().body(Map.of("message", "잘못된 모드"));
    }
}
