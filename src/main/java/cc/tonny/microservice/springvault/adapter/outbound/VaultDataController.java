package cc.tonny.microservice.springvault.adapter.outbound;

import cc.tonny.microservice.springvault.domain.VaultData;
import cc.tonny.microservice.springvault.repository.VaultDataRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class VaultDataController {
    private final VaultDataRepository repository;

    @GetMapping("/")
    public Flux<VaultData> getAllVaultData() {
        return repository.findAll();
    }

    @PostMapping("/")
    public Mono<VaultData> addVaultData() {
        return repository.save(new VaultData(System.currentTimeMillis(), Thread.currentThread().getName()));
    }
}
