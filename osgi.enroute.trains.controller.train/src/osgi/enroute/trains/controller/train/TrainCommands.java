package osgi.enroute.trains.controller.train;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.trains.train.api.TrainController;

/**
 * 
 */
@Component(immediate = true, property = {
		Debug.COMMAND_FUNCTION + "=trains", //
		Debug.COMMAND_FUNCTION + "=move", //
		Debug.COMMAND_FUNCTION + "=light", //
}, service=TrainCommands.class)
public class TrainCommands {

	// TODO also listen for TrainCommand
	
	final Map<String, TrainController> trains = new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addTrain(TrainController t, ServiceReference<?> ref) {
		String ch = (String) ref.getProperty("channel");
		trains.put(ch.toLowerCase(), t);
	}

	void removeTrain(TrainController t) {
		trains.values().remove(t);
	}

	public void move(String ch, int directionAndSpeed) {
		trains.get(ch).move(directionAndSpeed);
	}

	public void light(String ch, boolean on) {
		trains.get(ch).light(on);
	}

	public Map<String, TrainController> trains() {
	    return trains;
	}
}
