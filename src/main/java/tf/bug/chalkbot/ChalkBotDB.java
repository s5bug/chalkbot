package tf.bug.chalkbot;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.postgresql.api.PostgresqlResult;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import net.time4j.tz.Timezone;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

public class ChalkBotDB {

    private final PostgresqlConnection connection;

    public static Mono<ChalkBotDB> create(
        String dbHost,
        int dbPort,
        String dbUsername,
        String dbPassword,
        String dbDatabase
    ) {
        final PostgresqlConnectionConfiguration dbOptions = PostgresqlConnectionConfiguration.builder()
            .host(dbHost)
            .port(dbPort)
            .username(dbUsername)
            .password(dbPassword)
            .database(dbDatabase)
            .build();
        final PostgresqlConnectionFactory connectionFactory = new PostgresqlConnectionFactory(dbOptions);

        return connectionFactory.create().map(ChalkBotDB::new);
    }

    private ChalkBotDB(PostgresqlConnection connection) {
        this.connection = connection;
    }

    public PostgresqlConnection getConnection() {
        return this.connection;
    }

    public Mono<Void> createGuild(BigInteger guildId) {
        return connection
            .createStatement("INSERT INTO guilds (id) VALUES ($1) ON CONFLICT DO NOTHING")
            .bind("$1", guildId)
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .then();
    }

    public Flux<String> getPrefixes(BigInteger guildId) {
        return connection
            .createStatement("SELECT prefixes FROM guilds WHERE id = $1")
            .bind("$1", guildId)
            .execute()
            .flatMap(result -> result.map((row, m) -> row.get("prefixes", String[].class)))
            .flatMap(Flux::fromArray);
    }

    public Mono<Optional<Timezone>> getTimezone(BigInteger userId) {
        return connection
            .createStatement("SELECT timezone FROM users WHERE id = $1")
            .bind("$1", userId)
            .execute()
            .flatMap(result -> result.map((row, m) -> row.get("timezone", String.class)))
            .map(Timezone::of)
            .map(Optional::of)
            .single(Optional.empty());
    }

    public Mono<Void> setTimezone(BigInteger userId, Timezone tz) {
        return connection
            .createStatement("""
                INSERT INTO users (id, timezone)
                VALUES ($1, $2)
                ON CONFLICT (id) DO UPDATE
                    SET timezone = excluded.timezone
                """)
            .bind("$1", userId)
            .bind("$2", tz.getID().canonical())
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .then();
    }

    public Mono<Optional<Locale>> getLocale(BigInteger userId) {
        return connection
            .createStatement("SELECT language FROM users WHERE id = $1")
            .bind("$1", userId)
            .execute()
            .flatMap(result -> result.map((row, m) -> row.get("language", String.class)))
            .map(Locale::forLanguageTag)
            .map(Optional::of)
            .single(Optional.empty());
    }

    public Mono<Void> setLocale(BigInteger userId, Locale locale) {
        return connection
            .createStatement("""
                INSERT INTO users (id, language)
                VALUES ($1, $2)
                ON CONFLICT (id) DO UPDATE
                    SET language = excluded.language
                """)
            .bind("$1", userId)
            .bind("$2", locale.toLanguageTag())
            .execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .then();
    }

}
