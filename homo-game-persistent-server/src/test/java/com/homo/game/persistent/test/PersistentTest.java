package com.homo.game.persistent.test;

import com.homo.core.facade.storege.dirty.DirtyDriver;
import com.homo.core.facade.storege.landing.DBDataHolder;
import com.homo.game.persitent.PersistentApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = PersistentApplication.class)
@Slf4j
public class PersistentTest {
    @Autowired
    DirtyDriver dirtyDriver;
    @Autowired
    DBDataHolder dbDataHolder;
    @Test
    public void start() throws InterruptedException {
        Thread.currentThread().join();
    }
}
