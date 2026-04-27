package kr.bang9;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("kr.bang9.**.dao")
public class Bang9Application {

    public static void main(String[] args) {
        SpringApplication.run(Bang9Application.class, args);
    }
}
