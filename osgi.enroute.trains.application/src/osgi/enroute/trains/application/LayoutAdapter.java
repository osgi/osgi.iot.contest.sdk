package osgi.enroute.trains.application;

import static java.lang.Math.max;

import osgi.enroute.trains.application.SegmentPosition.Symbol;
import osgi.enroute.trains.cloud.api.Segment;
import osgi.enroute.trains.track.util.SegmentFactoryAdapter;
import osgi.enroute.trains.track.util.Tracks;
import osgi.enroute.trains.track.util.Tracks.SegmentHandler;

public class LayoutAdapter extends SegmentFactoryAdapter<LayoutAdapter.Layout> {

	interface Layout {
		void layout(int x, int y, Layout from);

		SegmentPosition getPosition();
		
		default void adjustWidth() {
			
		}
		
		default void title() {
			getPosition().title = getPosition().segment.track;
		}
	}

	class Switch extends Tracks.SwitchHandler<Layout>implements Layout {
		final SegmentPosition segmentPosition;

		public Switch(Segment segment) {
			super(segment);
			this.segmentPosition = new SegmentPosition();
			this.segmentPosition.segment = segment;
		}

		@Override
		public void layout(int x, int y, Layout from) {
			if (from == prev)
				this.segmentPosition.y = y;
			
			if (x > this.segmentPosition.x) {
				this.segmentPosition.width = 2;
				this.segmentPosition.x = max(this.segmentPosition.x, x);

				if (from == prev)
					this.segmentPosition.y = y;
				
				this.next.get().title();
				
				if (this.altNext != null) {
					// split
					this.segmentPosition.symbol = Symbol.SWITCH;
					
					this.altNext.get().layout(x + 2, y + 1, this);
					this.next.get().layout(x + 2, y, this);
					this.altNext.get().title();
				} else {
					// merge

					this.segmentPosition.symbol = Symbol.MERGE;
					
					this.next.get().layout(x + 2, y - 1, this);
				}
			}
		}

		@Override
		public SegmentPosition getPosition() {
			return segmentPosition;
		}

	}

	class Intermediate extends Tracks.StraightHandler<Layout>implements Layout {
		final SegmentPosition segmentPosition;
		final int width;
		
		public Intermediate(Segment segment, Symbol symbol, int width) {
			super(segment);
			this.segmentPosition = new SegmentPosition();
			this.segmentPosition.segment = segment;
			this.segmentPosition.symbol=symbol;
			this.width = width;
		}

		@Override
		public void layout(int x, int y, Layout from) {
			
			if(!prev.getTrack().equals(segment.track)){
				title();
			}
			
			if (x > 0 && isRoot())
				return;

			if (x > this.segmentPosition.x) {
				this.segmentPosition.x = max(x, this.segmentPosition.x);
				this.segmentPosition.y = y;
				next.get().layout(x + this.width, y, this);
			}

		}

		@Override
		public SegmentPosition getPosition() {
			return segmentPosition;
		}
		
		@Override
		public void adjustWidth() {
				
			this.segmentPosition.width = next.get().getPosition().x - this.segmentPosition.x;
		}

	}

	class Block extends Tracks.StraightHandler<Layout>implements Layout {
		final SegmentPosition segmentPosition;
		boolean done;

		public Block(Segment segment) {
			super(segment);
			this.segmentPosition = new SegmentPosition();
			this.segmentPosition.segment = segment;
		}

		@Override
		public void layout(int x, int y, Layout from) {
			this.segmentPosition.x = x;
			this.segmentPosition.y = y;
			this.segmentPosition.symbol=Symbol.BLOCK;
		}

		@Override
		public SegmentPosition getPosition() {
			return segmentPosition;
		}

	}

	public SegmentHandler<Layout> block(Segment segment) {
		return new Block(segment);
	}

	public SegmentHandler<Layout> curve(Segment segment) {
		return new Intermediate(segment, SegmentPosition.Symbol.PLAIN, 1);
	}

	public SegmentHandler<Layout> straight(Segment segment) {
		return new Intermediate(segment, SegmentPosition.Symbol.PLAIN, 1);
	}

	public SegmentHandler<Layout> signal(Segment segment) {
		return new Intermediate(segment, SegmentPosition.Symbol.SIGNAL, 0);
	}

	public SegmentHandler<Layout> locator(Segment segment) {
		return new Intermediate(segment, SegmentPosition.Symbol.LOCATOR, 0);
	}

	public SegmentHandler<Layout> swtch(Segment segment) {
		return new Switch(segment);
	}

}
