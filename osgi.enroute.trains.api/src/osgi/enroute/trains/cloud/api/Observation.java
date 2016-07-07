package osgi.enroute.trains.cloud.api;

import org.osgi.dto.DTO;

/**
 * Event class for sending out information about what's happening
 */
public class Observation extends DTO {
	public final static String TOPIC = "osgi/trains/observation";

	public enum Type {
		CHANGE, //
		/**
		 * Detected an RFID
		 */
		LOCATED, 
		/**
		 * Assignment changed
		 */
		ASSIGNMENT, 
		/**
		 * Assignment reached
		 */
		ASSIGNMENT_REACHED,
		/**
		 * Signal changed color
		 */
		SIGNAL,
		/**
		 * Switched changed alternate state
		 */
		SWITCH,
		/**
		 * A segment is blocked 
		 */
		BLOCKED,
		
		/**
		 * Following events are defined for the emulator only. They are not sent in the real world.
		 */
		
		/**
		 * Speed change in the emulator's train
		 */
		EMULATOR_TRAIN_SPEED,
		
		/**
		 * A train in the emulator went to another segment
		 */
		EMULATOR_TRAIN_MOVES,
		
		/**
		 * The train ran into a blocked section
		 */
		EMULATOR_TRAIN_CRASHED,
		
		/**
		 * The train entered a switch from the wrong side
		 */
		EMULATOR_TRAIN_WRONG_SWITCH,
		
		/**
		 * General purpose timeout for when no events are received
		 */
		TIMEOUT;
	}

	public Type type;
	public String segment;
	public String train;
	public Color signal;
	public String assignment;
	public long time;
	public long id;
	public String message;
	public boolean alternate;
	public boolean blocked;
	public double speed;
}
