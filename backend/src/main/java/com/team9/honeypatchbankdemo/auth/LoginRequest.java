package com.team9.honeypatchbankdemo.auth;

/** POST /api/login 요청 본문: { "id": "...", "password": "..." } */
public record LoginRequest(String id, String password) {}
