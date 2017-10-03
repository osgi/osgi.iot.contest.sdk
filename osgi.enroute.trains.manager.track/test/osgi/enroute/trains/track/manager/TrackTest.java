package osgi.enroute.trains.track.manager;

import java.util.Arrays;

import aQute.lib.io.IO;
import junit.framework.TestCase;
import osgi.enroute.trains.track.api.TrackObservation;
import osgi.enroute.trains.track.api.TrackObservation.Type;
import osgi.enroute.trains.track.manager.Tracks.SegmentHandler;

public class TrackTest extends TestCase {

	public void testRoute() throws Exception {
		String train = "White";
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-main.txt"));
		Tracks tracks = new Tracks(plan);
		
		SegmentHandler src = tracks.getHandler("A01");
		SegmentHandler dest = tracks.getHandler("D04");
		String route =  Arrays.toString(src.findForward(dest, train).toArray());
		assertEquals("[A01, A02, A03, A04, A05, A06, A07, A08, A09, A10, A11, A12, A13, S01, A14, X01, B00, B01, B02, B03, B04, B05, B06, B07, S02, B08, X02, D00, D01, D02, D03, D04]", route);
	}
	

	public void testBlocked() throws Exception {
		String train = "White";
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-main.txt"));
		Tracks tracks = new Tracks(plan);

		TrackObservation o = new TrackObservation();
		o.type = Type.BLOCKED;
		o.segment = "B02";
		o.blocked = true;
		tracks.event(o);
		
		assertEquals(true, tracks.getHandler("B02").blocked);
		
		SegmentHandler src = tracks.getHandler("A01");
		SegmentHandler dest = tracks.getHandler("D04");
		String route =  Arrays.toString(src.findForward(dest, train).toArray());
		assertEquals("[A01, A02, A03, A04, A05, A06, A07, A08, A09, A10, A11, A12, A13, S01, A14, X01, C00, C01, C02, C03, C04, C05, C06, C07, C08, C09, S03, C10, X02, D00, D01, D02, D03, D04]", route);
	}

	public void testOccupied() throws Exception {
		String train = "White";
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-main.txt"));
		Tracks tracks = new Tracks(plan);

		TrackObservation o = new TrackObservation();
		o.type = Type.LOCATED;
		o.segment = "B02";
		o.train = "Red";
		tracks.event(o);

		assertEquals("Red", tracks.getHandler("B02").occupiedBy);
		
		TrackObservation o2 = new TrackObservation();
		o2.type = Type.LOCATED;
		o2.segment = "B03";
		o2.train = "Red";
		tracks.event(o2);
		
		assertEquals(null, tracks.getHandler("B02").occupiedBy);
		assertEquals("Red", tracks.getHandler("B03").occupiedBy);

		
		SegmentHandler src = tracks.getHandler("A01");
		SegmentHandler dest = tracks.getHandler("D04");
		String route =  Arrays.toString(src.findForward(dest, train).toArray());
		assertEquals("[A01, A02, A03, A04, A05, A06, A07, A08, A09, A10, A11, A12, A13, S01, A14, X01, C00, C01, C02, C03, C04, C05, C06, C07, C08, C09, S03, C10, X02, D00, D01, D02, D03, D04]", route);
	}
	
	public void testNoUnOccupiedRoute() throws Exception {
		// in case no unoccupied route, just give any route (we should wait until other train moves then)
		String train = "White";
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-main.txt"));
		Tracks tracks = new Tracks(plan);

		TrackObservation o = new TrackObservation();
		o.type = Type.LOCATED;
		o.segment = "D01";
		o.train = "Red";
		tracks.event(o);

		SegmentHandler src = tracks.getHandler("A01");
		SegmentHandler dest = tracks.getHandler("D04");
		String route =  Arrays.toString(src.findForward(dest, train).toArray());
		assertEquals("[A01, A02, A03, A04, A05, A06, A07, A08, A09, A10, A11, A12, A13, S01, A14, X01, B00, B01, B02, B03, B04, B05, B06, B07, S02, B08, X02, D00, D01, D02, D03, D04]", route);
	}
	
	
	public void testSingleItemRoute() throws Exception {
		// in case no unoccupied route, just give any route (we should wait until other train moves then)
		String train = "White";
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-main.txt"));
		Tracks tracks = new Tracks(plan);

		SegmentHandler src = tracks.getHandler("A01");
		SegmentHandler dest = tracks.getHandler("A01");
		String route =  Arrays.toString(src.findForward(dest, train).toArray());
		assertEquals("[A01]", route);
	}
}
