package osgi.enroute.trains.passenger.api;

import java.util.List;

public interface PersonDatabase {

	Person register(String firstName, String lastName, String company, String email, String phone);
	
	List<Person> getPersons();
	
	Person getPerson(String id);
	
}
