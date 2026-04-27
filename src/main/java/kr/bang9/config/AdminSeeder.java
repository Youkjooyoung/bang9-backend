package kr.bang9.config;

import kr.bang9.user.dao.UserDao;
import kr.bang9.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private static final String ADMIN_EMAIL = "admin@bang9.local";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String ADMIN_NICKNAME = "admin";

    @Bean
    @Order(1)
    public ApplicationRunner seedAdminAccount(UserDao userDao, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userDao.existsByEmail(ADMIN_EMAIL)) {
                log.info("[AdminSeeder] admin 계정이 이미 존재합니다. 시드를 건너뜁니다.");
                return;
            }
            User admin = User.builder()
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .nickname(ADMIN_NICKNAME)
                .name("관리자")
                .phone("010-0000-0000")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender("OTHER")
                .role("ADMIN")
                .status("ACTIVE")
                .build();
            userDao.insertUser(admin);
            log.info("[AdminSeeder] admin 계정 생성 완료: email={}", ADMIN_EMAIL);
        };
    }
}
