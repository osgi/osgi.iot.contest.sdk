package osgi.enroute.trains.track.manager.example.provider;

import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.track.util.SegmentFactoryAdapter;

class TrackManagerFactory extends SegmentFactoryAdapter<Object>{
	final ExampleTrackManagerImpl	owner;
	
	TrackManagerFactory(ExampleTrackManagerImpl owner) {
		this.owner = owner;
	}
	
	@Override
	public Signal signal(Segment segment) {
		return new Signal(owner,segment);
	}

	@Override
	public Locator locator(Segment segment) {
		return new Locator(owner,segment);
	}

	@Override
	public Switch swtch(Segment segment) {
		return new Switch(owner,segment);
	}
}
