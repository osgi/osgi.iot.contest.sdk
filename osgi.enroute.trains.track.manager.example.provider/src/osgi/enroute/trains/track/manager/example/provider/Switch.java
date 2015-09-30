package osgi.enroute.trains.track.manager.example.provider;

import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.track.util.Tracks.SwitchHandler;

class Switch extends SwitchHandler<Object> {

	private ExampleTrackManagerImpl owner;

	public Switch(ExampleTrackManagerImpl owner, Segment segment) {
		super(segment);
		this.owner = owner;
	}

	public void alternative(boolean toAlternate) {
		this.toAlternate = toAlternate;

		Observation o = new Observation();
		o.type = Observation.Type.SWITCH;
		o.segment = segment.id;
		o.alternate = toAlternate;
		owner.observation(o);
	}

}
