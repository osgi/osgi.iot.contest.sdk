package osgi.enroute.trains.segment.api;

/**
 * A controller controls a signal or a switch.
 */
public interface SegmentController {

	/**
	 * Service property for identifying this controller, by segment name
	 */
	String CONTROLLER_SEGMENT = "controller.segment";

	/**
	 * Get the host location of the segment controller.
	 */
	default String getLocation() {
		return "unknown";
	}

}
