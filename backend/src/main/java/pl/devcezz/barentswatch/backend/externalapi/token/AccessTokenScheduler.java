package pl.devcezz.barentswatch.backend.externalapi.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.devcezz.barentswatch.backend.Registry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;

@ApplicationScoped
class AccessTokenScheduler {

    Logger logger = LoggerFactory.getLogger(AccessTokenScheduler.class);

    @Inject
    @RestClient
    AccessTokenExternalApi accessTokenExternalApi;

    @Inject
    AccessTokenProperties accessTokenProperties;

    @Inject
    ObjectMapper objectMapper;

    @Scheduled(every = "${barents-watch.token.scheduled.every:30m}")
    void fetchAccessToken() throws JsonProcessingException {
        Form form = new Form()
                .param("client_id", accessTokenProperties.clientId)
                .param("scope", accessTokenProperties.scope)
                .param("client_secret", accessTokenProperties.clientSecret)
                .param("grant_type", accessTokenProperties.grantType);

        try {
            Token token = objectMapper.readValue(accessTokenExternalApi.fetchToken(form), Token.class);
            Registry.accessToken.set("Bearer " + token.value());
        } catch (WebApplicationException exception) {
            logger.error("Cannot access token from external API: {}", exception.getMessage());
            throw exception;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Token(@JsonProperty(value = "access_token") String value) {}
}
