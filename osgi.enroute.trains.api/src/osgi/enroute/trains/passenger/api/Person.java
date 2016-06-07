package osgi.enroute.trains.passenger.api;

import org.osgi.dto.DTO;

public class Person extends DTO {

	public String id;
	
	public String firstName;
	public String lastName;
	
	// for "networking" applications?
	public String company;
	public String email;
	public String phone;
	
	public PassengerStatistics statistics;
	
}
