package com.nexus.notification.infrastructure.messaging;

import com.nexus.notification.application.port.out.BrokerHealthProbePort;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaBrokerHealthProbe implements BrokerHealthProbePort {

    private static final Logger log = LoggerFactory.getLogger(KafkaBrokerHealthProbe.class);

    private final String bootstrapServers;
    private final int timeoutMs;

    public KafkaBrokerHealthProbe(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                                   @Value("${nexus.health.broker.timeout-ms:3000}") int timeoutMs) {
        this.bootstrapServers = bootstrapServers;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public boolean isHealthy() {
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, timeoutMs,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, timeoutMs);
        try (Admin admin = Admin.create(config)) {
            return !admin.describeCluster().nodes().get(timeoutMs, TimeUnit.MILLISECONDS).isEmpty();
        } catch (Exception e) {
            log.warn("Message broker health check failed: {}", e.toString());
            return false;
        }
    }
}
