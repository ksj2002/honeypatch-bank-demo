package com.team9.honeypatchbankdemo.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 로그인 응답.
 * 정상 로그인 / 취약 모드 공격 성공 / 허니 모드 공격 모두 success()로 만들어서
 * 응답 모양이 완전히 같도록 한다. (명세 3번)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)   // 실패 응답에는 name 필드를 넣지 않음
public record LoginResponse(boolean success, String name, String message) {

    public static LoginResponse success(String name) {
        return new LoginResponse(true, name, name + " 로그인 성공");
    }

    public static LoginResponse fail(String message) {
        return new LoginResponse(false, null, message);
    }
}
