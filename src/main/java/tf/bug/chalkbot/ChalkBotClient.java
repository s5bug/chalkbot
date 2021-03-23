package tf.bug.chalkbot;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
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
import tf.bug.chalkbot.i18n.LangKeyHandler;
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
    private final GatewayDiscordClient client;
    private final ResourceBundle.Control resourceBundleControl;
    private final LangKeyHandler langKeyHandler;
    private final CommandTokenizer commandTokenizer;
    private final OverpassClient overpassClient;
    private final TimeZoneEngine timeZoneEngine;

    public static Mono<ChalkBotClient> create(String token, ChalkBotDB db) {
        return DiscordClient.create(token).login().map(gdc -> new ChalkBotClient(gdc, db));
    }

    public ChalkBotClient(GatewayDiscordClient client, ChalkBotDB db) {
        this.db = db;

        this.client = client;

        this.resourceBundleControl = new XMLResourceBundleControl();

        this.langKeyHandler = new LangKeyHandler(this, "message");

        this.commandTokenizer = new CommandTokenizer(this, CommandRoot.getInstance(), "command");

        this.overpassClient = new OverpassClient(HttpClient.create());

        this.timeZoneEngine = TimeZoneEngine.initialize();
    }

    public ChalkBotDB getDb() {
        return this.db;
    }

    public GatewayDiscordClient getClient() {
        return this.client;
    }

    public ResourceBundle.Control getResourceBundleControl() {
        return this.resourceBundleControl;
    }

    public LangKeyHandler getLangKeyHandler() {
        return this.langKeyHandler;
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

    public Flux<String> getPrefixes(Optional<Snowflake> guildId) {
        Flux<String> prefixes;
        if(guildId.isPresent()) {
            BigInteger gid = guildId.get().asBigInteger();
            prefixes = db.prefixes(gid);
        } else {
            prefixes = Flux.just("c!");
        }
        return prefixes;
    }

    public Mono<MemberQueryResponse> queryMember(
        String query,
        Optional<Guild> guild
    ) {
        Flux<User> userMatching = client.getUsers().filter(p -> query.equals(p.getUsername()));
        Mono<Optional<User>> singleUser = userMatching.map(Optional::of).single(Optional.empty());
        Mono<MemberQueryResponse> userResp = singleUser.map(uo -> {
            if(uo.isPresent()) {
                return new MemberQueryResponse.User(uo.get());
            } else {
                return new MemberQueryResponse.Empty();
            }
        });

        if(guild.isPresent()) {
            Flux<Member> matching = client.getGuildMembers(guild.get().getId()).filter(p -> query.equals(p.getDisplayName()));
            Mono<Optional<Member>> memberResult = matching.map(Optional::of).single(Optional.empty());

            return memberResult.flatMap(mo -> {
               if(mo.isPresent()) {
                   return Mono.just(new MemberQueryResponse.Member(mo.get()));
               } else {
                   return userResp;
               }
            });
        } else {
            return userResp;
        }
    }

    public sealed interface MemberQueryResponse permits
        MemberQueryResponse.Empty,
        MemberQueryResponse.User,
        MemberQueryResponse.Member {
        final record Empty() implements MemberQueryResponse {}
        final record User(discord4j.core.object.entity.User user) implements MemberQueryResponse {}
        final record Member(discord4j.core.object.entity.Member member) implements MemberQueryResponse {}
    }

}
