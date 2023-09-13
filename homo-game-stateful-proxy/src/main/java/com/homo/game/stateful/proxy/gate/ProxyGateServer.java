package com.homo.game.stateful.proxy.gate;

import com.homo.core.gate.DefaultGateServer;

public class ProxyGateServer extends DefaultGateServer {

    public ProxyGateServer(String name, int port) {
        super(name, port);
    }

    @Override
    public ProxyGateClient newClient(String addr, int port) {
        return new ProxyGateClient(this, addr + ":" + port);
    }

}
