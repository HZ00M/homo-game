package com.homo.game.proxy;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@Log4j2
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
