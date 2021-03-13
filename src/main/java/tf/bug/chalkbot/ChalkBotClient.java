package tf.bug.chalkbot;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import net.iakovlev.timeshape.TimeZoneEngine;
import net.time4j.Moment;
import net.time4j.PlainTimestamp;
import net.time4j.format.DisplayMode;
import net.time4j.format.expert.ChronoFormatter;
import net.time4j.tz.NameStyle;
import net.time4j.tz.TZID;
import net.time4j.tz.Timezone;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import tf.bug.chalkbot.commands.CommandRoot;
import tf.bug.chalkbot.commands.CommandTokenizer;
import tf.bug.chalkbot.i18n.XMLResourceBundleControl;
import tf.bug.chalkbot.overpass.OverpassClient;
import tf.bug.chalkbot.overpass.OverpassResponse;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class ChalkBotClient {

    private final ChalkBotDB db;
    private final DiscordClient client;
    private final ResourceBundle.Control resourceBundleControl;
    private final CommandTokenizer commandTokenizer;
    private final OverpassClient overpassClient;
    private final TimeZoneEngine timeZoneEngine;

    public ChalkBotClient(String token, ChalkBotDB db) {
        this.db = db;

        this.client = DiscordClient.create(token);

        this.resourceBundleControl = new XMLResourceBundleControl();

        this.commandTokenizer = new CommandTokenizer(this, CommandRoot.getInstance(), "commands");

        this.overpassClient = new OverpassClient(HttpClient.create());

        this.timeZoneEngine = TimeZoneEngine.initialize();
    }

    public ChalkBotDB getDb() {
        return this.db;
    }

    public DiscordClient getClient() {
        return this.client;
    }

    public ResourceBundle.Control getResourceBundleControl() {
        return this.resourceBundleControl;
    }

    public Publisher<?> handleGuildCreation(GatewayDiscordClient me, GuildCreateEvent gce) {
        return db.createGuild(gce.getGuild().getId().asBigInteger());
    }

    public Publisher<?> handleMessageCreation(GatewayDiscordClient me, MessageCreateEvent mce) {
        String content = mce.getMessage().getContent();

        String usMent = "<@" + me.getSelfId().asString() + ">";
        String usNickMent = "<@!" + me.getSelfId().asString() + ">";
        Mono<String> rest;
        if(content.startsWith(usMent + " ") || content.startsWith(usNickMent + " ")) {
            if(content.startsWith(usMent + " ")) {
                rest = Mono.just(content.substring((usMent + " ").length()));
            } else {
                rest = Mono.just(content.substring((usNickMent + " ").length()));
            }
        } else {
            Flux<String> prefixes = getPrefixes(mce.getGuildId());
            Flux<String> matches = prefixes.filter(content::startsWith);
            Mono<String> longest = matches.reduce((a, b) -> {
                if(a.length() > b.length()) {
                    return a;
                } else {
                    return b;
                }
            });
            rest = longest.map(longStr -> content.substring(longStr.length()));
        }

        return rest.flatMap(str -> {
            BigInteger userId = mce.getMessage().getAuthor().get().getId().asBigInteger();
            Mono<Locale> locale = db.locale(userId).last(Locale.US);
            return locale.flatMap(rlocale -> this.commandTokenizer.run(mce, rlocale, str));
        });
    }

    Flux<String> getPrefixes(Optional<Snowflake> guildId) {
        Flux<String> prefixes;
        if(guildId.isPresent()) {
            BigInteger gid = guildId.get().asBigInteger();
            prefixes = db.prefixes(gid);
        } else {
            prefixes = Flux.just("c!");
        }
        return prefixes;
    }
}
