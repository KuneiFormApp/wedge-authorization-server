package com.kuneiform.infraestructure.persistence.repository;

import com.kuneiform.infraestructure.persistence.entity.OAuthClientEntity;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for OAuth client persistence. Provides basic CRUD operations and
 * custom query methods.
 */
@Repository
public interface OAuthClientJdbcRepository extends CrudRepository<OAuthClientEntity, Long> {

  /**
   * Finds an OAuth client by its client ID.
   *
   * @param clientId the client ID to search for
   * @return Optional containing the entity if found
   */
  Optional<OAuthClientEntity> findByClientId(String clientId);

  /**
   * Deletes an OAuth client by its client ID.
   *
   * @param clientId the client ID to delete
   */
  void deleteByClientId(String clientId);
}
