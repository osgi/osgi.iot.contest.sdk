package osgi.enroute.trains.location.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eurotech.cloud.client.EdcCallbackHandler;
import com.eurotech.cloud.client.EdcClientException;
import com.eurotech.cloud.client.EdcClientFactory;
import com.eurotech.cloud.client.EdcCloudClient;
import com.eurotech.cloud.client.EdcConfiguration;
import com.eurotech.cloud.client.EdcConfigurationFactory;
import com.eurotech.cloud.client.EdcDeviceProfile;
import com.eurotech.cloud.client.EdcDeviceProfileFactory;
import com.eurotech.cloud.message.EdcPayload;

import osgi.enroute.trains.controller.api.TrainLocator;

@Designate(ocd = LocationConfiguration.class)
@Component(name = LocationConfiguration.LOCATION_CONFIGURATION_PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class TrainLocationClient implements TrainLocator {
    static final Logger log = LoggerFactory.getLogger(TrainLocationClient.class);

    private static final String ACCOUNT_NAME = "NetLogix-WB";
    private static final String BROKER_URL = "mqtts://broker-sandbox.everyware-cloud.com:8883";
    private static final String USERNAME = "osgiTrain";
    private static final String PASSWORD = "osg!TrainDem0";

    private static final String ASSET_ID = "myAssetId";
    private static final String CLIENT_ID = "TrainLocator";
    private static final double LATITUDE = 52.3188909; // Hanover
    private static final double LONGITUDE = 9.8009607; // Hanover

    private EdcCloudClient edcCloudClient = null;
    private Map<String, String> tag2code = new HashMap<>();
    private Map<String, String> code2segment = new HashMap<>();
    private Deferred<String> nextLocation = new Deferred<String>();

    //
    // Activate: create client configuration, and set its properties
    //
    @Activate
    void activate(LocationConfiguration config) {
        if (config.tag2code() == null || config.code2segment() == null) {
            final String msg = "configuration MUST contain tag2code and code2segment";
            error("{}: {}.\n", getClass().getSimpleName(), msg);
            throw new Error(msg);
        }

        for (String s : config.tag2code()) {
            String[] split = s.split(":", 2);
            tag2code.put(split[0], split[1]);
        }

        for (String s : config.code2segment()) {
            String[] split = s.split(":", 2);
            code2segment.put(split[0], split[1]);
        }

        // info("tag2code=" + tag2code);
        // info("code2segment=" + code2segment);

        BiFunction<String, String, String> getOrDefault = (t, u) -> t != null ? t : u;

        String accountName = getOrDefault.apply(config.accountName(), ACCOUNT_NAME);
        String brokerUrl = getOrDefault.apply(config.brokerUrl(), BROKER_URL);
        String username = getOrDefault.apply(config.username(), USERNAME);
        String password = getOrDefault.apply(config.password(), PASSWORD);

        EdcConfigurationFactory confFact = EdcConfigurationFactory.getInstance();
        EdcConfiguration conf = confFact.newEdcConfiguration(
                accountName, ASSET_ID, brokerUrl, CLIENT_ID, username, password);

        EdcDeviceProfileFactory profFactory = EdcDeviceProfileFactory.getInstance();
        EdcDeviceProfile prof = profFactory.newEdcDeviceProfile();
        prof.setDisplayName("Train Locator Client");
        prof.setModelName("JavaClient");

        // set GPS position in device profile - this is sent only once
        prof.setLongitude(LONGITUDE);
        prof.setLatitude(LATITUDE);

        try {
            //
            // Connect and start the session
            //
            edcCloudClient = EdcClientFactory.newInstance(conf, prof, new MyCallbackHandler());
            edcCloudClient.startSession();
            info("Session started");

            //
            // Subscribe
            //
            info("Subscribe to data topics of TrainDemo assets in the account");
            edcCloudClient.subscribe("+", "TrainDemo/#", 1);

            // info("Subscribe to control topics of all assets in the account");
            // edcCloudClient.controlSubscribe("+", "#", 1);

        } catch (EdcClientException e) {
            error(e.toString());
            throw new RuntimeException(e);
        }
    }

    //
    // Stop the session and close the connection
    //
    @Deactivate
    void deactivate() {
        info("Terminating EDC Cloud Client");
        try {
            edcCloudClient.stopSession();
            edcCloudClient.terminate();
        } catch (EdcClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized Promise<String> nextLocation() {
        return nextLocation.getPromise();
    }

    private synchronized void trigger(String train, String segment) {
        info("trigger: train={} segment={}", train, segment);
        Deferred<String> currentLocation = nextLocation;
        nextLocation = new Deferred<String>();
        currentLocation.resolve(train + ":" + segment);
    }

    // logging is configured to use OSGi logger and so need webconsole to view
    // these methods ensures errors are visible
    private static void error(String fmt, Object... args) {
        System.err.printf("ERROR: " + fmt.replaceAll("\\{}", "%s") + "\n", args);
        log.error(fmt, args);
    }

    private static void info(String fmt, Object... args) {
        System.out.printf(fmt.replaceAll("\\{}", "%s") + "\n", args);
        log.info(fmt, args);
    }

    class MyCallbackHandler implements EdcCallbackHandler {
        // -----------------------------------------------------------------------
        //
        // MQTT Callback methods
        //
        // -----------------------------------------------------------------------

        // display data messages received from broker
        @Override
        public void publishArrived(String assetId, String topic, EdcPayload msg, int qos, boolean retain) {

            String train = String.valueOf(msg.getMetric("train"));
            Object location = msg.getMetric("location");

            if (location != null) {
                String tag = location.toString();
                String code = tag2code.get(tag);
                if (code == null) {
                    error("unknown tag <{}>", tag);
                } else {
                    String segment = code2segment.get(code);
                    if (segment == null) {
                        error("no segment for code <{}>", code);
                    } else {
                        trigger(train, segment);
                    }
                }
            } else if (msg.getMetric("connection") != null) {
                info("Connection: " + msg.getMetric("connection"));
            } else {
                info("Data publish arrived on semantic topic: " + topic + ", qos: " + qos + ", assetId: " + assetId);
            }
        }

        // display control messages received from broker
        @Override
        public void controlArrived(String assetId, String topic, EdcPayload msg, int qos, boolean retain) {
            info("Control publish arrived on semantic topic: " + topic + " , qos: " + qos);
            // Print all the metrics
            for (String name : msg.metricNames()) {
                info(name + ":" + msg.getMetric(name));
            }
        }

        @Override
        public void connectionLost() {
            info("EDC client connection lost");
        }

        @Override
        public void connectionRestored() {
            info("EDC client reconnected");
        }

        @Override
        public void published(int messageId) {
            // info("Publish message ID: " + messageId + " confirmed");
        }

        @Override
        public void subscribed(int messageId) {
            // info("Subscribe message ID: " + messageId + " confirmed");
        }

        @Override
        public void unsubscribed(int messageId) {
            // info("Unsubscribe message ID: " + messageId + " confirmed");
        }

        @Override
        public void controlArrived(String assetId, String topic, byte[] payload, int qos, boolean retain) {
            // TODO Auto-generated method stub
        }

        @Override
        public void publishArrived(String assetId, String topic, byte[] payload, int qos, boolean retain) {
            // TODO Auto-generated method stub
        }
    }

}
