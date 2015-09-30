package osgi.enroute.trains.track.manager.example.provider;

import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.track.util.Tracks.LocatorHandler;

class Locator extends LocatorHandler<Object> {

	private ExampleTrackManagerImpl owner;

	public Locator(ExampleTrackManagerImpl owner, Segment segment) {
		super(segment);
		this.owner = owner;
	}

	public void locatedAt(String rfid) {
		lastSeenId = rfid;
		Observation observation = new Observation();
		observation.type = Observation.Type.LOCATED;
		observation.segment = segment.id;
		observation.train = owner.getNameForRfid(rfid);
		owner.observation(observation);
	}

	
}
