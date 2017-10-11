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
	
	
	/**
	 * Abort the current assignment.
	 */
	void abort();

	
	/**
	 * Get the (max) speed of the train
	 */
	int speed();
	
	/**
	 * Set a new (max) speed of the train
	 * @param speed
	 */
	void speed(int speed);
}
