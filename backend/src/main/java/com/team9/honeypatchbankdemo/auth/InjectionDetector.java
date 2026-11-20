package com.team9.honeypatchbankdemo.auth;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * SQL 인젝션 탐지.
 *
 * 탐지 기준
 * 1) 작은따옴표(')가 없으면 탐지하지 않는다.
 *    로그인 쿼리에서 입력값은 '...' 안에 들어가므로, 따옴표 없이는 문자열 밖으로 빠져나올 수 없다.
 * 2) 따옴표가 있어도 그것만으로는 탐지하지 않는다. (o'brien 오탐 방지)
 *    따옴표 + 아래 SQL 문법 요소가 "함께" 있을 때만 공격으로 본다.
 *      - SQL 주석:            --   /*
 *      - 따옴표 직후 논리연산: ' OR ...   ' AND ...
 *      - UNION SELECT
 *      - 문장 구분자:         ;
 */
@Component
public class InjectionDetector {

    private static final Pattern SQL_META = Pattern.compile(
            "--"                                   // 한 줄 주석
            + "|/\\*"                              // 블록 주석
            + "|'\\s*(or|and)\\b"                  // ' 바로 뒤에 OR / AND
            + "|\\bunion\\b\\s+(all\\s+)?select\\b"
            + "|;",                                // 문장 구분자
            Pattern.CASE_INSENSITIVE);

    public boolean isInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        if (input.indexOf('\'') < 0) {
            return false;
        }
        return SQL_META.matcher(input).find();
    }
}
