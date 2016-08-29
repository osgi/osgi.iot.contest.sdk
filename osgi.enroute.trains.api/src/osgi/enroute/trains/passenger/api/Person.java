package osgi.enroute.trains.passenger.api;

import org.osgi.dto.DTO;

public class Person extends DTO {

	public String id;
	
	public String firstName;
	public String lastName;
	
	public String email;

	// urls to picture/website
	public String picture;
	public String website;
	
	// for "networking" applications?
	public String company;
	public String phone;
	
}
