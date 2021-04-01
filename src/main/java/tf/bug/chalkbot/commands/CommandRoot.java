package tf.bug.chalkbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;
import tf.bug.chalkbot.ChalkBotClient;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CommandRoot implements Command {

    private final Map<String, Command> subCommands;

    private CommandRoot() {
        this.subCommands = new HashMap<>();
        this.subCommands.put("ping", CommandPing.getInstance());
        this.subCommands.put("time", CommandTime.getInstance());
        this.subCommands.put("timezone", CommandTimezone.getInstance());
    }

    private static CommandRoot instance;

    public static CommandRoot getInstance() {
        if(instance == null) {
            instance = new CommandRoot();
        }

        return instance;
    }

    @Override
    public boolean hasSubCommands() {
        return true;
    }

    @Override
    public boolean hasSubCommand(String subCommandToken) {
        return subCommands.containsKey(subCommandToken);
    }

    @Override
    public Command getSubCommand(String subCommandToken) {
        return subCommands.get(subCommandToken);
    }

    @Override
    public Mono<Void> run(ChalkBotClient client, MessageCreateEvent mce, Locale userLocale, String arguments) {
        return Mono.empty();
    }

}
