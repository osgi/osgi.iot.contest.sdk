package osgi.enroute.trains.hardware.commands;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import osgi.enroute.trains.segment.api.Color;
import osgi.enroute.trains.segment.api.RFIDSegmentController;
import osgi.enroute.trains.segment.api.SegmentController;
import osgi.enroute.trains.segment.api.SignalSegmentController;
import osgi.enroute.trains.segment.api.SwitchSegmentController;
import osgi.enroute.trains.track.api.Segment;
import osgi.enroute.trains.track.api.TrackManager;
import osgi.enroute.trains.track.api.Segment.Type;

/**
 * Simple GoGo commands to access SegmentControllers published from (remote)
 * Raspberry Pis
 */
@Component(immediate = true, service = Object.class, property = { "osgi.command.scope=trainhw",
		"osgi.command.function=trackinfo", "osgi.command.function=signal", "osgi.command.function=switch",
		"osgi.command.function=locator", })
public class HardwareCommands {
	// private static final String remote = "(" +
	// Constants.SERVICE_EXPORTED_INTERFACES + "=*)";
	private static final String remote = "(" + SegmentController.CONTROLLER_ID + "=0)";

	private Map<Integer, String> id2name = null;
	private Map<String, SignalSegmentController> signals = new ConcurrentHashMap<>();
	private Map<String, SwitchSegmentController> switches = new ConcurrentHashMap<>();
	private Map<String, RFIDSegmentController> locators = new ConcurrentHashMap<>();

	private boolean info = false;

	@Reference
	void setTrackInfo(TrackManager ti) {
		try {
			id2name = ti.getSegments().values().stream().filter(s -> s.controller >= 0)
					.collect(Collectors.toMap(s -> s.controller, s -> s.id + ":" + s.type));
			info("TrackInfo=%s", id2name);
		} catch (IllegalStateException e) {
			warn("CONFIGURATION ERROR duplicate controller.id=?: %s", e.getMessage());
		}
	}

	@Reference(target = remote, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
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

	@Reference(target = remote, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
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

	@Reference(target = remote, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
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
		} else if (name != null) {
			if (!id2name.values().contains(name + ":" + type)) {
				warn("unexpected %s(name=%s)", type, name);
				name = null;
			}
		} else if (id != null) {
			name = id2name.get(id);
			if (name == null) {
				warn("unexpected %s(id=%s)", type, id);
			} else {
				name = name.replaceFirst(":.*", "");
			}
		}

		return name;
	}

	void info(String format, Object... args) {
		if (info) {
			System.out.printf("HardwareCommands: " + format + "\n", args);
		}
	}

	void warn(String format, Object... args) {
		System.err.printf("HardwareCommands: " + format + "\n", args);
	}

	public void signal(String[] args) {
		switch (args.length) {
		case 0:
			getControllers("SIGNAL", signals)
					.forEach((k, v) -> System.out.printf("%s -> %s\n", k, (v != null ? v.getLocation() : null)));
			break;
		case 1:
		case 2:
			String arg0 = args[0];
			SignalSegmentController ssc = signals.get(arg0);
			if (ssc == null)
				throw new IllegalArgumentException("No such signal: " + arg0);
			if (args.length == 1) {
				System.out.printf("signal(%s) is %s\n", arg0, ssc.getSignal());
			} else {
				ssc.signal(Color.valueOf(args[1].toUpperCase()));
			}
			break;
		default:
			throw new IllegalArgumentException("Usage: signal [[segmentName [color]]");
		}
	}

	public void _switch(String[] args) {
		switch (args.length) {
		case 0:
			getControllers("SWITCH", switches)
					.forEach((k, v) -> System.out.printf("%s -> %s\n", k, (v != null ? v.getLocation() : null)));
			break;
		case 1:
		case 2:
			String arg0 = args[0];
			SwitchSegmentController ssc = switches.get(arg0);
			if (ssc == null)
				throw new IllegalArgumentException("No such switch: " + arg0);
			if (args.length == 1) {
				System.out.printf("switch(%s) is %s\n", arg0, ssc.getSwitch() ? "ALT" : "NORMAL");
			} else
				switch (args[1]) {
				case "normal":
				case "NORMAL":
					ssc.swtch(false);
					break;
				case "alt":
				case "ALT":
					ssc.swtch(true);
					break;
				default:
					throw new IllegalArgumentException("Usage: switch [[segmentName [NORMAL | ALT]]");
				}
			break;
		default:
			throw new IllegalArgumentException("Usage: switch [[segmentName [NORMAL | ALT]]");
		}
	}

	public void locator(String[] args) {
		switch (args.length) {
		case 0:
			getControllers("LOCATOR", locators)
					.forEach((k, v) -> System.out.printf("%s -> %s\n", k, (v != null ? v.getLocation() : null)));
			break;
		case 1:
			String arg0 = args[0];
			RFIDSegmentController rsc = locators.get(arg0);
			if (rsc == null)
				throw new IllegalArgumentException("No such locator: " + arg0);
			System.out.printf("locator(%s) last location was %s\n", arg0, rsc.lastRFID());
			break;
		default:
			throw new IllegalArgumentException("Usage: locator [segmentName]");
		}
	}

	public void trackinfo() {
		System.out.println(id2name);
	}

	public void trackinfo(boolean info) {
		this.info = info;
		System.out.println("Console info messages " + (info ? "on" : "off"));
	}

	private Map<String, SegmentController> getControllers(String type,
			Map<String, ? extends SegmentController> installed) {
		Map<String, SegmentController> controllers = new TreeMap<>();
		for (String nameType : id2name.values()) {
			if (nameType.endsWith(":" + type)) {
				String name = nameType.replaceFirst(":.*", "");
				SegmentController value = installed.get(name);
				controllers.put(name, value);
			}
		}
		return controllers;
	}

}
