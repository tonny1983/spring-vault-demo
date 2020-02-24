package cc.tonny.microservice.springvault.config;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.cloud.vault.config.VaultConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Log4j2
@AllArgsConstructor
public class VaultConfiguration {
    private final SecretLeaseContainer leaseContainer;
    private final RefreshEndpoint refreshEndpoint;
    private final StandardEnvironment environment;

    @PostConstruct
    private void postConstruct() {
        final String secretPath = "database/creds/readwrite";
        leaseContainer.addLeaseListener(secretLeaseEvent -> {

            if (secretLeaseEvent.getSource().getPath().equals(secretPath)) {
                log.info("Secret event for {} : {}", secretPath, secretLeaseEvent);
                log.info("Current Username {} Password {}",
                        environment.getProperty("mongodb.username"),
                        environment.getProperty("mongodb.password"));
                if (secretLeaseEvent instanceof SecretLeaseExpiredEvent
                        && secretLeaseEvent.getSource().getMode() == RequestedSecret.Mode.RENEW) {
                    log.error("Database lease expired. Replace RENEW for expired one with ROTATE.");
                    leaseContainer.requestRotatingSecret(secretPath);
                } else if (secretLeaseEvent instanceof SecretLeaseCreatedEvent
                        && secretLeaseEvent.getSource().getMode() == RequestedSecret.Mode.ROTATE) {
                    var credentials = ((SecretLeaseCreatedEvent) secretLeaseEvent).getSecrets();
                    var username = credentials.get("username");
                    var password = credentials.get("password");
                    log.info("NEW username {}, NEW password {}", username, password);
                    Map<String, Object> map = new HashMap<>();
                    map.put("mongodb.username", username);
                    map.put("mongodb.password", password);
                    MutablePropertySources propertySources = environment.getPropertySources();
                    propertySources.remove("newValue");
                    propertySources
                            .addFirst(new MapPropertySource("newValue", map));
                    refreshEndpoint.refresh();
                }
            }
        });
    }

    @Bean
    public VaultConfigurer configurer() {

        return configurer -> configurer
                .add(RequestedSecret.rotating("database/creds/readwrite"));
    }
}
