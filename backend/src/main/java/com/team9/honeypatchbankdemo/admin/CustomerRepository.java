package com.team9.honeypatchbankdemo.admin;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 고객 목록 조회.
 * 진짜/가짜 테이블은 구조가 같으므로 같은 쿼리, 같은 매퍼를 쓴다. (테이블 이름만 다름)
 */
@Repository
public class CustomerRepository {

    /** 고객 한 명. 진짜(customers)와 가짜(decoy_customers) 모두 이 모양으로 내려간다. */
    public record Customer(long id, String name, long balance) {
    }

    private static final RowMapper<Customer> MAPPER = (rs, rowNum) ->
            new Customer(rs.getLong("id"), rs.getString("name"), rs.getLong("balance"));

    private final JdbcTemplate jdbcTemplate;

    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 실제 고객 데이터 */
    public List<Customer> findAll() {
        return jdbcTemplate.query("SELECT id, name, balance FROM customers ORDER BY id", MAPPER);
    }

    /** 허니 세션용 가짜 고객 데이터 */
    public List<Customer> findAllDecoy() {
        return jdbcTemplate.query("SELECT id, name, balance FROM decoy_customers ORDER BY id", MAPPER);
    }
}
