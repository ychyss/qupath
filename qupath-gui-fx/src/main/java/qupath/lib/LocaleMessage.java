package qupath.lib;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.BuildInfo;

import java.util.ResourceBundle;
import java.util.Locale;

public class LocaleMessage {

    private static final Logger logger = LoggerFactory.getLogger(BuildInfo.class);
    private static final LocaleMessage INSTANCE = new LocaleMessage();
    private ResourceBundle bundle;

    public static LocaleMessage getInstance() {
        return INSTANCE;
    }

    // 测试结束之后改成private
    public LocaleMessage() {
        // 1 设置总体的locale
        Locale.setDefault(Locale.CHINA);
//        System.out.println("locale="+Locale.CHINA);
        // 2 获取配置文件
        bundle = ResourceBundle.getBundle("i18n.messages");
    }

    public String get(String key) {
        if(bundle.containsKey(key)) {
            return bundle.getString(key);
        }else{
            logger.error("no key " + key + " in locale file.");
            return "";
        }
    }

    @Test
    public void Testa() {
        LocaleMessage.getInstance().get("choose.theme");
    }
}
