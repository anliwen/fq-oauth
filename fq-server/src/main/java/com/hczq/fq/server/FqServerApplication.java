package com.hczq.fq.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.hczq.fq")
public class FqServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FqServerApplication.class, args);
    }

}
