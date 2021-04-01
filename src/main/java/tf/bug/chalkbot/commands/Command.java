package tf.bug.chalkbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;
import tf.bug.chalkbot.ChalkBotClient;

import java.util.Locale;

public interface Command {

    boolean hasSubCommands();
    boolean hasSubCommand(String subCommandToken);
    Command getSubCommand(String subCommandToken);

    Mono<Void> run(ChalkBotClient client, MessageCreateEvent mce, Locale userLocale, String arguments);

}
