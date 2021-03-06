package org.openlumify.core.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.bootstrap.InjectHelper;
import org.openlumify.core.util.OpenLumifyLogger;
import org.openlumify.core.util.OpenLumifyLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@Singleton
public class OpenLumifyResourceBundleManager {
    private static final OpenLumifyLogger LOGGER = OpenLumifyLoggerFactory.getLogger(OpenLumifyResourceBundleManager.class);
    public static final String RESOURCE_BUNDLE_BASE_NAME = "MessageBundle";
    private Properties unlocalizedProperties;
    private Map<Locale, Properties> localizedProperties;
    private final boolean devMode;

    @Inject
    public OpenLumifyResourceBundleManager(Configuration configuration) {
        unlocalizedProperties = new Properties();
        localizedProperties = new HashMap<>();
        this.devMode = configuration.getBoolean(Configuration.DEV_MODE, Configuration.DEV_MODE_DEFAULT);
    }

    public void register(InputStream inputStream) throws IOException {
        unlocalizedProperties.load(new InputStreamReader(inputStream, "UTF-8"));
    }

    public void register(InputStream inputStream, Locale locale) throws IOException {
        Properties properties = localizedProperties.get(locale);
        if (properties == null) {
            properties = new Properties();
            localizedProperties.put(locale, properties);
        }
        properties.load(new InputStreamReader(inputStream, "UTF-8"));
    }

    public ResourceBundle getBundle() {
        Locale defaultLocale = Locale.getDefault();
        LOGGER.debug("returning a bundle configured for the default locale: %s ", defaultLocale);
        return createBundle(defaultLocale);
    }

    public ResourceBundle getBundle(Locale locale) {
        LOGGER.debug("returning a bundle configured for locale: %s ", locale);
        return createBundle(locale);
    }

    private ResourceBundle createBundle(Locale locale) {
        Properties properties = new Properties();
        properties.putAll(unlocalizedProperties);
        properties.putAll(getLocaleProperties(locale));
        return new OpenLumifyResourceBundle(properties, getRootBundle(locale));
    }

    private Properties getLocaleProperties(Locale locale) {
        Properties properties = new Properties();

        Properties languageProperties = localizedProperties.get(new Locale(locale.getLanguage()));
        if (languageProperties != null) {
            properties.putAll(languageProperties);
        }

        Properties languageCountryProperties = localizedProperties.get(new Locale(locale.getLanguage(), locale.getCountry()));
        if (languageCountryProperties != null) {
            properties.putAll(languageCountryProperties);
        }

        Properties languageCountryVariantProperties = localizedProperties.get(new Locale(locale.getLanguage(), locale.getCountry(), locale.getVariant()));
        if (languageCountryVariantProperties != null) {
            properties.putAll(languageCountryVariantProperties);
        }

        return properties;
    }

    private ResourceBundle getRootBundle(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE_NAME, locale, new UTF8PropertiesControl(devMode));
    }

    /**
     * use an InputStreamReader to allow for UTF-8 values in property file bundles, otherwise use the base class implementation
     */
    private class UTF8PropertiesControl extends ResourceBundle.Control {
        private final boolean disableCache;

        public UTF8PropertiesControl(boolean disableCache) {
            this.disableCache = disableCache;
        }

        @Override
        public long getTimeToLive(String baseName, Locale locale) {
            return disableCache ? TTL_DONT_CACHE : TTL_NO_EXPIRATION_CONTROL;
        }

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            if (format.equals("java.properties")) {
                String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
                InputStream inputStream = null;
                if (reload) {
                    URL url = loader.getResource(resourceName);
                    if (url != null) {
                        URLConnection urlConnection = url.openConnection();
                        if (urlConnection != null) {
                            urlConnection.setUseCaches(false);
                            inputStream = urlConnection.getInputStream();
                        }
                    }
                } else {
                    inputStream = loader.getResourceAsStream(resourceName);
                }

                if (inputStream != null) {
                    try {
                        return new PropertyResourceBundle(new InputStreamReader(inputStream, "UTF-8"));
                    } finally {
                        inputStream.close();
                    }
                }
            }
            return super.newBundle(baseName, locale, format, loader, reload);
        }
    }
}
