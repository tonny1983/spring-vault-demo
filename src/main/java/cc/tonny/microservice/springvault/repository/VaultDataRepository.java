package cc.tonny.microservice.springvault.repository;

import cc.tonny.microservice.springvault.domain.VaultData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface VaultDataRepository extends ReactiveMongoRepository<VaultData, Long> {
}
