package osgi.enroute.trains.train.manager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.converter.Converter;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.scheduler.api.Scheduler;
import osgi.enroute.trains.track.api.Observation;
import osgi.enroute.trains.track.api.Segment;
import osgi.enroute.trains.track.api.TrackManager;
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
	})
public class TrainManagerImpl implements TrainManager{

	private Config config;
	
	// estimated avg time interval between segments
	private long interval = -1;
	
	// last observation of this train
	private Observation lastObservation = null;

	// segment we think we are on
	private String currentSegment = null;
	
	// current target
	private String currentAssignment = null;
	
	// current route
	private List<Segment> currentRoute = null;
	
	@Reference
	private Scheduler scheduler;

	@Reference
	private Converter converter;

	@Reference
	private MQTTService mqtt;
	
	@Reference
	private TrackManager trackManager;

	@ObjectClassDefinition
	@interface Config {
		String name();

		int speed() default 50;
	}
	
	@Activate
	void activate(Config config) throws Exception {
		this.config = config;
		
		mqtt.subscribe(Assignment.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(Assignment.class))
			.filter(a -> a.type == Assignment.Type.ASSIGN)
			.filter(a -> a.train.equals(config.name()))
			.forEach(a -> {
				// new assignment for this train
				assign(a.segment);
			});
		
		mqtt.subscribe(Observation.TOPIC)
			.map(msg -> converter.convert(msg.payload().array()).to(Observation.class))
			.filter(o -> o.type == Observation.Type.LOCATED)
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
			startRoute();
		}
	}
	
	private void observation(Observation o){
		if(lastObservation == null){
			// first observation, now we know where we are at
			currentSegment = o.segment;
			
			if(currentAssignment == null){
				// no assignment, just stop
				stop();
			} else {
				startRoute();
			}
		} else {
			System.out.println("Train located at "+o.segment);
			if(o.segment == null){
				// invalid located event!?
				return;
			}
			System.out.println("Observation interval: "+(System.currentTimeMillis() - interval));
			interval = System.currentTimeMillis();
			// update the route 
			if(currentRoute != null)
				updateRoute(o.segment);
		}
		
		lastObservation = o;
	}
	
	private void startRoute(){
		System.out.println("Plan route "+currentSegment+"->"+currentAssignment);
		currentRoute = trackManager.planRoute(currentSegment, currentAssignment);
		System.out.println("Start route "+currentRoute);
		if(currentRoute == null){
			// no route existing? abort
			assignmentAborted();
		}
		move();	
	}
	
	private void updateRoute(String segment){
		// we are now at segment
		if(currentSegment.equals(segment)){
			// nothing to be done?
			return;
		}
		
		this.currentSegment = segment;
		
		// if segment is the assignment, we are done
		if(segment.equals(currentAssignment)){
			assignmentReached();
			return;
		}

		// check where we are at the route
		int index = 0;
		for(Segment s : currentRoute){
			if(s.id.equals(currentSegment)){
				break;
			}
			index++;
		}
		
		if(index >= currentRoute.size()){
			// we are no longer on the route?! stop and plan again...
			System.out.println("Train got off the route?! Stop and reschedule...");
			stop();
			startRoute();
			return;
		}
		
		// update the remaining route
		List<Segment> remainingRoute = new ArrayList<>();
		for(int i=index;i<currentRoute.size();i++){
			remainingRoute.add(currentRoute.get(i));
		}
		System.out.println("Route remaining: "+remainingRoute);
		
		// check whether we have to request access to a track
		String fromTrack = remainingRoute.get(0).track;
		String toTrack = remainingRoute.get(Math.min(4, remainingRoute.size()-1)).track;
		
		if (!fromTrack.equals(toTrack)) {
			stop();
			// ok to block here?
			System.out.println("Train "+config.name()+" requests access from "+fromTrack+" to "+toTrack);
			boolean granted = trackManager.requestAccessTo(config.name(), fromTrack, toTrack);
			
			if(granted){
				move();
			} else {
				System.out.println("Train "+config.name()+" did not get access to "+toTrack+", aborting assignment?");
				assignmentAborted();
			}
      }
		
	}
	

	private void move(){
		TrainCommand c = new TrainCommand();
		c.type = Type.MOVE;
		c.train = config.name();
		c.directionAndSpeed = config.speed();
		command(c);
	}
	
	private void stop(){
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
		System.out.println("Train "+config.name()+" reached assignment!");
		stop();
		
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
		currentRoute = null;
	}
	
	private void assignmentAborted(){
		stop();
		
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
		currentRoute = null;
	}

}
