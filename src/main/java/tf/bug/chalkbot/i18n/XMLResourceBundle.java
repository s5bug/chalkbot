package tf.bug.chalkbot.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class XMLResourceBundle extends ResourceBundle {

    private Properties props;

    public XMLResourceBundle(InputStream stream) throws IOException {
        props = new Properties();
        props.loadFromXML(stream);
    }

    @Override
    protected Object handleGetObject(String key) {
        return props.getProperty(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        Set<String> handleKeys = props.stringPropertyNames();
        return Collections.enumeration(handleKeys);
    }

}
