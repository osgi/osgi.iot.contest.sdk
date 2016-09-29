package osgi.enroute.trains.cloud.api;

/**
 * Defines the service interface for the Track Manager from the perspective of a
 * Command giver, e.g. a GUI
 */
public interface TrackForCommand extends TrackInfo {

	/**
	 * Give a train an assignment. The assignment is to move itself to the given
	 * segment.
	 * 
	 * @param train
	 *            the train id
	 * @param toSegment
	 *            the id of the requested segment
	 * @return true if granted,otherwise false. This function can wait up to 1
	 *         minute.
	 */
	void assign(String train, String toSegment);

	
	/**
	 * Notify a train of an emergency
	 * 
	 * @param train
	 * 			the train id
	 * @param reason
	 * 			cause of the emergency 
	 * @param emergency 
	 * 			whether the emergency is still applying
	 */
	void emergency(String train, String reason, boolean emergency);
	
	
	/**
	 * External indication that a segment is broken
	 * 
	 * @param Segment
	 *            The segment that is blocked/unblocked
	 * @param The
	 *            reason the segment is blocked
	 * @param blocked
	 *            true if blocked, false is free
	 */
	void blocked(String segment, String reason, boolean blocked);
	

	/**
	 * External indication that a segment is dark
	 * 
	 * @param Segment
	 *            The segment  that is dark/light
	 * @param blocked
	 *            true if dark, false is light
	 */
	void dark(String segment, boolean dark);
	
}
