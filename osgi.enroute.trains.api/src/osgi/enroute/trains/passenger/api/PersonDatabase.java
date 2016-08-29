package osgi.enroute.trains.passenger.api;

import java.util.List;

public interface PersonDatabase {

	Person register(String email, String firstName, String lastName);
	
	Person register(String email, String firstName, String lastName, String company, String phone, String picture, String website);
	
	List<Person> getPersons();
	
	Person getPerson(String id);
	
}
