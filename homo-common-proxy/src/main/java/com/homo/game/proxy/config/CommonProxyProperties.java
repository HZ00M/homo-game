package com.homo.game.proxy.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

@Data
@Configuration
public class CommonProxyProperties {
    @Value("${reactor.netty.max.connection:10000}")
    private int maxConnectionCount;

    @Value("${homo.sentinel.datasource.namespace:homo_common_proxy}")
    private String datasourceNamespace;

//    @Value("${homo.sentinel.datasource.flowRuleKey:flowRules}")
//    private String flowRuleKey ;

    @Value("${homo.common.httpForward.flow.control.keys:{}}")
    private Set<String> flowControlKeys;

    @Value("${homo.common.httpForward.flow.control.keys:{}}")
    private Set<String> ignoreCheckTokenKeys;
    /**
     * {
     *     "http-test-service": "http://http-test-service:33333",
     *     "official": "http://tpf-official.tpf-inner-login:30031/official",
     *     "login": "http://127.0.0.1:996/tpf-login",
     *     "old-proxy": "http://proxy-http.tpf:20010",
     *     "wuhui-delivery": "http://order-service.wuhui-dev-shuling:31004",
     *     "platform-card": "http://cardxytx.firewick.net",
     *     "share": "http://10.0.3.29:34567/monitor",
     *     "share2": "http://10.0.3.29:34567/monitor2",
     *     "pay": "http://pay.ehijoy.com",
     *     "yidun": "http://open-yb.dun.163.com/api/open/v1/risk/doubtful/check"
     * }
     */
    @Value("#{${homo.common.pathForward.server.path.map}}")
    /**
     * {
     *     "http-test-service": "3333",
     *     "login-service-http": "http://http-test-service:31505"
     * }
     */
    private Map<String,String> forwardUrlMap;
    @Value("#{${homo.common.postForward.server.port.map}}")
    private Map<String, String> serverPortMap;



}
