package cc.tonny.microservice.springvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@SpringBootApplication
@RefreshScope
public class SpringVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringVaultApplication.class, args);
    }

}
