package osgi.enroute.trains.train.api;

/**
 * Train management
 * 
 */
public interface TrainsManager {

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

}
