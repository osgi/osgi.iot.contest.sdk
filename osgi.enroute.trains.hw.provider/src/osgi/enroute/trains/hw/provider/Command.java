package osgi.enroute.trains.hw.provider;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.trains.cloud.api.Color;
import osgi.enroute.trains.controller.api.SignalSegmentController;
import osgi.enroute.trains.controller.api.SwitchSegmentController;
import osgi.enroute.trains.train.api.TrainController;

/**
 * 
 */
@Component(immediate = true, property = { Debug.COMMAND_SCOPE + "=tc", //
		Debug.COMMAND_FUNCTION + "=tc", //
		Debug.COMMAND_FUNCTION + "=trains", //
		Debug.COMMAND_FUNCTION + "=signals", //
		Debug.COMMAND_FUNCTION + "=switches", //
		Debug.COMMAND_FUNCTION + "=move", //
		Debug.COMMAND_FUNCTION + "=light", //
		Debug.COMMAND_FUNCTION + "=color",//
}, service=Command.class)
public class Command {
	final Map<String, TrainController> trains = new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addTrain(TrainController t, ServiceReference<?> ref) {
		trains.put((String) ref.getProperty("train.name"), t);
	}

	void removeTrain(TrainController t) {
		trains.values().remove(t);
	}

	final Map<Integer, SignalSegmentController> signals = new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addSignal(SignalSegmentController t, ServiceReference<?> ref) {
		signals.put((Integer) ref.getProperty("controller"), t);
	}

	void removeSignal(SignalSegmentController s) {
		signals.values().remove(s);
	}

	final Map<Integer, SwitchSegmentController> switches = new ConcurrentHashMap<>();
	
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addSwitch(SwitchSegmentController t, ServiceReference<?> ref) {
		switches.put((Integer) ref.getProperty("controller"), t);
	}

	void removeSwitch(SwitchSegmentController s) {
		switches.values().remove(s);
	}
	public void move(String train, int directionAndSpeed) {
		trains.get(train).move(directionAndSpeed);
	}

	public void light(String train, boolean on) {
		trains.get(train).light(on);
	}

	public String color(int controller, Color color) {
		SignalSegmentController c = signals.get(controller);
		if (c == null)
			return "No such controller";
		else
			c.signal(color);
		return null;
	}

	public Color color(int controller) {
		SignalSegmentController c = signals.get(controller);
		if (c == null)
			return null;
		else
			return c.getSignal();
	}

	public Collection<SignalSegmentController> signals() {
		return signals.values();
	}

	public Collection<SwitchSegmentController> switches() {
		return switches.values();
	}
	
	public Collection<TrainController> trains() {
		return trains.values();
	}
}
