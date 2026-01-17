package com.kuneiform.infraestructure.config.graalvm;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(NativeCacheConfig.CaffeineHints.class)
public class NativeCacheConfig {

  static class CaffeineHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      // 1. Hint for 'SSSMSA' (from your first error)
      hints
          .reflection()
          .registerType(
              TypeReference.of("com.github.benmanes.caffeine.cache.SSSMSA"),
              hint ->
                  hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
                      .withField("FACTORY"));

      // 2. Hint for 'SSLSMSW' (from your current error)
      hints
          .reflection()
          .registerType(
              TypeReference.of("com.github.benmanes.caffeine.cache.SSLSMSW"),
              hint ->
                  hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
                      .withField("FACTORY"));
    }
  }
}
