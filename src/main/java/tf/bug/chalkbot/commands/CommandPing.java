package tf.bug.chalkbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import tf.bug.chalkbot.ChalkBotClient;

import java.util.*;

public class CommandPing implements Command {

    private CommandPing() {}

    private static CommandPing instance;

    public static CommandPing getInstance() {
        if(instance == null) {
            instance = new CommandPing();
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
        Optional<String> response = client.getLangKeyHandler().format(userLocale, "command.ping.pong");

        // TODO handle missing key errors
        String respText = response.orElse("command.ping.pong");

        return mce.getMessage().getChannel().flatMap(c -> c.createMessage(respText)).then(Mono.empty());
    }

}
