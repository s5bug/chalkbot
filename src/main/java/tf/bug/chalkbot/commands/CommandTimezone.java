package tf.bug.chalkbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import net.time4j.tz.NameStyle;
import net.time4j.tz.Timezone;
import org.apache.commons.text.StringEscapeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tf.bug.chalkbot.ChalkBotClient;
import tf.bug.chalkbot.overpass.OverpassElement;

import java.text.MessageFormat;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CommandTimezone implements Command {
    
    private CommandTimezone() {}
    
    private static CommandTimezone instance;

    public static CommandTimezone getInstance() {
        if(instance == null) {
            instance = new CommandTimezone();
        }

        return instance;
    }

    @Override
    public boolean hasSubCommands() {
        return true;
    }

    @Override
    public boolean hasSubCommand(String subCommandToken) {
        return "set".equals(subCommandToken);
    }

    @Override
    public Command getSubCommand(String subCommandToken) {
        if("set".equals(subCommandToken)) {
            return CommandTimezoneSet.getInstance();
        } else {
            return null;
        }
    }

    @Override
    public Mono<Void> run(ChalkBotClient client, MessageCreateEvent mce, Locale userLocale, String arguments) {
        // TODO check arguments

        // TODO check for membership
        Optional<User> authorO = mce.getMessage().getAuthor();
        if(authorO.isPresent()) {
            User author = authorO.get();

            Mono<Optional<Timezone>> tzm = client.getDb().getTimezone(author.getId().asBigInteger());

            return tzm.flatMap(tzo -> {
                if(tzo.isPresent()) {
                    Timezone rtz = tzo.get();
                    String tzDisplayName = rtz.getDisplayName(NameStyle.LONG_GENERIC_TIME, userLocale);
                    Optional<String> omsg =
                        client.getLangKeyHandler().
                            format(userLocale, "command.timezone.timezone", author.getTag(), tzDisplayName);

                    // TODO missing key handling
                    String msg = omsg.orElse("command.timezone.timezone");

                    return mce.getMessage().getChannel().flatMap(c -> c.createMessage(msg)).then();
                } else {
                    // TODO handle timezone missing
                    return Mono.empty();
                }
            });
        } else {
            // TODO should handle webhooks?
            return Mono.empty();
        }
    }

    public static class CommandTimezoneSet implements Command {

        private CommandTimezoneSet() {}

        private static CommandTimezoneSet instance;

        public static CommandTimezoneSet getInstance() {
            if(instance == null) {
                instance = new CommandTimezoneSet();
            }

            return instance;
        }

        @Override
        public boolean hasSubCommands() {
            return false;
        }

        @Override
        public boolean hasSubCommand(String subCommandToken) {
            return false;
        }

        @Override
        public Command getSubCommand(String subCommandToken) {
            return null;
        }

        private static final String NODES_QUERY = """
            [out:json];
            (
              node["name"="{0}"]["place"="city"];
              node["name:{1}"="{0}"]["place"="city"];
              node["name"="{0}"]["place"="county"];
              node["name:{1}"="{0}"]["place"="county"];
              node["name"="{0}"]["place"="state"];
              node["name:{1}"="{0}"]["place"="state"];
              node["name"="{0}"]["place"="province"];
              node["name:{1}"="{0}"]["place"="province"];
              node["name"="{0}"]["place"="country"];
              node["name:{1}"="{0}"]["place"="country"];
            );
            out;""";

        @Override
        public Mono<Void> run(ChalkBotClient client, MessageCreateEvent mce, Locale userLocale, String arguments) {
            String placeQuery = StringEscapeUtils.escapeJson(arguments);
            String userLang = userLocale.getLanguage();

            String fullQuery = MessageFormat.format(NODES_QUERY, placeQuery, userLang);

            // TODO display a localized dialog to the user
            return client.getOverpassClient().request(fullQuery).flatMap(resp -> {
                Flux<OverpassElement> elements = Flux.fromIterable(resp.getElements());
                Flux<Timezone> entries = elements.flatMap(el -> {
                    Optional<ZoneId> resultZoneId = client.getTimeZoneEngine().query(el.getLat(), el.getLon());
                    if(resultZoneId.isPresent()) {
                        Timezone tz = Timezone.of(resultZoneId.get().toString());
                        return Mono.just(tz);
                    } else {
                        return Mono.empty();
                    }
                });
                Mono<List<Timezone>> timezones = entries.collectList();

                return timezones.flatMap(l -> {
                    if(l.isEmpty()) {
                        // TODO display that there is no timezone
                        return Mono.empty();
                    } else if(l.size() == 1) {
                        Timezone tz = l.get(0);
                        // TODO check for membership
                        Optional<User> ou = mce.getMessage().getAuthor();
                        if(ou.isPresent()) {
                            User u = ou.get();
                            return client.getDb().setTimezone(u.getId().asBigInteger(), tz).then(
                                mce.getMessage().getChannel().flatMap(c -> {
                                    String tzDisplayName = tz.getDisplayName(NameStyle.LONG_GENERIC_TIME, userLocale);
                                    Optional<String> omsg =
                                        client.getLangKeyHandler().format(userLocale, "command.timezone.set.set", u.getTag(), tzDisplayName);
                                    // TODO handle missing language key
                                    String msg = omsg.orElse("command.timezone.set.set");
                                    return c.createMessage(msg).then();
                                })
                            );
                        } else {
                            // TODO handle webhooks?
                            return Mono.empty();
                        }
                    } else {
                        // TODO display a selection dialog
                        return Mono.empty();
                    }
                });
            });
        }

    }

}
