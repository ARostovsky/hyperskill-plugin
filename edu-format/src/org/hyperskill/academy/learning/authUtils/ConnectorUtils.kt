package org.hyperskill.academy.learning.authUtils

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.json.JsonMapper

object ConnectorUtils {
  fun createMapper(): ObjectMapper {
    return JsonMapper.builder()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
      .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
      .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
      .disable(MapperFeature.AUTO_DETECT_FIELDS)
      .disable(MapperFeature.AUTO_DETECT_GETTERS)
      .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
      .disable(MapperFeature.AUTO_DETECT_SETTERS)
      .build()
  }

}