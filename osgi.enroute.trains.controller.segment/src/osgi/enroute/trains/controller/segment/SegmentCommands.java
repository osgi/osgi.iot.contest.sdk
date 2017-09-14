package osgi.enroute.trains.controller.segment;

import java.nio.ByteBuffer;
import java.util.Collection;
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
import osgi.enroute.trains.segment.api.Color;
import osgi.enroute.trains.segment.api.SegmentCommand;
import osgi.enroute.trains.segment.api.SegmentController;
import osgi.enroute.trains.segment.api.SignalSegmentController;
import osgi.enroute.trains.segment.api.SwitchSegmentController;
import osgi.enroute.trains.track.api.Observation;

/**
 * 
 */
@Component(immediate = true, property = {
		Debug.COMMAND_SCOPE + "=trains",
		Debug.COMMAND_FUNCTION + "=signal", //
		Debug.COMMAND_FUNCTION + "=swtch", //
		Debug.COMMAND_FUNCTION + "=signals", //
		Debug.COMMAND_FUNCTION + "=switches", //
}, service=SegmentCommands.class)
public class SegmentCommands {
	
	@Reference
	protected MQTTService mqtt;
	
	@Reference
	protected Converter converter;
	
	@Activate
	void activate(){
		// listen for SegmentCommands
		try {
			mqtt.subscribe(SegmentCommand.TOPIC).forEach(msg ->{
				SegmentCommand c = converter.convert(msg.payload().array()).to(SegmentCommand.class);
				switch(c.type){
				case SIGNAL:
					System.out.println("Set signal "+c.segment+" to "+c.signal);
					signal(c.segment, c.signal);
					
					break;
				case SWITCH:
					System.out.println("Set switch "+c.segment+" to "+c.alternate);
					swtch(c.segment, c.alternate);
				break;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	final Map<String, SignalSegmentController> signals = new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addSignal(SignalSegmentController t, ServiceReference<?> ref) {
		signals.put((String) ref.getProperty(SegmentController.CONTROLLER_SEGMENT), t);
	}

	void removeSignal(SignalSegmentController s) {
		signals.values().remove(s);
	}

	final Map<String, SwitchSegmentController> switches = new ConcurrentHashMap<>();
	
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addSwitch(SwitchSegmentController t, ServiceReference<?> ref) {
		switches.put((String) ref.getProperty(SegmentController.CONTROLLER_SEGMENT), t);
	}

	void removeSwitch(SwitchSegmentController s) {
		switches.values().remove(s);
	}

	public void signal(String segment, String color){
		signal(segment, Color.valueOf(color.toUpperCase()));
	}
	
	public void signal(String segment, Color color){
		SignalSegmentController c = signals.get(segment);
		if (c != null){
			c.signal(color);
		
			try {
				Observation o = new Observation();
				o.time = System.currentTimeMillis();
				o.type = Observation.Type.SIGNAL;
				o.segment = segment;
				o.signal = color;
				mqtt.publish(Observation.TOPIC, ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
				
			} catch(Exception e){
				System.err.println("Failed to publish observation");
				e.printStackTrace();
			}
		}
	}
	
	public Color signal(String segment) {
		SignalSegmentController c = signals.get(segment);
		if (c == null)
			return null;
		else
			return c.getSignal();
	}

	public void swtch(String segment, boolean alt) {
		SwitchSegmentController c = switches.get(segment);
		if (c != null){
			c.swtch(alt);

			try {
				Observation o = new Observation();
				o.time = System.currentTimeMillis();
				o.type = Observation.Type.SWITCH;
				o.segment = segment;
				o.alternate = alt;
				mqtt.publish(Observation.TOPIC, ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
				
			} catch(Exception e){
				System.err.println("Failed to publish observation");
				e.printStackTrace();
			}
		}
	}

	public boolean swtch(String segment) {
		SwitchSegmentController c = switches.get(segment);
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
}
