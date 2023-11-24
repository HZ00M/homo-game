package com.homo.game.stateful.proxy;

import com.homo.core.gate.GateServerMgr;
import com.homo.core.gate.tcp.TcpGateDriver;
import com.homo.game.stateful.proxy.gate.ProxyLogicHandler;
import com.homo.game.stateful.proxy.gate.ProxyGateServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@Slf4j
@SpringBootApplication(scanBasePackages = "com.homo.game")
public class StatefulProxyApplication implements CommandLineRunner {
    @Autowired
    private GateServerMgr gateServerMgr;
    @Autowired
    private ProxyLogicHandler proxyLogicHandler;

    static CountDownLatch countDownLatch = new CountDownLatch(1);
    public static void main(String[] args) {
        SpringApplication.run(StatefulProxyApplication.class);
        log.debug("============================================");
        try {
            countDownLatch.await(); // 等待退出
        } catch (InterruptedException e) {
            log.error("main error :", e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        int port = 666;
        log.info("StatefulProxyApplication run!------------------------------------------");
        ProxyGateServer proxyGateServer = new ProxyGateServer(StatefulProxyApplication.class.getSimpleName(),port);
        gateServerMgr.startGateServer(proxyGateServer);
        TcpGateDriver tcpGate = (TcpGateDriver) gateServerMgr.getGateDriver();
        tcpGate.registerPostHandler(proxyLogicHandler);
    }
}
