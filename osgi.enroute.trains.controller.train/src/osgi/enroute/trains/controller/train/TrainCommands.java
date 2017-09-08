package osgi.enroute.trains.controller.train;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.converter.Converter;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.trains.train.api.TrainCommand;
import osgi.enroute.trains.train.api.TrainController;

/**
 * 
 */
@Component(immediate = true, property = {
		Debug.COMMAND_SCOPE +"=trains",
		Debug.COMMAND_FUNCTION + "=trains", //
		Debug.COMMAND_FUNCTION + "=move", //
		Debug.COMMAND_FUNCTION + "=light", //
}, service=TrainCommands.class)
public class TrainCommands {

	@Reference
	protected MQTTService mqtt;
	
	@Reference
	protected Converter converter;
	
	@Activate
	void activate(){
		// listen for SegmentCommands
		try {
			mqtt.subscribe(TrainCommand.TOPIC).forEach(msg ->{
				TrainCommand c = converter.convert(msg.payload().array()).to(TrainCommand.class);
				switch(c.type){
				case LIGHT:
					light(c.train, c.on);
					
					break;
				case MOVE:
					move(c.train, c.directionAndSpeed);
					break;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}	
	final Map<String, TrainController> trains = new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addTrain(TrainController t, ServiceReference<?> ref) {
		String train = (String) ref.getProperty(TrainController.CONTROLLER_TRAIN);
		trains.put(train, t);
	}

	void removeTrain(TrainController t) {
		trains.values().remove(t);
	}

	public void move(String train, int directionAndSpeed) {
		System.out.println("Move "+train+" at "+directionAndSpeed+" %");
		TrainController t = trains.get(train);
		if(t != null){
			t.move(directionAndSpeed);
			
			// TODO publish observation?
		}
		
	}

	public void light(String train, boolean on) {
		TrainController t = trains.get(train);
		if(t != null){
			t.light(on);
			
			// TODO publish observation?
		}
	}

	public Map<String, TrainController> trains() {
	    return trains;
	}
}
