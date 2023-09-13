package com.homo.game.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootApplication
public class CommonProxyApplication implements CommandLineRunner {
    static CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        SpringApplication.run(CommonProxyApplication.class);
        log.debug("============================================");
        try {
            countDownLatch.await(); // 等待退出
        } catch (InterruptedException e) {
            log.error("main error :", e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("CommonProxyApplication run!------------------------------------------");
    }
}
