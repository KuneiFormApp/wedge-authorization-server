package com.kuneiform.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MfaDataTest {

  @Test
  void shouldCreateMfaDataUsingBuilder() {
    String mfaKeyId = "issuer:user";
    String secret = "secret123";

    MfaData mfaData =
        MfaData.builder().twoFaRegistered(true).mfaKeyId(mfaKeyId).mfaSecret(secret).build();

    assertThat(mfaData.isTwoFaRegistered()).isTrue();
    assertThat(mfaData.getMfaKeyId()).isEqualTo(mfaKeyId);
    assertThat(mfaData.getMfaSecret()).isEqualTo(secret);
  }

  @Test
  void shouldCreateMfaDataUsingConstructor() {
    MfaData mfaData = new MfaData(false, "id", "sec");
    assertThat(mfaData.isTwoFaRegistered()).isFalse();
  }
}
