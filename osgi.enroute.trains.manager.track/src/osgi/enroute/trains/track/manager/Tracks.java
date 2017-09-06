package osgi.enroute.trains.track.manager;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import osgi.enroute.dto.api.TypeReference;
import osgi.enroute.trains.segment.api.Color;
import osgi.enroute.trains.track.api.Observation;
import osgi.enroute.trains.track.api.Segment;
import osgi.enroute.trains.track.api.TrackConfiguration;

/**
 * A utility to parse configuration data and turn it into managed segments. This
 * code makes it easier to use the track by creating a fully object linked model
 * of the track.
 */
public class Tracks {

	final Map<String, SegmentHandler> handlers = new HashMap<>();
	final Map<String, Segment> segments = new HashMap<>();

	/**
	 * Base class. manages day to day business
	 */
	public static class SegmentHandler {
		public Segment segment;
		public SegmentHandler next;
		public SegmentHandler prev;

		public SegmentHandler(Segment segment) {
			this.segment = segment;
		}

		boolean isPrevAlt(SegmentHandler prev) {
			return false;
		}

		@Override
		public String toString() {
			return segment.id;
		}
		
		public String getTrack(){
			return segment.track;
		}

		public boolean isMerge() {
			return false;
		}

		public boolean isSwitch() {
			return false;
		}

		public boolean isPlain() {
			return true;
		}
		
		public boolean isSignal(){
			return false;
		}

		public LinkedList<SegmentHandler> findForward(SegmentHandler destination) {
			LinkedList<SegmentHandler> route = new LinkedList<>();
			if (find(route, destination, true))
				return new LinkedList<SegmentHandler>(route);
			else
				return null;
		}

		public LinkedList<SegmentHandler> findBackward(SegmentHandler destination) {
			LinkedList<SegmentHandler> route = new LinkedList<>();
			if (find(route, destination, false))
				return new LinkedList<SegmentHandler>(route);
			else
				return null;
		}

		boolean find(List<SegmentHandler> route, SegmentHandler destination, boolean forward) {
			if (route.contains(this)){
				return false;
			}

			route.add(this);

			if (this == destination){
				return true;
			}

			Collection<SegmentHandler> choices = move(forward);

			if (choices.size() == 1) {
				return choices.iterator().next().find(route, destination, forward);
			}

			int marker = route.size();
			for (SegmentHandler choice : choices) {
				if (choice.find(route, destination, forward))
					return true;
					

				while(route.size() > marker){
					route.remove(marker);
				}
			}

			return false;

		}

		public Collection<SegmentHandler> move(boolean forward) {
			return Collections.singleton(forward ? next : prev);
		}

		void fixup() {
			segment.to = toIds(move(true));
			segment.from = toIds(move(false));
			segment.length = length();
		}

		protected int length() {
			return 0;
		}

		/**
		 * Events should be handled in subclasses if they have relevant events
		 */
		public boolean event(Observation e) {
			return false;
		}
	}

	public static  String[] toIds(Collection<SegmentHandler> coll) {
		return coll.stream().map(h -> h.segment.id).toArray(String[]::new);
	}

	public static class StraightHandler extends SegmentHandler {
		public StraightHandler(Segment segment) {
			super(segment);
		}

		protected int length() {
			return 16;
		}
	}

	public static class CurvedHandler extends SegmentHandler {
		public CurvedHandler(Segment segment) {
			super(segment);
		}

		protected int length() {
			return 16;
		}
	}

	public static class SwitchHandler extends SegmentHandler {
		public SegmentHandler altNext;
		public SegmentHandler altPrev;
		public boolean toAlternate;

		public SwitchHandler(Segment segment) {
			super(segment);
		}

		@Override
		public Collection<SegmentHandler> move(boolean forward) {
			if (forward) {
				if (altNext == null)
					return super.move(true);
				else
					return Arrays.asList(next, altNext);
			} else {
				if (altPrev == null)
					return super.move(false);
				else
					return Arrays.asList(prev, altPrev);
			}
		}

		@Override
		public boolean isMerge() {
			return altPrev != null;
		}

