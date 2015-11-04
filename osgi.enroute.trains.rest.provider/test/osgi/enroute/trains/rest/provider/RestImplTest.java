package osgi.enroute.trains.rest.provider;

import java.net.URI;

import junit.framework.TestCase;
import osgi.enroute.trains.rest.client.TrackClient;

/*
 * 
 * 
 * 
 */

public class RestImplTest extends TestCase {

	public void testX() throws Exception {
		TrackClient tc = new TrackClient(new URI("http://localhost:8080/rest/"));
		tc.blocked("A06", "WTF", true);
	}

	public void testY() throws Exception {
		TrackClient tc = new TrackClient(new URI("http://localhost:8080/rest/"));
		while(true) 
			System.out.println(tc.getRecentObservations(0));
	}
}
