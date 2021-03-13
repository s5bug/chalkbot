package tf.bug.chalkbot;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.Properties;

public class ChalkBot {

    public static void main(final String[] args) {
        String propertiesPath = args[0];
        File propertiesFile = new File(propertiesPath);
        Properties properties = new Properties();
        try(InputStream is = new FileInputStream(propertiesFile)) {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String token = properties.getProperty("discord_token");

        String dbHost = properties.getProperty("postgres_host");
        int dbPort = Integer.parseInt(properties.getProperty("postgres_port"));
        String dbUsername = properties.getProperty("postgres_username");
        String dbPassword = properties.getProperty("postgres_password");
        String dbDatabase = properties.getProperty("postgres_database");

        ChalkBotDB.create(
            dbHost,
            dbPort,
            dbUsername,
            dbPassword,
            dbDatabase
        ).flatMap(db -> {
            ChalkBotClient client = new ChalkBotClient(token, db);

            return client.getClient().withGateway(gateway -> {
                Publisher<?> handleGuildCreation =
                    gateway.on(GuildCreateEvent.class)
                        .flatMap(gce -> client.handleGuildCreation(gateway, gce));

                Publisher<?> ping =
                    gateway.on(MessageCreateEvent.class)
                        .flatMap(mce -> client.handleMessageCreation(gateway, mce));

                return Mono.when(handleGuildCreation, ping);
            });
        }).block();
    }

}
