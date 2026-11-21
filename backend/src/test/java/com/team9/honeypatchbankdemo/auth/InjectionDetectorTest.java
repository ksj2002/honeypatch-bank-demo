package com.team9.honeypatchbankdemo.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 명세 6번 검증 시나리오 기준 탐지 로직 테스트 (Spring 없이 바로 실행) */
class InjectionDetectorTest {

    private final InjectionDetector detector = new InjectionDetector();

    @Test
    void 정상_입력은_탐지하지_않는다() {
        assertFalse(detector.isInjection("admin"));
        assertFalse(detector.isInjection("admin1234"));
    }

    @Test
    void 명세의_공격_입력을_탐지한다() {
        assertTrue(detector.isInjection("' OR '1'='1' --"));
    }

    @Test
    void 따옴표와_주석_조합을_탐지한다() {
        assertTrue(detector.isInjection("admin'--"));
    }

    @Test
    void 따옴표_하나만으로는_오탐하지_않는다() {
        assertFalse(detector.isInjection("o'brien"));
    }
}
