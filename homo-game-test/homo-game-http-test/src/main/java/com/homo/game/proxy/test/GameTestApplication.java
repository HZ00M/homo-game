package com.homo.game.proxy.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootApplication
public class GameTestApplication implements CommandLineRunner {
    static CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        SpringApplication.run(GameTestApplication.class);
        log.debug("============================================");
        try {
            countDownLatch.await(); // 等待退出
        } catch (InterruptedException e) {
            log.error("main error :", e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("GameTestApplication run!------------------------------------------");
    }
}
