package osgi.enroute.trains.segment.api;

import osgi.enroute.trains.segment.api.SegmentController;

/**
 * This controller controls a SWITCH Segment
 */
public interface SwitchSegmentController extends SegmentController {

	/**
	 * Set the switch to normal or the alternative
	 * @param alternative
	 */
	void swtch(boolean alternative);
	
	boolean getSwitch();
}
