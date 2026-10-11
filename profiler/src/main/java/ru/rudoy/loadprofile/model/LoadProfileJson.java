package ru.rudoy.loadprofile.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Читает и пишет профиль в JSON; профиль хранится как читаемый артефакт, поэтому с отступами. */
public final class LoadProfileJson {

    private final ObjectMapper json = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public String write(LoadProfile profile) {
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("cannot serialize profile", e);
        }
    }

    public LoadProfile read(String text) {
        try {
            return json.readValue(text, LoadProfile.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("profile is not valid JSON", e);
        }
    }
}
