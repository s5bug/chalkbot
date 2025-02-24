package tf.bug.chalkbot.overpass;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class OverpassClient {

    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    public OverpassClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public Mono<OverpassResponse> request(String query) {
        return httpClient
            .post()
            .uri("https://z.overpass-api.de/api/interpreter")
            .send(ByteBufFlux.fromString(Mono.just(query), StandardCharsets.UTF_8, ByteBufAllocator.DEFAULT))
            .responseContent()
            .aggregate()
            .asString(StandardCharsets.UTF_8)
            .flatMap(s -> Mono.fromCallable(() -> objectMapper.readValue(s, OverpassResponse.class)).subscribeOn(Schedulers.boundedElastic()));
    }

    public String getName(OverpassElement element, Locale locale) {
        String langCode = locale.getLanguage();
        String nameWithLang = "name:" + langCode;
        String localName = element.getTags().get("name");
        return element.getTags().getOrDefault(nameWithLang, localName);
    }

}
