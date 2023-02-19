package qupath.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.prefs.PathPrefs;

import java.util.ResourceBundle;
import java.util.Locale;

/**
 * Helper class to get strings from properties file with corresponding locale.
 *
 * @author HYS
 *
 */
public class LocaleMessage {

    private static final Logger logger = LoggerFactory.getLogger(LocaleMessage.class);
    private static final LocaleMessage INSTANCE = new LocaleMessage();
    private ResourceBundle bundle;

    public static LocaleMessage getInstance() {
        return INSTANCE;
    }

    private LocaleMessage() {
        // 1 set locale
        Locale.setDefault(PathPrefs.defaultLocaleProperty().get());
        // 2 get message properties
        bundle = ResourceBundle.getBundle("i18n.messages");
//        ResourceBundle bundle1 = ResourceBundle.getBundle("i18n.extension_bioformat");
//        logger.info("extension name="+bundle1.getString("extension.name"));
    }

    public String get(String key) {
        if(bundle.containsKey(key)) {
            return bundle.getString(key);
        }else{
            logger.error("no key of " + key + " in locale file.");
            return "";
        }
    }

}
