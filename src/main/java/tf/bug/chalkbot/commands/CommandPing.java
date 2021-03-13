package tf.bug.chalkbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import tf.bug.chalkbot.ChalkBotClient;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class CommandPing implements Command {

    private Map<Locale, ResourceBundle> pingMessages;

    private CommandPing() {
        this.pingMessages = new HashMap<>();
    }

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
        Locale strippedLocale = userLocale.stripExtensions();
        ResourceBundle bundle =
            pingMessages.computeIfAbsent(
                strippedLocale,
                (l) -> ResourceBundle.getBundle("command_ping", l, client.getResourceBundleControl())
            );

        String response = bundle.getString("pong");

        return mce.getMessage().getChannel().flatMap(c -> c.createMessage(response)).then(Mono.empty());
    }

}
