package osgi.enroute.trains.track.util;

import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;

public class SegmentFactoryAdapter<T> implements SegmentFactory<T> {

	@Override
	public SegmentHandler<T> create(Segment segment) throws Exception {
		SegmentHandler<T> handler;
		switch (segment.type) {
		case BLOCK:
			handler = block(segment);
			break;

		case CURVED:
			handler = curve(segment);
			break;
		case LOCATOR:
			handler = locator(segment);
			break;
		case SIGNAL:
			handler = signal(segment);
			break;
		case STRAIGHT:
			handler = straight(segment);
			break;
		case SWITCH:
			handler = swtch(segment);
			break;
		default:
			throw new IllegalArgumentException("Missing case " + segment.type);
		}
		handler.segment = segment;
		return handler;
	}

	public SegmentHandler<T> block(Segment segment) {
		return new Tracks.BlockHandler<T>(segment);
	}

	public SegmentHandler<T> curve(Segment segment) {
		return new Tracks.CurvedHandler<T>(segment);
	}

	public SegmentHandler<T> straight(Segment segment) {
		return new Tracks.StraightHandler<T>(segment);
	}

	public SegmentHandler<T> signal(Segment segment) {
		return new Tracks.SignalHandler<T>(segment);
	}

	public SegmentHandler<T> locator(Segment segment) {
		return new Tracks.LocatorHandler<T>(segment);
	}

	public SegmentHandler<T> swtch(Segment segment) {
		return new Tracks.SwitchHandler<T>(segment);
	}

}
