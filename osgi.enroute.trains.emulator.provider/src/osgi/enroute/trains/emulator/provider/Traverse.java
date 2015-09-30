package osgi.enroute.trains.emulator.provider;

import java.io.Closeable;

import org.osgi.framework.BundleContext;

import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;

public interface Traverse extends Closeable {
	Traverse next(String rfid);

	Traverse prev(String rfid);

	void register(BundleContext context);

	int l();

	default boolean isBlocked() {
		return false;
	}

	default Segment getSegment() {
		SegmentHandler<?> sh = (SegmentHandler<?>)this;
		return sh.segment;
	}
}
