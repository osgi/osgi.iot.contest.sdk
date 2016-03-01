package osgi.enroute.trains.track.controller.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import osgi.enroute.trains.cloud.api.Color;
import osgi.enroute.trains.cloud.api.Command;
import osgi.enroute.trains.cloud.api.Command.Type;
import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.cloud.api.TrackForSegment;
import osgi.enroute.trains.controller.api.RFIDSegmentController;
import osgi.enroute.trains.controller.api.SegmentController;
import osgi.enroute.trains.controller.api.SignalSegmentController;
import osgi.enroute.trains.controller.api.SwitchSegmentController;
import osgi.enroute.trains.controller.api.TrainLocator;

/**
 * The TrackController listens for Command events and performs those on the
 * right Segment Controller
 */
@Component(name = "osgi.enroute.trains.track.controller",
        property = { "event.topics=" + Command.TOPIC },
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
    
    private Map<Integer, Boolean> sw = new HashMap<>();

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
            SwitchSegmentController swCtrl = switches.get(segment.controller);
            Boolean target = false;
            
            if (swCtrl == null) {
                error("switch controller <{}> not found", segment.controller);
                target = sw.get(segment.controller);
                if (target == null) {
                    target = true;
                }
                sw.put(segment.controller, !target);
            } else {
                target = !swCtrl.getSwitch();
                swCtrl.swtch(target);
            }
            trackManager.switched(s, target);
            break;

        }
    }

    // use RFID/bluetooth TrainLocator service
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    void setTrainLocator(TrainLocator locator) {
        trackLocation(locator);
    }

    private void trackLocation(TrainLocator locator) {
        locator.nextLocation().then(p -> {
            String[] split = p.getValue().split("\\s*:\\s*", 2);
            String train = split[0];
            String segment = split[1];
            System.err.printf("Located train<%s> at segment<%s>\n", train, segment);
            try {
                trackManager.locatedTrainAt(train, segment);
            } catch (Exception e) {
                System.err.printf("XXX eek! %s\n", e.toString());
            }
            trackLocation(locator);
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
            } , p -> {
                System.out.println("Retry rfid check: " + p.getFailure());
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

        // publish initial states
        synchronized (switches) {

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

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addSignalController(SignalSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        signals.put(id, c);
    }

    public void removeSignalController(SignalSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        signals.remove(id);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addSwitchController(SwitchSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        switches.put(id, c);
    }

    public void removeSwitchController(SwitchSegmentController c, Map<String, Object> properties) {
        int id = (Integer) properties.get(SegmentController.CONTROLLER_ID);
        switches.remove(id);
    }

    private static void error(String fmt, Object... args) {
        System.err.printf("ERROR: " + fmt.replaceAll("\\{}", "%s") + "\n", args);
        logger.error(fmt, args);
    }
}
