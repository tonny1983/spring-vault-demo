package cc.tonny.microservice.springvault.config;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.cloud.vault.config.VaultConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;

import javax.annotation.PostConstruct;

@Configuration
@Log4j2
@AllArgsConstructor
public class VaultConfiguration {
    private final SecretLeaseContainer leaseContainer;
    private final RestartEndpoint restartEndpoint;

    @PostConstruct
    private void postConstruct() {
        final RequestedSecret secretPath = RequestedSecret.renewable("database/creds/readwrite");

        leaseContainer.addLeaseListener(secretLeaseEvent -> {

            if (secretLeaseEvent.getSource() == secretPath) {
                log.info("Secret event for {} : {}", secretPath, secretLeaseEvent);
                if (secretLeaseEvent instanceof SecretLeaseExpiredEvent
                        && secretLeaseEvent.getSource().getMode() == RequestedSecret.Mode.RENEW) {
                    log.error("Database lease expired. The application will be rebooted.");
                    restartEndpoint.restart();
                }
            }
        });

        leaseContainer.addRequestedSecret(secretPath);
    }

    @Bean
    public VaultConfigurer configurer() {

        return configurer -> configurer
                .add(RequestedSecret.rotating("database/creds/readwrite"));
    }
}
