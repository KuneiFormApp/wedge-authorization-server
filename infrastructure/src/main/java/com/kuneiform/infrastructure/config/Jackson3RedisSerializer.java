package com.kuneiform.infrastructure.config;

import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class Jackson3RedisSerializer<T> implements RedisSerializer<T> {

  private final ObjectMapper mapper;
  private final Class<T> type;

  public Jackson3RedisSerializer(ObjectMapper mapper, Class<T> type) {
    this.mapper = mapper;
    this.type = type;
  }

  @Override
  public byte[] serialize(T t) throws SerializationException {
    if (t == null) {
      return new byte[0];
    }
    try {
      return mapper.writeValueAsString(t).getBytes(StandardCharsets.UTF_8);
    } catch (JacksonException e) {
      throw new SerializationException("Error serializing object via Jackson 3", e);
    }
  }

  @Override
  public T deserialize(byte[] bytes) throws SerializationException {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    try {
      String json = new String(bytes, StandardCharsets.UTF_8);
      return mapper.readValue(json, type);
    } catch (JacksonException e) {
      throw new SerializationException("Error deserializing object via Jackson 3", e);
    }
  }
}
