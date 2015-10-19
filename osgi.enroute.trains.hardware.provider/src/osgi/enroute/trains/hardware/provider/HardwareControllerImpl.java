package osgi.enroute.trains.hardware.provider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.function.BiFunction;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import osgi.enroute.configurer.api.RequireConfigurerExtender;
import osgi.enroute.trains.cloud.api.Color;
import osgi.enroute.trains.controller.api.RFIDSegmentController;
import osgi.enroute.trains.controller.api.SegmentController;
import osgi.enroute.trains.controller.api.SignalSegmentController;
import osgi.enroute.trains.controller.api.SwitchSegmentController;
import osgi.enroute.trains.train.api.TrainController;

/**
 * Controller for OSGi demo hardware on a Raspberry Pi.
 * <p>
 * The hardware consists of:
 * <ul>
 * <li>One infra-red LED, connected to GPIO17, to emulate a Lego remote
 * controller.</li>
 * <li>Two multi-color LEDs, connected to GPIO??, to control signal lights.</li>
 * <li>Two RFID readers, connected to the SPI bus, to detect train position.
 * </li>
 * <li>Two Lego motors, connected via GPIO?? to an H-bridge motor driver, to
 * control track switches.</li>
 * </ul>
 */
@RequireConfigurerExtender
@Designate(ocd = HardwareConfig.class)
@Component(name = HardwareConfig.HARDWARE_CONFIGURATION_PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class HardwareControllerImpl {

    static protected Logger logger = LoggerFactory
            .getLogger(HardwareControllerImpl.class);

    private List<ServiceRegistration<? extends SegmentController>> segmentControllers = new ArrayList<>();
    private List<ServiceRegistration<TrainController>> trainControllers = new ArrayList<>();
    private HardwareConfig config;
    private BundleContext ctx;
    private String location;

    @Activate
    void activate(BundleContext ctx, HardwareConfig hwc) {
        this.config = hwc;
        this.ctx = ctx;

        if (hwc.irLed()) {
            registerTrain(1);
            registerTrain(2);
            registerTrain(3);
            registerTrain(4);
        }

        registerSegment(hwc.signals(), SignalSegmentController.class, Signal::new);
        registerSegment(hwc.switches(), SwitchSegmentController.class, Switch::new);
        registerSegment(hwc.locators(), RFIDSegmentController.class, Locator::new);

        try {
            location = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            location = "unknown";
        }
    }

    private void registerTrain(int channel) {
        Hashtable<String, Object> dict = new Hashtable<>();
        dict.put(Constants.SERVICE_EXPORTED_INTERFACES, "*");
        dict.put(TrainController.CONTROLLER_CHANNEL, channel);
        TrainController service = new Train(channel);
        trainControllers.add(ctx.registerService(TrainController.class, service, dict));
    }

    private <S extends SegmentController> void registerSegment(
            String[] segments, Class<S> type,
            BiFunction<Integer, String, S> factory) {
        Hashtable<String, Object> dict = new Hashtable<>();
        dict.put(Constants.SERVICE_EXPORTED_INTERFACES, "*");
        dict.put(SegmentController.CONTROLLER_ID, 0);

        for (int index = 0; index < segments.length; index++) {
            String segment = segments[index];
            if (!segment.isEmpty()) {
                dict.put(SegmentController.CONTROLLER_SEGMENT, segment);
                S service = factory.apply(index, segment);
                segmentControllers.add(ctx.registerService(type, service, dict));
            }
        }
    }

    @Deactivate
    void unregister() {
        segmentControllers.forEach(ServiceRegistration::unregister);
        trainControllers.forEach(ServiceRegistration::unregister);
    }

    public String toString() {
        return String.format("%s: irLED=%s,\n\tsignals=%s,\n\tswitches=%s,\n\tlocators=%s",
                getClass().getSimpleName(), config.irLed(),
                Arrays.asList(config.signals()),
                Arrays.asList(config.switches()),
                Arrays.asList(config.locators()));
    }

    /**
     * The IR remote control supports 4 devices, each having a "red" and "blue"
     * channel. Typically, the train motor is connected to "red" and the train
     * light to "blue".
     */
    class Train implements TrainController {
        private final int channel;

        public Train(int channel) {
            this.channel = channel;
            if (channel < 1 || channel > 4) {
                throw new IllegalArgumentException("RC channel must be 1-4, but was: " + channel);
            }
        }

        @Override
        public void move(int directionAndSpeed) {
            if (directionAndSpeed < -7 || directionAndSpeed > 7) {
                throw new IllegalArgumentException("RC level must be -7+7, but was: " + directionAndSpeed);
            }
            // TODO: implement move()
            info("move(channel=%d, speed=%d)", channel, directionAndSpeed);
        }

        @Override
        public void light(boolean on) {
            // TODO: implement light()
            info("LIGHT->%s", on ? "on" : "off");
        }

    }

    class Signal implements SignalSegmentController {
        private final int index;
        private final String segName;
        private Color color = Color.RED;

        public Signal(int index, String segName) {
            this.index = index;
            this.segName = segName;
        }

        @Override
        public void signal(Color color) {
            // TODO: implement signal()
            this.color = color;
            info("SIGNAL(%s) -> %s", segName, color);
        }

        @Override
        public Color getSignal() {
            return color;
        }

        @Override
        public String getLocation() {
            return String.format("Signal(host=%s, segment=%s)", location, segName);
        }
    }

    class Switch implements SwitchSegmentController {
        private final int index;
        private final String segName;
        private boolean state;

        public Switch(int index, String segName) {
            this.index = index;
            this.segName = segName;
        }

        @Override
        public void swtch(boolean alt) {
            // TODO: implement signal()
            state = alt;
            info("SWITCH(%s) -> %s", segName, alt ? "ALT" : "NORMAL");
        }

        @Override
        public boolean getSwitch() {
            return state;
        }

        @Override
        public String getLocation() {
            return String.format("Switch(host=%s, segment=%s)", location, segName);
        }
    }

    class Locator implements RFIDSegmentController {
        private final int index;
        private final String segName;
        private String lastTag = null;

        public Locator(int index, String segName) {
            this.index = index;
            this.segName = segName;
        }

        @Override
        public Promise<String> nextRFID() {
            // TODO: implement nextRFID()
            Deferred<String> next = new Deferred<String>();
            next.resolve("dummy-tag");
            return next.getPromise();
        }

        @Override
        public String lastRFID() {
            return lastTag;
        }

        @Override
        public String getLocation() {
            return String.format("Locator(host=%s, segment=%s)", location, segName);
        }
    }

    private void info(String format, Object... args) {
        System.err.printf("HW Controller: " + format + "\n", args);
    }

}
