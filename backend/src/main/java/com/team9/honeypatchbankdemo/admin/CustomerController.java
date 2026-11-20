package com.team9.honeypatchbankdemo.admin;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team9.honeypatchbankdemo.admin.CustomerRepository.Customer;
import com.team9.honeypatchbankdemo.common.SessionKeys;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * GET /api/admin/customers
 * 허니 세션이면 decoy_customers, 아니면 customers를 같은 JSON 모양으로 내려준다.
 */
@RestController
@RequestMapping("/api/admin")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> customers(HttpServletRequest request) {
        HttpSession session = request.getSession(false);   // 없으면 새로 만들지 않는다
        if (session == null || session.getAttribute(SessionKeys.LOGIN_USER) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다"));
        }

        boolean decoy = Boolean.TRUE.equals(session.getAttribute(SessionKeys.DECOY));
        List<Customer> customers = decoy
                ? customerRepository.findAllDecoy()
                : customerRepository.findAll();

        // 방어자 내부 로그 (서버 콘솔에만 남고 응답에는 드러나지 않는다)
        log.info("[DEFENDER] customers 조회 user={} source={} rows={}",
                session.getAttribute(SessionKeys.LOGIN_USER),
                decoy ? "decoy_customers" : "customers",
                customers.size());

        return ResponseEntity.ok(Map.of("customers", customers));
    }
}
