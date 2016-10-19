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
import osgi.enroute.trains.controller.api.SegmentController;
import osgi.enroute.trains.controller.api.SignalSegmentController;
import osgi.enroute.trains.controller.api.SwitchSegmentController;
import osgi.enroute.trains.train.api.TrainController;

/**
 * 
 */
@Component(immediate = true, property = { Debug.COMMAND_SCOPE + "=trns", //
		Debug.COMMAND_FUNCTION + "=trns", //
		Debug.COMMAND_FUNCTION + "=trains", //
		Debug.COMMAND_FUNCTION + "=move", //
		Debug.COMMAND_FUNCTION + "=light", //
		Debug.COMMAND_FUNCTION + "=signal", //
		Debug.COMMAND_FUNCTION + "=switch", //
}, service=Command.class)
public class Command {
	final Map<String, TrainController> trains = new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addTrain(TrainController t, ServiceReference<?> ref) {

		String ch = (String) ref.getProperty("channel");
		trains.put(ch.toLowerCase(), t);
	}

	void removeTrain(TrainController t) {
		trains.values().remove(t);
	}

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

	public void move(String ch, int directionAndSpeed) {
		trains.get(ch).move(directionAndSpeed);
	}

	public void light(String ch, boolean on) {
		trains.get(ch).light(on);
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
	
	public Map<String, TrainController> trains() {
	    return trains;
	}
	
	public String trns() {
		return "" //
				+ "trns                              help\n"
				+ "signal                            show the signals and their state\n"
				+ "signal <controller> <Color>       set the color of a signal\n"
				+ "signal <controller>               get the color of a signal\n"
				+ "switch                            show the switches status\n"
				+ "switch <controller> <alt>         set the switch status\n"
				+ "switch <controller>               get the switch status\n"
				+ "trains                            show the trains\n"
				+ "move <ch> <speed %>               set the speed of the train\n"
				+ "light <ch> <on>                   set the light on or off\n"
				;
	}
}
