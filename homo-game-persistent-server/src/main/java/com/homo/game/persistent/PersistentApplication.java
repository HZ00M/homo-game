package com.homo.game.persistent;

import com.homo.service.dirty.anotation.DirtyLandingServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootApplication
//@MapperScan("com.homo")
@DirtyLandingServer
//@ComponentScan("com.homo")
public class PersistentApplication implements CommandLineRunner {
    static CountDownLatch countDownLatch = new CountDownLatch(1);
    public static void main(String[] args) {
        SpringApplication.run(PersistentApplication.class);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("PersistentApplication running");
    }
}
