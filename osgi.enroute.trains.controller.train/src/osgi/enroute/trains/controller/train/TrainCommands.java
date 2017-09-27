package osgi.enroute.trains.controller.train;

import java.nio.ByteBuffer;
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
import osgi.enroute.trains.track.api.TrackObservation;
import osgi.enroute.trains.train.api.TrainCommand;
import osgi.enroute.trains.train.api.TrainController;
import osgi.enroute.trains.train.api.TrainObservation;

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
					System.out.println("Turn light of train "+c.train+" "+(c.on ? "on" : "off"));
					light(c.train, c.on);
					break;
				case MOVE:
					System.out.println("Move "+c.train+" at "+c.directionAndSpeed+" %");
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
		TrainController t = trains.get(train);
		if(t != null){
			t.move(directionAndSpeed);
			try {
				TrainObservation o = new TrainObservation();
				o.time = System.currentTimeMillis();
				o.type = directionAndSpeed == 0 ? TrainObservation.Type.STOPPED : TrainObservation.Type.MOVING;
				o.directionAndSpeed = directionAndSpeed;
				mqtt.publish(TrainObservation.TOPIC, ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		
	}

	public void light(String train, boolean on) {
		TrainController t = trains.get(train);
		if(t != null){
			t.light(on);
			
			try {
				TrainObservation o = new TrainObservation();
				o.time = System.currentTimeMillis();
				o.type = TrainObservation.Type.LIGHT;
				o.on = on;
				mqtt.publish(TrainObservation.TOPIC, ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public Map<String, TrainController> trains() {
	    return trains;
	}
}
