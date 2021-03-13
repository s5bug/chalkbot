package tf.bug.chalkbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import net.time4j.Moment;
import net.time4j.format.DisplayMode;
import net.time4j.format.expert.ChronoFormatter;
import net.time4j.tz.Timezone;
import reactor.core.publisher.Mono;
import tf.bug.chalkbot.ChalkBotClient;
import tf.bug.chalkbot.ChalkBotDB;

import java.text.MessageFormat;
import java.util.*;

public class CommandTime implements Command {

    private Map<Locale, ResourceBundle> timeMessages;

    private CommandTime() {
        timeMessages = new HashMap<>();
    }

    private static CommandTime instance;

    public static CommandTime getInstance() {
        if(instance == null) {
            instance = new CommandTime();
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
    public Command getSubCommand(String subCommandName) {
        return null;
    }

    @Override
    public <T> Mono<T> run(ChalkBotClient client, MessageCreateEvent mce, Locale userLocale, String arguments) {
        Locale strippedLocale = userLocale.stripExtensions();
        ResourceBundle bundle =
            timeMessages.computeIfAbsent(
                strippedLocale,
                (l) -> ResourceBundle.getBundle("command_time", l, client.getResourceBundleControl())
            );

        User timeToGet = mce.getMessage().getAuthor().get();
        if(!arguments.isEmpty()) {
            // TODO support resolving users
        }

        ChalkBotDB db = client.getDb();

        Mono<Optional<Timezone>> tz =
            db.timeZone(timeToGet.getId().asBigInteger()).map(Optional::of).last(Optional.empty());

        return tz.flatMap(tzo -> {
            if(tzo.isPresent()) {
                String message = bundle.getString("time");
                Timezone tzr = tzo.get();

                ChronoFormatter<Moment> f =
                    ChronoFormatter.ofMomentStyle(DisplayMode.FULL, DisplayMode.FULL, userLocale, tzr.getID());

                Moment now = Moment.nowInSystemTime();

                String format = f.format(now);

                String result = MessageFormat.format(message, timeToGet.getUsername(), format);

                return mce.getMessage().getChannel().flatMap(c -> c.createMessage(result));
            } else {
                String message = bundle.getString("no_timezone");

                // TODO make a system for referring to a user
                String result = MessageFormat.format(message, timeToGet.getUsername());

                return mce.getMessage().getChannel().flatMap(c -> c.createMessage(result));
            }
        }).then(Mono.empty());
    }
}
