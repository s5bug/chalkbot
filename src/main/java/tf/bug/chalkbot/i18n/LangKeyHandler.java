package tf.bug.chalkbot.i18n;

import tf.bug.chalkbot.ChalkBotClient;

import java.text.MessageFormat;
import java.util.*;

public class LangKeyHandler {

    private final ChalkBotClient client;
    private final String bundleId;
    private final Map<Locale, ResourceBundle> langBundles;

    public LangKeyHandler(ChalkBotClient client, String bundleId) {
        this.client = client;
        this.bundleId = bundleId;
        this.langBundles = new HashMap<>();
    }

    public Optional<String> get(Locale locale, String key) {
        Locale strippedLocale = locale.stripExtensions();
        ResourceBundle bundle =
            langBundles.computeIfAbsent(
                strippedLocale,
                l -> ResourceBundle.getBundle(this.bundleId, l, client.getResourceBundleControl())
            );

        try {
            return Optional.of(bundle.getString(key));
        } catch (MissingResourceException mre) {
            return Optional.empty();
        }
    }

    public Optional<String> format(Locale locale, String key, Object... arguments) {
        Optional<String> msgFormat = this.get(locale, key);

        return msgFormat.map(f -> MessageFormat.format(f, arguments));
    }

}
