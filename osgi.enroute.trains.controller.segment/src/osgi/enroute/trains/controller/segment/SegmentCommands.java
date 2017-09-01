package osgi.enroute.trains.controller.segment;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.trains.segment.api.Color;
import osgi.enroute.trains.segment.api.SegmentController;
import osgi.enroute.trains.segment.api.SignalSegmentController;
import osgi.enroute.trains.segment.api.SwitchSegmentController;

/**
 * 
 */
@Component(immediate = true, property = { 
		Debug.COMMAND_FUNCTION + "=signal", //
		Debug.COMMAND_FUNCTION + "=switch", //
}, service=SegmentCommands.class)
public class SegmentCommands {
	
	// TODO also listen for SegmentCommand
	
	final Map<Integer, SignalSegmentController> signals = new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addSignal(SignalSegmentController t, ServiceReference<?> ref) {
		signals.put((Integer) ref.getProperty(SegmentController.CONTROLLER_ID), t);
	}

	void removeSignal(SignalSegmentController s) {
		signals.values().remove(s);
	}

	final Map<Integer, SwitchSegmentController> switches = new ConcurrentHashMap<>();
	
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addSwitch(SwitchSegmentController t, ServiceReference<?> ref) {
		switches.put((Integer) ref.getProperty(SegmentController.CONTROLLER_ID), t);
	}

	void removeSwitch(SwitchSegmentController s) {
		switches.values().remove(s);
	}

	public String signal(int controller, String color) {
		SignalSegmentController c = signals.get(controller);
		if (c == null)
			return "No such controller";
		else
			c.signal(Color.valueOf(color.toUpperCase()));
		return null;
	}

	public Color signal(int controller) {
		SignalSegmentController c = signals.get(controller);
		if (c == null)
			return null;
		else
			return c.getSignal();
	}

	public String _switch(int controller, boolean alt) {
		SwitchSegmentController c = switches.get(controller);
		if (c == null)
			return "No such controller";
		else
			c.swtch(alt);;
		return null;
	}

	public boolean _switch(int controller) {
		SwitchSegmentController c = switches.get(controller);
		if (c == null)
			return false;
		else
			return c.getSwitch();
	}

	public Collection<SignalSegmentController> signal() {
		return signals.values();
	}

	public Collection<SwitchSegmentController> _switch() {
		return switches.values();
	}
}
