package osgi.enroute.trains.track.manager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.converter.Converter;

import osgi.enroute.dto.api.TypeReference;
import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.scheduler.api.Scheduler;
import osgi.enroute.trains.segment.api.Color;
import osgi.enroute.trains.segment.api.SegmentCommand;
import osgi.enroute.trains.track.api.Segment;
import osgi.enroute.trains.track.api.TrackConfiguration;
import osgi.enroute.trains.track.api.TrackManager;
import osgi.enroute.trains.track.api.TrackObservation;
import osgi.enroute.trains.track.api.TrackObservation.Type;
import osgi.enroute.trains.track.manager.Tracks.SegmentHandler;
import osgi.enroute.trains.track.manager.Tracks.SignalHandler;
import osgi.enroute.trains.track.manager.Tracks.SwitchHandler;
import osgi.enroute.trains.train.api.TrainCommand;

/**
 * 
 */
@Component(immediate = true, name = "osgi.enroute.trains.track.manager", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TrackManagerImpl implements TrackManager {

	private static final int TIMEOUT = 60000;

	private Tracks tracks;

	// located observation train -> segment
	private Map<String, String> lastLocation = new HashMap<>();

	// track access track->train
	private Map<String, String> access = new HashMap<>();

	// last track access train->track
	private Map<String, String> lastAccess = new HashMap<>();

	@Reference
	private Scheduler scheduler;

	@Reference
	private Converter converter;

	@Reference
	private MQTTService mqtt;

	@Activate
	public void activate(TrackConfiguration config) throws Exception {
		tracks = new Tracks(config.segments());

		tracks.getSegments().entrySet().stream().filter(e -> e.getValue().type == Segment.Type.SIGNAL).forEach(e -> setSignal(e.getKey(), Color.RED));
		
		mqtt.subscribe(TrackObservation.TOPIC).forEach(msg -> {
			TrackObservation o = converter.convert(msg.payload().array()).to(TrackObservation.class);
			if (o.type == Type.LOCATED) {
				// update train observation
				lastLocation.put(o.train, o.segment);

				Segment s = tracks.getSegment(o.segment);
				if(s == null){
					System.err.println("Train "+o.train+" located on invalid segment "+o.segment);
					return;
				}
				
				String track = s.track;
				synchronized (access) {
					String trainForTrack = access.get(track);
					if (trainForTrack == null) {
						System.out.println("Initial access for train "+o.train+" to track "+track);
						access.put(track, o.train);
						lastAccess.put(o.train, track);
					} else if (!trainForTrack.equals(o.train)) {
						System.err.println("Train "+o.train+" doesn't have access to track "+track);
						stopTrain(o.train);
					}
				}

				releaseOtherTracks(o.train, track);
			} else {
				// update track state
				tracks.event(o);
			}
		});

	}

	@Override
	public Segment getSegment(String segment) {
		return tracks.getSegment(segment);
	}
	
	@Override
	public Map<String, Segment> getSegments() {
		return tracks.getSegments();
	}

	@Override
	public List<String> getTrains() {
		return new ArrayList<String>(lastLocation.keySet());
	}

	@Override
	public Map<String, Color> getSignals() {
		return tracks.filter(new TypeReference<SignalHandler>() {
		}).collect(Collectors.toMap(sh -> sh.segment.id, sh -> sh.color));
	}

	@Override
	public Map<String, Boolean> getSwitches() {
		return tracks.filter(new TypeReference<SwitchHandler>() {
		}).collect(Collectors.toMap(sh -> sh.segment.id, sh -> sh.toAlternate));
	}

	@Override
	public boolean requestAccessTo(String train, String fromTrack, String toTrack) {
		System.out.println("Access request from "+train+" "+fromTrack+"->"+toTrack);
		long start = System.currentTimeMillis();
		boolean granted = false;

		while (!granted && System.currentTimeMillis() - start < TIMEOUT) {
			synchronized (access) {
				if(tracks.isBlocked(toTrack)){
					// blocked, decline access
					granted = false;
				} else {
					String currentAllocation = access.get(toTrack);
					if(train.equals(currentAllocation)){
						// train already has access
						granted = true;
					} else if(currentAllocation != null){
						// other train currently has access, we have to wait
						granted = false;
					} else {
						// no other train currently has access, assign track to this train and set signals/switches 
						// assign track to this train
						access.put(toTrack, train);
						lastAccess.put(train, toTrack);

						// check if switch is ok
						Optional<SwitchHandler> optSwitch = tracks.getSwitch(fromTrack, toTrack);
						if (!optSwitch.isPresent()) {
							// in case no switch, just set signal - if present -
							// green
							System.out.println("No switch between " + fromTrack + " and " + toTrack);
							greenSignal(tracks.getSignal(fromTrack));
							granted = true;
						} else {
							// set switch in correct position
							SwitchHandler switchHandler = optSwitch.get();
							if (shouldSwitch(switchHandler, fromTrack, toTrack)) {
								doSwitch(switchHandler.segment.id, !switchHandler.toAlternate);
								sleep(3000);
								// TODO should we wait for switched observation here?!
							}							
							
							// set green signal
							if (!greenSignal(tracks.getSignal(fromTrack))) {
								sleep(1000);
								// TODO should we wait for green signal observation here?!
							}

							// now grant the access
							granted = true;
						}
					}
				}
				
				// if not granted, wait until timeout
				if (!granted) {
					try {
						long wait = TIMEOUT - System.currentTimeMillis() + start;
						if (wait > 0) {
							access.wait(wait);
						}
					} catch (InterruptedException e) {
					}
				}
			}
		}

		return granted;
	}
	
	public List<Segment> planRoute(String fromSegment, String toSegment) {
		// plan the route
		SegmentHandler src = tracks.getHandler(fromSegment);
		SegmentHandler dest = tracks.getHandler(toSegment);
		LinkedList<SegmentHandler> route = src.findForward(dest);

		return route.stream()
				.filter(sh -> !(sh instanceof SwitchHandler)) // exclude switch/signals
				.filter(sh -> !(sh instanceof SignalHandler)) // from routes
				.map(sh -> sh.segment).collect(Collectors.toList());
	}

	private void releaseOtherTracks(String train, String track) {
		synchronized (access) {
			String lastTrack = lastAccess.get(train);

			List<String> tracks = access.entrySet().stream().filter(e -> e.getValue().equals(train))
					.filter(e -> !e.getKey().equals(track)).filter(e -> !e.getKey().equals(lastTrack))
					.map(e -> e.getKey()).collect(Collectors.toList());

			if (!tracks.isEmpty()) {
				System.out.println("Releasing: tracks "+tracks+" from train "+train);
				tracks.forEach(t -> access.remove(t));
				access.notifyAll();
			}
		}
	}

	// set the signal to green for 10 seconds
	private boolean greenSignal(Optional<SignalHandler> optSignal) {
		boolean alreadyGreen = false;

		if (optSignal.isPresent()) {
			SignalHandler signal = optSignal.get();
			alreadyGreen = (signal.color.equals(Color.GREEN));
			setSignal(signal.segment.id, Color.GREEN);
			scheduler.after(() -> setSignal(signal.segment.id, Color.YELLOW), 10000);
			scheduler.after(() -> setSignal(signal.segment.id, Color.RED), 15000);
		}

		return alreadyGreen;
	}

	// checks whether the switch is in the right state to go from fromTrack to
	// toTrack
	private boolean shouldSwitch(SwitchHandler sh, String fromTrack, String toTrack) {
		boolean switchOK = true;

		if (sh.isMerge()) {
			// check if previous is fromTrack
			if (sh.prev.getTrack().equals(fromTrack)) {
				// if so, then alternate should be false
				if (sh.toAlternate) {
					switchOK = false;
				}
				// else alternate should be true
			} else if (!sh.toAlternate) {
				switchOK = false;
			}
		} else {
			// check if next is toTrack
			if (sh.next.getTrack().equals(toTrack)) {
				// if so, then alternate should be false
				if (sh.toAlternate) {
					switchOK = false;
				}
				// else alternate should be true
			} else if (!sh.toAlternate) {
				switchOK = false;
			}
		}

		return !switchOK;
	}
	
	private void setSignal(String segment, Color color) {
		SegmentCommand c = new SegmentCommand();
		c.type = SegmentCommand.Type.SIGNAL;
		c.segment = segment;
		c.signal = color;
		command(c);
	}

	private void doSwitch(String segment, boolean alt) {
		SegmentCommand c = new SegmentCommand();
		c.type = SegmentCommand.Type.SWITCH;
		c.segment = segment;
		c.alternate = alt;
		command(c);
	}
	
	private void command(SegmentCommand c){
		try {
			mqtt.publish(SegmentCommand.TOPIC,  ByteBuffer.wrap( converter.convert(c).to(byte[].class)));
		} catch(Exception e){
			System.out.println("Failed publishing command");
		}
	}
	
	private void stopTrain(String train) {
		TrainCommand c = new TrainCommand();
		c.type = TrainCommand.Type.MOVE;
		c.train = train;
		c.directionAndSpeed = 0;
		
		try {
			mqtt.publish(TrainCommand.TOPIC,  ByteBuffer.wrap( converter.convert(c).to(byte[].class)));
		} catch(Exception e){
			System.out.println("Failed publishing command");
		}
	}
	
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
}
