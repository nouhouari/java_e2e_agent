package com.sicpa.fxagent.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.json.JavalinJackson;
import io.javalin.json.JsonMapper;

public final class JsonMapperFactory {
    private JsonMapperFactory() {}

    public static JsonMapper create() {
        JavalinJackson mapper = new JavalinJackson();
        mapper.updateMapper(om -> {
            om.registerModule(new JavaTimeModule());
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        });
        return mapper;
    }
}
