package osgi.enroute.trains.controller.api;

import org.osgi.util.promise.Promise;

public interface TrainLocator {

	/**
	 * get train location. Promise is resolved when train passes next location.
	 * @return colon separated string "trainId:segmentName"
	 */
	Promise<String> nextLocation();
}
