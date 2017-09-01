package osgi.enroute.trains.track.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import junit.framework.TestCase;
import osgi.enroute.trains.track.api.Segment;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;
import osgi.enroute.trains.track.util.Tracks.SwitchHandler;

public class TrackTest extends TestCase {

	public void testSimple() throws Exception {
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-simple.txt"));
		Tracks<Object> track = new Tracks<Object>(plan, new SegmentFactoryAdapter<Object>());
		assertTrack(track, 20);
	}

	public void testMain() throws Exception {
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-main.txt"));
		Tracks<Object> track = new Tracks<Object>(plan, new SegmentFactoryAdapter<Object>());
		//assertTrack(track, 76);
		assertRoute(track.getHandler("A09_L"), track.getHandler("E06_L"));
	}
	
	public void testRoute() throws Exception {
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-main.txt"));
		Tracks<Object> track = new Tracks<Object>(plan, new SegmentFactoryAdapter<Object>());
		String route = Arrays.toString(track.getHandler("A09_L").findForward(track.getHandler("E06_L")).toArray());
		assertEquals("[A09_L, A10, A11, A12, A13, A14, A15, A16, A16_S, A16_L, X01, C00, C01, C02, C03, C04, C05, C05_S, C05_L, X02, E00, E01, E02, E03, E04, E05, E06, E06_L]",route);
	}

	private <T> void assertTrack(Tracks<T> track, int expectedSize) {

		for (SegmentHandler<T> a : track.getHandlers()) {
			for (SegmentHandler<T> b : track.getHandlers()) {
				assertRoute(a, b);
			}

			assertEquals("missing next in to for " + a + "->" + a.next, a.segment.to[0], a.next.segment.id);
			assertTrue("missing me in from for " + a + "->" + a.next, in(a.next.segment.from, a.segment.id));
			
			if ( a.isMerge()) {
				assertEquals( 2, a.segment.from.length);
				assertEquals( 1, a.segment.to.length);
				assertNull( ((SwitchHandler<T>)a).altNext);
				assertNotNull( ((SwitchHandler<T>)a).altPrev);
			}
			if ( a.isSwitch()) {
				assertEquals( 2, a.segment.to.length);
				assertEquals( 1, a.segment.from.length);
				assertNotNull( ((SwitchHandler<T>)a).altNext);
				assertNull( ((SwitchHandler<T>)a).altPrev);
			}
			if ( a.isPlain()) {
				assertEquals( 1, a.segment.to.length);
				assertEquals( 1, a.segment.from.length);
			}
		}

		Map<String, Segment> segments = track.getSegments();
		assertEquals(expectedSize, segments.size());

	}

	private boolean in(String[] array, String id) {
		for (String s : array) {
			if (s == id)
				return true;
			if (id.equals(s))
				return true;
		}
		return false;
	}

	private <T> void assertRoute(SegmentHandler<T> a, SegmentHandler<T> b) {
		List<SegmentHandler<T>> route = a.findForward(b);
		System.out.println(a + "->" + b + " : " + route);
		assertNotNull(a + " -> " + b + " are not forward connected", route);
		assertEquals("First must be -> " + a + " to " + b, a, route.get(0));
		assertEquals("Last must be ->" + b + " from " + a, b, route.get(route.size() - 1));
		route = a.findBackward(b);
		System.out.println(a + "->" + b + " : " + route);
		assertNotNull(a + " <- " + b + " are not backward connected", route);
	}

	public void testTrackConfiguration() throws Exception {
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-main.txt"));
		Tracks<Object> track = new Tracks<Object>(plan, new SegmentFactoryAdapter<Object>());
		Map<String, Object> configuration = track.toConfiguration("blabla");

		Tracks<Object> other = new Tracks<Object>(configuration, new SegmentFactoryAdapter<Object>());

		assertSameTrack(track, other);

	}

	private <T> void assertSameTrack(Tracks<T> a, Tracks<T> b) {
		Set<SegmentHandler<T>> ahs = new HashSet<>(a.getHandlers());
		Set<SegmentHandler<T>> bhs = new HashSet<>(b.getHandlers());
		assertEquals("Must have the same number of elements ", ahs.size(), bhs.size());

		for (SegmentHandler<T> ha : ahs) {
			SegmentHandler<T> hb = b.getHandler(ha.segment.id);
			assertNotNull("from " + ha, hb);
			assertEquals(ha + "->" + hb, ha.segment.id, hb.segment.id);
			assertNotNull(ha + ".next->" + hb, ha.next);
			assertNotNull(hb + ".next->" + hb, hb.next);
			assertNotNull(ha + ".next.segment->" + hb, ha.next.segment);
			assertNotNull(hb + ".next.segment->" + hb, hb.next.segment);
			assertNotNull(ha + ".prev->" + hb, ha.prev);
			assertNotNull(hb + ".prev->" + hb, hb.prev);
			assertNotNull(ha + ".prev.segment->" + hb, ha.prev.segment);
			assertNotNull(hb + ".prev.segment->" + hb, hb.prev.segment);
			assertEquals(ha + "->" + hb, ha.next.segment.id, hb.next.segment.id);
			assertEquals(ha + "->" + hb, ha.prev.segment.id, hb.prev.segment.id);
		}
	}

	public void testGenerateJSON() throws Exception {
		String plan = IO.collect(TrackTest.class.getResourceAsStream("track-main.txt"));
		Tracks<Object> track = new Tracks<Object>(plan, new SegmentFactoryAdapter<Object>());

		String json = new JSONCodec().enc().indent("  ").put(track.getConfigurationDTO("main")).toString();
		assertNotNull(json);
		System.out.println(json);
	}
}
