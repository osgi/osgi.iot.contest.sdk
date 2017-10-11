package osgi.enroute.trains.train.manager;

import java.nio.ByteBuffer;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.converter.Converter;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.trains.track.api.Segment;
import osgi.enroute.trains.track.api.TrackManager;
import osgi.enroute.trains.track.api.TrackObservation;
import osgi.enroute.trains.train.api.Assignment;
import osgi.enroute.trains.train.api.TrainCommand;
import osgi.enroute.trains.train.api.TrainCommand.Type;
import osgi.enroute.trains.train.api.TrainManager;

/**
 * Train manager.
 */
@Component(immediate = true,
	name="osgi.enroute.trains.train.manager",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	property = {
			Debug.COMMAND_FUNCTION + "=assign", //
			Debug.COMMAND_FUNCTION + "=stop",
			Debug.COMMAND_FUNCTION + "=speed"
	})
public class TrainManagerImpl implements TrainManager {

	private Config config;
	
	// estimated avg time interval between segments
	private long interval = -1;
	
	private int speed = 40;
	
	// segment we think we are on
	private String currentSegment = null;
	
	// current target
	private String currentAssignment = null;
	
	// current access
	private String currentAccess = null;
	
	@Reference
	private Converter converter;

	@Reference
	private MQTTService mqtt;
	
	@Reference
	private TrackManager trackManager;

	@ObjectClassDefinition
	@interface Config {
		String name();
	}
	
	@Activate
	void activate(Config config) throws Exception {
		this.config = config;
		
		mqtt.subscribe(Assignment.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(Assignment.class))
			.filter(a -> a.train.equals(config.name()))
			.forEach(a -> {
				switch(a.type){
				case ASSIGN:
					// new assignment for this train
					System.out.println("New assignment for "+a.train);
					assign(a.segment);
					break;
				case ABORT:
					System.out.println("Abort assignment for "+a.train);
					assignmentAborted();
					break;
				}
			});
		
		mqtt.subscribe(TrackObservation.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(TrackObservation.class))
			.filter(o -> o.type == TrackObservation.Type.LOCATED)
			.filter(o -> o.train.equals(config.name()))
			.forEach(o -> {
				// new observation of this train
				observation(o);
			});
	}
	
	@Override
	public void assign(String toSegment) {
		System.out.println("New assignment "+toSegment+" for "+config.name());
		currentAssignment = toSegment;
		if(currentSegment != null){
			updateRoute();
		}
	}
	
	@Override
	public void abort(){
		assignmentAborted();
	}
	
	@Override
	public void speed(int speed){
		this.speed = speed;
	}
	
	@Override
	public int speed(){
		return this.speed;
	}
	
	private void observation(TrackObservation o){
		long timeSinceLastObservation = (System.currentTimeMillis() - interval);
		System.out.println("Train "+config.name()+" located at "+o.segment+" ( after "+timeSinceLastObservation+" ms)");
		if(o.segment == null){
			// invalid located event!?
			return;
		}
		interval = System.currentTimeMillis();
		
		currentSegment = o.segment;
		
		if(currentAssignment == null){
			// no assignment, just stop
			halt();
		} else {
			updateRoute();
		}
	}
	
	private void updateRoute(){
		// if segment is the assignment, we are done
		if(currentSegment.equals(currentAssignment)){
			assignmentReached();
			return;
		}

		// calculate the remaining route
		List<Segment> remainingRoute = trackManager.planRoute(currentSegment, currentAssignment, config.name());
		//System.out.println("Route remaining: "+remainingRoute);
		if(remainingRoute == null){
			// no route existing? abort
			assignmentAborted();
		}
		
		// check whether we have to request access to a track
		String fromTrack = remainingRoute.get(0).track;
		String toTrack = remainingRoute.get(Math.min(5, remainingRoute.size()-1)).track;
		
		if (!fromTrack.equals(toTrack) && !toTrack.equals(currentAccess)) {
			halt();
			// ok to block here?
			System.out.println("Train "+config.name()+" requests access from "+fromTrack+" to "+toTrack);
			boolean granted = trackManager.requestAccessTo(config.name(), fromTrack, toTrack);
			
			if(granted){
				if(currentAssignment != null){
					currentAccess = toTrack;
					move(speed);
				}
			} else {
				System.out.println("Train "+config.name()+" did not get access to "+toTrack+", aborting assignment?");
				assignmentAborted();
			}
		} else {
			move(speed);
		}
	}
	

	private void move(int speed){
		TrainCommand c = new TrainCommand();
		c.type = Type.MOVE;
		c.train = config.name();
		c.directionAndSpeed = speed;
		command(c);
	}
	
	private void halt(){
		TrainCommand c = new TrainCommand();
		c.type = Type.MOVE;
		c.train = config.name();
		c.directionAndSpeed = 0;
		command(c);
	}
	
	private void command(TrainCommand c){
		try {
			mqtt.publish(TrainCommand.TOPIC,  ByteBuffer.wrap( converter.convert(c).to(byte[].class)));
		} catch(Exception e){
			System.out.println("Failed publishing command");
		}
	}
	
	private void assignmentReached(){
		halt();
		System.out.println("Train "+config.name()+" reached assignment!");
		
		Assignment a = new Assignment();
		a.type = Assignment.Type.REACHED;
		a.segment = currentAssignment;
		a.train = config.name();
		
		try {
			mqtt.publish(Assignment.TOPIC,  ByteBuffer.wrap( converter.convert(a).to(byte[].class)));
		} catch(Exception e){
			System.out.println("Failed publishing assignment reached");
		}
		
		currentAssignment = null;
	}
	
	private void assignmentAborted(){
		halt();

		Assignment a = new Assignment();
		a.type = Assignment.Type.ABORTED;
		a.segment = currentAssignment;
		a.train = config.name();
		
		try {
			mqtt.publish(Assignment.TOPIC,  ByteBuffer.wrap( converter.convert(a).to(byte[].class)));
		} catch(Exception e){
			System.out.println("Failed publishing assignment aborted");
		}
		
		currentAssignment = null;
	}
}
