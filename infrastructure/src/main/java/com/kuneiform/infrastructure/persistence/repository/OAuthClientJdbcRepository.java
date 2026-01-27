package com.kuneiform.infrastructure.persistence.repository;

import com.kuneiform.infrastructure.persistence.entity.OAuthClientEntity;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for OAuth client persistence. Provides basic CRUD operations and
 * custom query methods.
 */
@Repository
public interface OAuthClientJdbcRepository extends CrudRepository<OAuthClientEntity, Long> {

  Optional<OAuthClientEntity> findByClientId(String clientId);

  void deleteByClientId(String clientId);
}
