package osgi.enroute.trains.track.api;

import java.util.List;
import java.util.Map;

import osgi.enroute.trains.segment.api.Color;

/**
 * General track info
 * 
 */
public interface TrackManager {

	/**
	 * Get segment information by id
	 * @param segment
	 * @return
	 */
	Segment getSegment(String segment);
	
	/**
	 * Get the general configuration
	 * 
	 * @return a map where the key is the segment name and the segment contains
	 *         the segment info.
	 */
	Map<String, Segment> getSegments();

	/**
	 * Get the list of trains
	 * 
	 * @return
	 */
	List<String> getTrains();

	/**
	 * Get the the current set of signals and their state
	 * 
	 * @return
	 */
	Map<String, Color> getSignals();

	/**
	 * Get the current set of switches and their state.
	 * 
	 * @return
	 */
	Map<String, Boolean> getSwitches();
	
	/**
	 * The given train requests access to the toSegment, it assumes it is on the
	 * fromSegment. You can only request access for one segment.
	 * 
	 * @param train
	 *            the train id
	 * @param fromSegment
	 *            the id of the from segment
	 * @param toSegment
	 *            the id of the requested segment
	 * @return true if granted,otherwise false. This function can wait up to 1
	 *         minute.
	 */
	boolean requestAccessTo(String train, String fromSegment, String toSegment);
	
	/**
	 * Give a route of segments from a segment to a segment.
	 * 
	 * @param fromSegment 
	 * 				the segment to start from
	 * @param toSegment 
	 * 				the segment to reach
	 * @param train
	 * 				the train for which to calculate the route
	 * @return a list of segments to follow, or null if no route exists.
	 */
	List<Segment> planRoute(String fromSegment, String toSegment, String train);
}
