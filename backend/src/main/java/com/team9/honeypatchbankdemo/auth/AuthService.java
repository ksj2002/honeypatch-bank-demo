package com.team9.honeypatchbankdemo.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    /** 로그인에 성공한 사용자 (DB의 실제 id, 표시 이름) */
    public record User(String id, String name) {}

    private final JdbcTemplate jdbc;

    public AuthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> login(String id, String password) {
        // ⚠️ 데모를 위해 "일부러" 취약하게 문자열을 이어붙인 쿼리 (명세 7번).
        //    실제 서비스에서는 PreparedStatement(?) + 비밀번호 해시를 써야 한다.
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND password='" + password + "'";

        try {
            List<User> rows = jdbc.query(sql,
                    (rs, rowNum) -> new User(rs.getString("id"), rs.getString("name")));
            return rows.stream().findFirst();
        } catch (DataAccessException e) {
            // o'brien 같은 입력은 따옴표 짝이 안 맞아 SQL 문법 오류가 난다.
            // 500 에러 대신 일반 로그인 실패(401)로 처리해야 명세 6번 '경계' 케이스를 통과한다.
            return Optional.empty();
        }
    }
}
