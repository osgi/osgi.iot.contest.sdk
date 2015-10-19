package osgi.enroute.trains.track.manager.example.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.cloud.api.Segment.Type;
import osgi.enroute.trains.cloud.api.TrackInfo;
import osgi.enroute.trains.controller.api.RFIDSegmentController;
import osgi.enroute.trains.controller.api.SegmentController;
import osgi.enroute.trains.controller.api.SignalSegmentController;
import osgi.enroute.trains.controller.api.SwitchSegmentController;

/**
 * Discover SegmentControllers and TrainControllers, published from (remote) Raspberry Pis
 */
@Component(immediate = true)
public class ExampleHardwareDetector {

    private Map<Integer, String> id2name = null;
    private Map<String, SignalSegmentController> signals = new ConcurrentHashMap<>();
    private Map<String, SwitchSegmentController> switches = new ConcurrentHashMap<>();
    private Map<String, RFIDSegmentController> locators = new ConcurrentHashMap<>();

    @Reference
    void setTrackInfo(TrackInfo ti) {
        try {
            id2name = ti.getSegments().values().stream().filter(s -> s.controller >= 0)
                    .collect(Collectors.toMap(s -> s.controller, s -> s.id + ":" + s.type));
            info("TrackInfo=%s", id2name);
        } catch (IllegalStateException e) {
            warn("CONFIGURATION ERROR duplicate controller.id=?: %s", e.getMessage());
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addSignalSegmentController(SignalSegmentController sc, Map<String, Object> config) {
        String name = getSegmentName(config, Type.SIGNAL);
        if (name != null) {
            signals.put(name, sc);
            info("add SIGNAL(%s)", name);
        }
    }

    void removeSignalSegmentController(SignalSegmentController sc, Map<String, Object> config) {
        String name = getSegmentName(config, Type.SIGNAL);
        if (name != null) {
            signals.remove(name);
            info("remove SIGNAL(%s)", name);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addSwitchSegmentController(SwitchSegmentController sc, Map<String, Object> config) {
        String name = getSegmentName(config, Type.SWITCH);
        if (name != null) {
            switches.put(name, sc);
            info("add SWITCH(%s)", name);
        }
    }

    void removeSwitchSegmentController(SwitchSegmentController sc, Map<String, Object> config) {
        String name = getSegmentName(config, Type.SWITCH);
        if (name != null) {
            switches.remove(name);
            info("remove SWITCH(%s)", name);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void addLocatorSegmentController(RFIDSegmentController sc, Map<String, Object> config) {
        String name = getSegmentName(config, Type.LOCATOR);
        if (name != null) {
            locators.put(name, sc);
            info("add LOCATOR(%s)", name);
        }
    }

    void removeLocatorSegmentController(RFIDSegmentController sc, Map<String, Object> config) {
        String name = getSegmentName(config, Type.LOCATOR);
        if (name != null) {
            locators.remove(name);
            info("remove LOCATOR(%s)", name);
        }
    }

    private String getSegmentName(Map<String, Object> config, Segment.Type type) {
        String name = (String) config.get(SegmentController.CONTROLLER_SEGMENT);
        Integer id = (Integer) config.get(SegmentController.CONTROLLER_ID);

        if (id2name == null) {
            warn("Can't check %s(name=%s, id=%s) - TrackInfo not available", type, name, id);
        }
        else if (name != null) {
            if (!id2name.values().contains(name + ":" + type)) {
                warn("unexpected %s(name=%s)", type, name);
                name = null;
            }
        }
        else if (id != null) {
            name = id2name.get(id);
            if (name == null) {
                warn("unexpected %s(id=%s)", type, id);
            }
            else {
                name = name.replaceFirst(":.*", "");
            }
        }

        return name;
    }

    void info(String format, Object... args) {
        System.out.printf("HardwareDetector: " + format + "\n", args);
    }

    void warn(String format, Object... args) {
        System.err.printf("HardwareDetector: " + format + "\n", args);
    }

}