		@Override
		public boolean isSwitch() {
			return altNext != null;
		}

		@Override
		public boolean isPlain() {
			return false;
		}

		@Override
		boolean isPrevAlt(SegmentHandler prev) {
			return prev == altPrev;
		}

		protected int length() {
			return toAlternate ? 35 : 32;
		}

		public void setToAlternate(boolean alternate) {
			this.toAlternate = alternate;
		}

		public boolean event(Observation e) {
			switch (e.type) {
			case SWITCH:
				setToAlternate(e.alternate);
				return true;

			default:
				return super.event(e);
			}
		}
	}

	public static class SignalHandler extends SegmentHandler {
		public Color color = Color.YELLOW;

		public SignalHandler(Segment segment) {
			super(segment);
		}
		
		public boolean event(Observation e) {
			switch (e.type) {
			case SIGNAL:
				setSignal(e.signal);
				return true;

			default:
				return super.event(e);
			}
		}

		public void setSignal(Color signal) {
			this.color = signal;
		}
		
		public boolean isSignal(){
			return true;
		}
	}

	public Tracks(String[] segments) throws Exception {
		this(parse(segments));
	}

	public Tracks(Map<String, Object> config) throws Exception {
		this(parse((String[]) config.get("segments")));
	}

	public Tracks(String plan) throws Exception {
		this(parse(plan));
	}

	public static List<Segment> parse(String plan) {
		String lines[] = plan.split("\r?\n");
		return parse(lines);
	}

