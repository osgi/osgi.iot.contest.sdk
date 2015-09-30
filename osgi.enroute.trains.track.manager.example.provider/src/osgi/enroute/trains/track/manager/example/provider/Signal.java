package osgi.enroute.trains.track.manager.example.provider;

import osgi.enroute.trains.cloud.api.Color;
import osgi.enroute.trains.cloud.api.Observation;
import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.track.util.Tracks.SignalHandler;

class Signal extends SignalHandler<Object> {

	private ExampleTrackManagerImpl owner;

	public Signal(ExampleTrackManagerImpl owner, Segment segment) {
		super(segment);
		this.owner = owner;
	}

	public void setColor(Color color) {
		this.color = color;
		
		Observation o = new Observation();
		o.type = Observation.Type.SIGNAL;
		o.segment = segment.id;
		o.signal = color;
		owner.observation(o);
		
	}

}
