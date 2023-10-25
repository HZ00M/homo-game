package com.homo.tcp.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootApplication()
public class GameStatefulProxyTestApplication implements CommandLineRunner {

    static CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        SpringApplication.run(GameStatefulProxyTestApplication.class);
        log.info("==============================");
        try {
            countDownLatch.await();
        }catch (Exception e){
            log.error("GameStatefulProxyTestApplication start error",e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("GameStatefulProxyTestApplication start");
    }
}
