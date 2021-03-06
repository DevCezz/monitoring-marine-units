package pl.devcezz.barentswatch.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public final class AppConfig {

    @Produces
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}