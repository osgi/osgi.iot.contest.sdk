package osgi.enroute.trains.hardware.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persist hardware configuration.
 * 
 * The HardwareConfig for each Raspberry Pi will be set using the webconsole.
 * ConfigPersister ensures the configuration survives restarts.
 * 
 */
@Component
public class ConfigPersister implements ConfigurationListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** persist directory location, relative to bndrun file location */
    private static final String persistDir = "etc";

    /** only persist these pids */
    private static List<String> pids = Arrays.asList(new String[] {
            HardwareConfig.HARDWARE_CONFIGURATION_PID
    });

    private ConfigurationAdmin configAdmin;

    @Reference
    void setConfigAdmin(ConfigurationAdmin ca) {
        this.configAdmin = ca;
    }

    @Activate
    void activate() {
        for (String pid : pids) {
            try {
                File file = getFile(pid);
                InputStream in = new FileInputStream(file);
                loadConfiguration(pid, in);
                in.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (!pids.contains(event.getPid()))
            return;

        try {
            if (event.getType() == ConfigurationEvent.CM_UPDATED) {
                Configuration config = configAdmin.getConfiguration(event.getPid(), event.getFactoryPid());
                File file = getFile(event.getPid());
                log.info("saving configuration: {}", file);
                saveConfiguration(config, file);
            }
        } catch (IOException e) {
            log.warn("failed to save configuration", e);
        }
    }

    private File getFile(String pid) {
        String fileName = pid + ".config";
        return new File(persistDir, fileName);
    }

    private void loadConfiguration(String pid, InputStream in) throws IOException {
        @SuppressWarnings("unchecked")
        Dictionary<String, ?> config = ConfigurationHandler.read(in);
        in.close();
        Configuration configuration = configAdmin.getConfiguration(pid, null);
        configuration.update(config);
        log.info("loaded configuration for pid={}", pid);
    }

    private void saveConfiguration(Configuration config, File file) throws IOException {
        Dictionary<String, ?> dict = config.getProperties();

        OutputStream out = new FileOutputStream(file);
        Properties props = new Properties();

        for (Enumeration<String> e = dict.keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            if (!Constants.SERVICE_PID.equals(key)
                    && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)) {
                props.put(key, dict.get(key));
            }
        }

        try {
            log.debug("persisting configuration: {} - {}", config.getPid(), props);
            String comment = String.format("# updated by %s on %s\n", getClass().getSimpleName(), new Date());
            out.write(comment.getBytes());
            ConfigurationHandler.write(out, props);
        } finally {
            out.close();
        }
    }

}