	public static List<Segment> parse(String[] lines) {
		List<Segment> segments = new ArrayList<>();
		int n = 0;
		for (String line : lines) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#"))
				continue;

			String parts[] = line.split("\\s*:\\s*");

			Segment segment = parseLine(n++, parts);
			segments.add(segment);
		}
		return segments;
	}

	private static Segment parseLine(int n, String[] parts) {
		assert parts.length == 4;

		Segment segment = new Segment();
		segment.id = parts[0];
		segment.type = Enum.valueOf(Segment.Type.class, parts[1]);
		segment.tag = Integer.parseInt(parts[2]);
		segment.to = parts[3].split("\\s*,\\s*");

		segment.sequence = n;

		segment.track = segment.id.substring(0, 1);
		return segment;
	}

	public Tracks(Collection<? extends Segment> segments) throws Exception {
		index(segments);
		build(segments);
		link();
		fixup();
		validate();
	}

	private void index(Collection<? extends Segment> segments) {
		segments.forEach(segment -> this.segments.put(segment.id, segment));
	}

	private void build(Collection<? extends Segment> segments) throws Exception {
		for (Segment segment : segments) {
			SegmentHandler handler = null;
			switch (segment.type) {
			case CURVED:
				handler = new CurvedHandler(segment);
				break;
			case STRAIGHT:
				handler = new StraightHandler(segment);
				break;
			case SIGNAL:
				handler = new SignalHandler(segment);
				break;
			case SWITCH:
				handler = new SwitchHandler(segment);
				break;
			default:
				throw new IllegalArgumentException("Missing case " + segment.type);
			}
			handler.segment = segment;
			handlers.put(segment.id, handler);
		}
	}

	private void link() throws Exception {
		for (SegmentHandler segmentHandler : handlers.values()) {

			Segment segment = segmentHandler.segment;

			String[] to = segment.to;

			if (to == null || to.length == 0)
				throw new IllegalArgumentException("A non BLOCK segment has no to: " + segment.id);

			segmentHandler.next = link(segmentHandler, to[0]);

			if (to.length == 2) {

				if (!(segmentHandler instanceof Tracks.SwitchHandler))
					throw new IllegalArgumentException("Multiple 'to' specified but not a Switch " + segment.id);

				SwitchHandler s = (SwitchHandler) segmentHandler;
				s.altNext = link(segmentHandler, to[1]);
			} else if (to.length > 2)
				throw new IllegalArgumentException("Too many destinations: " + segment.id);
		}
	}

	private SegmentHandler link(SegmentHandler current, String nextId) {
		
		boolean alternate = nextId.endsWith("!");
		if (alternate) {
			nextId = nextId.substring(0, nextId.length() - 1);
		}
		
		// additional check in case created from a bunch of Segments and no ! is available
		if(handlers.get(nextId).segment.from!=null){
			if(handlers.get(nextId).segment.from.length==2){
				if(handlers.get(nextId).segment.from[1].equals(current.segment.id)){
					alternate = true;
				}
			}
		}

		SegmentHandler next = handlers.get(nextId);
		if (next == null)
			throw new IllegalArgumentException(
					"Invalid reference to next segment, from: " + current.segment + " to " + nextId);

		if (alternate) {

			if (!(next instanceof Tracks.SwitchHandler))
				throw new IllegalArgumentException(
						"Invalid reference to switch alternate and next is not a switch, from: " + current.segment.id
								+ " to " + next.segment);

			SwitchHandler s = (SwitchHandler) next;
			s.altPrev = current;
		} else {
			next.prev = current;
		}
		return next;
	}

	private void fixup() {
		for (SegmentHandler segmentHandler : handlers.values()) {
			segmentHandler.fixup();
		}
	}

	private void validate() {

	}

	public Collection<? extends SegmentHandler> getHandlers() {
		return handlers.values();
	}

	public Hashtable<String, Object> toConfiguration(String name) {
		Hashtable<String, Object> configuration = new Hashtable<>();
		configuration.put("name", name);

		List<String> lines = toLines();

		configuration.put("segments", lines.toArray(new String[lines.size()]));
		return configuration;
	}

	public List<String> toLines() {
		List<String> lines = new ArrayList<>();

		for (SegmentHandler handler : handlers.values()) {
			String line = toLine(handler);
			lines.add(line);
		}
		return lines;
	}

	private String toLine(SegmentHandler handler) {
		Segment s = handler.segment;
		try (Formatter formatter = new Formatter();) {
			formatter.format("%-10s : %-10s : %-6s ", s.id, s.type, s.tag);

			String nextId = getNextId(handler.next);
			formatter.format(": %s", nextId);

			if (handler instanceof SwitchHandler) {
				SwitchHandler sw = (SwitchHandler) handler;
				if (sw.altNext != null)
					formatter.format(",%s", getNextId(sw.altNext));
			}
			
			if(handler.next instanceof SwitchHandler) {
				if(handler.next.isPrevAlt(handler)){
					formatter.format("!");
				}
			}
			return formatter.toString();
		}
	}

	public TrackConfiguration.DTO getConfigurationDTO(String name) {
		TrackConfiguration.DTO dto = new TrackConfiguration.DTO();
		dto.name = name;
		dto.segments = handlers.values().stream().map((h) -> toLine(h)).collect(Collectors.toList());
		// TODO trains?
		return dto;
	}

	private String getNextId(SegmentHandler handler) {
		String nextId = handler.segment.id;

		if (handler instanceof SwitchHandler) {
			SwitchHandler sw = (SwitchHandler) handler;
			if (sw.altPrev == handler)
				return nextId + "!";
		}
		return nextId;
	}

	public SegmentHandler getHandler(String id) {
		return handlers.get(id);
	}

	public Map<String, Segment> getSegments() {
		return Collections.unmodifiableMap(segments);
	}

	public <Y extends SegmentHandler> Stream<Y> filter(TypeReference<Y> tref) {
		ParameterizedType type = (ParameterizedType) tref.getType();
		Class<?> clazz = (Class<?>) type.getRawType();
		@SuppressWarnings("unchecked")
		Stream<Y> map = handlers.values().stream().filter(h -> clazz.isInstance(h)).map(h -> (Y) clazz.cast(h));
		return map;
	}

	public <Y extends SegmentHandler> Y getHandler(Class<Y> clazz, String id) {
		Y result = clazz.cast(getHandler(id));
		return result;
	}

	public void event(Observation e) {
		if (e.segment != null) {
			SegmentHandler sh = getHandler(e.segment);
			if (sh == null)
				throw new IllegalArgumentException("No such segment in this track : " + e.segment);

			sh.event(e);

		}
	}
}
