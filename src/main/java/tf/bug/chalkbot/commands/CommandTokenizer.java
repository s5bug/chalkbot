package tf.bug.chalkbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tf.bug.chalkbot.ChalkBotClient;
import tf.bug.chalkbot.i18n.XMLResourceBundleControl;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandTokenizer {

    private static final Pattern TOKENIZE_PATTERN = Pattern.compile("\\h+");

    private final ChalkBotClient client;
    private final Command commandRoot;
    private final String bundleName;
    private final Map<Locale, Map<String, String>> loadedLocales;

    public CommandTokenizer(ChalkBotClient client, Command commandRoot, String bundleName) {
        this.client = client;
        this.commandRoot = commandRoot;
        this.bundleName = bundleName;
        this.loadedLocales = new HashMap<>();
    }

    public Mono<Void> run(MessageCreateEvent mce, Locale userLocale, String input) {
        Locale strippedLocale = userLocale.stripExtensions();
        Map<String, String> tokens =
            loadedLocales.computeIfAbsent(strippedLocale, (l) -> {
                ResourceBundle localeResourceBundle =
                    ResourceBundle.getBundle(this.bundleName, l, this.client.getResourceBundleControl());

                Map<String, String> tokenMap = new HashMap<>();
                for(String result : localeResourceBundle.keySet()) {
                    String[] possibleTokens = localeResourceBundle.getString(result).split(" ");
                    for(String possibleToken : possibleTokens) {
                        tokenMap.put(possibleToken, result);
                    }
                }

                return tokenMap;
            });

        Matcher matcher = TOKENIZE_PATTERN.matcher(input);

        Command result = this.commandRoot;
        boolean done = false;
        int ind = 0;
        while(!done) {
            if(result.hasSubCommands() && (matcher.find() || matcher.hitEnd())) {
                int tokenEnd = matcher.hitEnd() ? input.length() : matcher.start();
                String token = input.substring(ind, tokenEnd);
                ind = matcher.hitEnd() ? input.length() : matcher.end();

                if(tokens.containsKey(token)) {
                    String resultToken = tokens.get(token);
                    if(result.hasSubCommand(resultToken)) {
                        result = result.getSubCommand(resultToken);
                        if(matcher.hitEnd()) done = true;
                    } else {
                        done = true;
                    }
                } else {
                    done = true;
                }
            } else {
                done = true;
            }
        }

        return result.run(this.client, mce, userLocale, input.substring(ind));
    }

}
