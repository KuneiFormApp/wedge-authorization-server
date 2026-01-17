package com.kuneiform.infraestructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class Jackson3RedisSerializerTest {

  private Jackson3RedisSerializer<TestPojo> serializer;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = JsonMapper.builder().build();
    serializer = new Jackson3RedisSerializer<>(mapper, TestPojo.class);
  }

  @Test
  void shouldSerializeObject() {
    TestPojo pojo = new TestPojo("test", 123);
    byte[] bytes = serializer.serialize(pojo);

    assertThat(bytes).isNotEmpty();
    String json = new String(bytes, StandardCharsets.UTF_8);
    assertThat(json).contains("\"name\":\"test\"").contains("\"value\":123");
  }

  @Test
  void shouldDeserializeObject() {
    String json = "{\"name\":\"test\",\"value\":123}";
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

    TestPojo pojo = serializer.deserialize(bytes);

    assertThat(pojo).isNotNull();
    assertThat(pojo.getName()).isEqualTo("test");
    assertThat(pojo.getValue()).isEqualTo(123);
  }

  @Test
  void shouldReturnEmptyBytesForNullSerialization() {
    byte[] bytes = serializer.serialize(null);
    assertThat(bytes).isEmpty();
  }

  @Test
  void shouldReturnNullForNullDeserialization() {
    TestPojo pojo = serializer.deserialize(null);
    assertThat(pojo).isNull();
  }

  @Test
  void shouldReturnNullForEmptyBytesDeserialization() {
    TestPojo pojo = serializer.deserialize(new byte[0]);
    assertThat(pojo).isNull();
  }

  @Test
  void shouldThrowSerializationExceptionOnInvalidJson() {
    byte[] invalidJson = "{invalid-json}".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> serializer.deserialize(invalidJson))
        .isInstanceOf(SerializationException.class)
        .hasMessageContaining("Error deserializing");
  }

  // Simple POJO for testing
  static class TestPojo {
    private String name;
    private int value;

    public TestPojo() {}

    public TestPojo(String name, int value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getValue() {
      return value;
    }

    public void setValue(int value) {
      this.value = value;
    }
  }
}
