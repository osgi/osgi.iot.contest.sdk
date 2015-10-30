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
@Component(immediate = true, property = { Debug.COMMAND_SCOPE + "=trns", //
		Debug.COMMAND_FUNCTION + "=trns", //
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

	public String color(int controller, String color) {
		SignalSegmentController c = signals.get(controller);
		if (c == null)
			return "No such controller";
		else
			c.signal(Color.valueOf(color));
		return null;
	}

	public Color color(int controller) {
		SignalSegmentController c = signals.get(controller);
		if (c == null)
			return null;
		else
			return c.getSignal();
	}

	public String swtch(int controller, boolean alt) {
		SwitchSegmentController c = switches.get(controller);
		if (c == null)
			return "No such controller";
		else
			c.swtch(alt);;
		return null;
	}

	public boolean swtch(int controller) {
		SwitchSegmentController c = switches.get(controller);
		if (c == null)
			return false;
		else
			return c.getSwitch();
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
	
	public String trns() {
		return "" //
				+ "trns                              help\n"
				+ "signals                           show the signals and their state\n"
				+ "trains                            show the trains\n"
				+ "color <controller> <Color>        set the color of a signal\n"
				+ "color <controller>                get the color of a signal\n"
				+ "swtch <controller> <alt>          set the switch status\n"
				+ "swtch <controller>                get the switch status\n"
				+ "move <name> <speed %>             set the speed of the train\n"
				+ "light <name> <on>                 set the light on or off\n"
				+ "switches                          show the switches status\n"
				;
	}
}
