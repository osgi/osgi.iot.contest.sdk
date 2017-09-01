package osgi.enroute.trains.track.controller.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import osgi.enroute.trains.segment.api.Color;
import osgi.enroute.trains.segment.api.RFIDSegmentController;
import osgi.enroute.trains.segment.api.SegmentController;
import osgi.enroute.trains.segment.api.SignalSegmentController;
import osgi.enroute.trains.segment.api.SwitchSegmentController;
import osgi.enroute.trains.segment.api.TrainLocator;
import osgi.enroute.trains.track.api.Segment;
import osgi.enroute.trains.track.api.TrackCommand;
import osgi.enroute.trains.track.api.TrackForSegment;
import osgi.enroute.trains.track.api.TrackCommand.Type;

/**
 * The TrackController listens for Command events and performs those on the
 * right Segment Controller
 */
@Component(name = "osgi.enroute.trains.track.controller",
        property = { "event.topics=" + TrackCommand.TOPIC },
        immediate = true)
public class TrackControllerImpl implements EventHandler {
    static Logger logger = LoggerFactory.getLogger(TrackControllerImpl.class);

    private TrackForSegment trackManager;

    private Map<Integer, RFIDSegmentController> rfids = Collections
            .synchronizedMap(new HashMap<Integer, RFIDSegmentController>());
    private Map<Integer, SignalSegmentController> signals = Collections
            .synchronizedMap(new HashMap<Integer, SignalSegmentController>());
    private Map<Integer, SwitchSegmentController> switches = Collections
            .synchronizedMap(new HashMap<Integer, SwitchSegmentController>());

    @Override
    public void handleEvent(Event event) {
        Type type = (Type) event.getProperty("type");
        String s = (String) event.getProperty("segment");

        Segment segment = trackManager.getSegments().get(s);

        if (segment == null) {
            error("Segment <{}> does not exist", s);
            return;
        }

        if (!segment.type.toString().equals(type.toString())) {
            error("Event type<{}> doesn't match Segment type<{}>", type, segment.type);
            return;
        }

        switch (type) {

        case SIGNAL:
            Color color = (Color) event.getProperty("signal");

            SignalSegmentController sigCtrl = signals.get(segment.controller);
            if (sigCtrl == null) {
                error("signal controller <{}> not found", segment.controller);
            } else {
                sigCtrl.signal(color);
            }
            trackManager.signal(s, color);
            break;

        case SWITCH:
            Boolean target = (Boolean) event.getProperty("alternate");
            SwitchSegmentController swCtrl = switches.get(segment.controller);

            if (swCtrl == null) {
                error("switch controller <{}> not found", segment.controller);
            } else {
                swCtrl.swtch(target);
            }
            trackManager.switched(s, target);
            break;

        }
    }

    // use RFID/bluetooth TrainLocator service
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addTrainLocator(TrainLocator locator) {
        info("Found TrainLocator {}", locator);
        locateTrain(locator);
    }

    void removeTrainLocator(TrainLocator locator) {
        info("Lost TrainLocator {}", locator);
    }

    private void locateTrain(TrainLocator locator) {
        locator.nextLocation().then(p -> {
            String[] split = p.getValue().split("\\s*:\\s*", 2);
            String trainId = split[0];
            String segment = split[1];
            //info("Located trainId<{}> at segment<{}>", trainId, segment);
            try {
                trackManager.locatedTrainAt(trainId, segment);
            } catch (IllegalArgumentException e) {
                error("bad train or segment! {}", e.toString());
            }
            locateTrain(locator);
            return null;
        });
    }

    private void startTracking(int controller) {
        if (trackManager != null) {
            Segment rfidSegment = null;
            for (Segment s : trackManager.getSegments().values()) {
                if (s.controller == controller) {
                    rfidSegment = s;
                }
            }

            // track new rfid detections on this controller
            if (rfidSegment != null) {
                String segment = rfidSegment.id;
                trackRFID(controller, segment);
            }
        }
    }

    private void trackRFID(final int controller, final String segment) {
        // check whether this controller is still valid
        RFIDSegmentController c = rfids.get(controller);
        if (c != null) {
            c.nextRFID().then((p) -> {
                // notify track manager when a train passes by
                String train = p.getValue();
                trackManager.locatedTrainAt(train, segment);
                return null;
            }, p -> {
                // Paremus RSA has 30s timeout on remote service
                // can be changed by setting com.paremus.dosgi.net.timeout=60000
                // info("RSA timeout?: " + p.getFailure());
                trackRFID(controller, segment);
            }).then((p) -> {
                // and then start tracking the next one
                trackRFID(controller, segment);
                return null;
            });
        }
    }

    @Reference
    public void setTrackManager(TrackForSegment tm) {
        this.trackManager = tm;

        // start tracking RFID notifications for any previously registered
        // RFIDControllers
        synchronized (rfids) {
            for (int c : rfids.keySet()) {
                startTracking(c);
            }
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addRFIDController(RFIDSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        rfids.put(id, c);

        // start tracking RFID notifications
        startTracking(id);
    }

    public void removeRFIDController(RFIDSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        rfids.remove(id);
    }
    
    private Optional<String> getSegment(int controller) {
        return trackManager.getSegments().values().stream()
                .filter(s -> s.controller == controller)
                .map(s -> s.id)
                .findFirst();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addSignalController(SignalSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        signals.put(id, c);

        Optional<String> segment = getSegment(id);

        if (segment.isPresent()) {
            Color color = c.getSignal() ;
            info("initialise signal<{}> to {}", segment.get(), color);
            trackManager.signal(segment.get(), color);
        }
    }

    public void removeSignalController(SignalSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        signals.remove(id);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addSwitchController(SwitchSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        switches.put(id, c);

        Optional<String> segment = getSegment(id);
        
        if (segment.isPresent()) {
            boolean alt = c.getSwitch();
            info("initialise switch<{}> to {}", segment.get(), alt ? "ALT" : "NORMAL");
            trackManager.switched(segment.get(), alt);
        }
    }

    public void removeSwitchController(SwitchSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        switches.remove(id);
    }

    private static void error(String fmt, Object... args) {
        System.err.printf("ERROR: " + fmt.replaceAll("\\{}", "%s") + "\n", args);
        logger.error(fmt, args);
    }

    private static void info(String fmt, Object... args) {
        System.err.printf("TrackCtl: " + fmt.replaceAll("\\{}", "%s") + "\n", args);
        logger.info(fmt, args);
    }

}
