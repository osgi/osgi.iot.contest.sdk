package osgi.enroute.trains.track.util;

import osgi.enroute.trains.track.api.Segment;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;

public interface SegmentFactory<T> {
	SegmentHandler<T> create(Segment segment) throws Exception;
}
