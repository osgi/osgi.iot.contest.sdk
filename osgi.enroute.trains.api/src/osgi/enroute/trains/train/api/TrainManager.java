package osgi.enroute.trains.train.api;

/**
 * Train management
 * 
 */
public interface TrainManager {

	/**
	 * Give a train an assignment. The assignment is to move itself to the given
	 * segment.
	 * 
	 * @param toSegment
	 *            the id of the requested segment
	 */
	void assign(String toSegment);

}
