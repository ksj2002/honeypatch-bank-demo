package com.team9.honeypatchbankdemo.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.team9.honeypatchbankdemo.common.SessionKeys;

/** 고객 목록 진짜/가짜 분기 테스트 (로그인 없이 세션을 직접 만들어 확인) */
@SpringBootTest
class AdminApiTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;

    private String customers(boolean decoy) throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.LOGIN_USER, "admin");
        session.setAttribute(SessionKeys.DECOY, decoy);
        return mvc.perform(get("/api/admin/customers").session(session))
                .andExpect(jsonPath("$.customers.length()").value(10))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void 진짜_세션은_실제_고객_데이터를_받는다() throws Exception {
        assertThat(customers(false)).contains("\"name\":\"홍길동\"");
    }

    @Test
    void 허니_세션은_가짜_고객_데이터를_받는다() throws Exception {
        assertThat(customers(true)).contains("\"name\":\"김민수\"");
    }

    @Test
    void 가짜_데이터에_실제_고객_정보가_없다() throws Exception {
        String decoy = customers(true);
        jdbc.queryForList("SELECT name, balance FROM customers").forEach(row -> {
            assertThat(decoy).doesNotContain("\"name\":\"" + row.get("NAME") + "\"");
            assertThat(decoy).doesNotContain("\"balance\":" + row.get("BALANCE") + "}");
        });
    }
}
