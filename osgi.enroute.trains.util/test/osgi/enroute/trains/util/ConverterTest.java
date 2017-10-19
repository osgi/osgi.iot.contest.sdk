package osgi.enroute.trains.util;

import org.osgi.util.converter.Converter;

import junit.framework.Assert;
import junit.framework.TestCase;
import osgi.enroute.trains.demo.api.DemoObservation;
import osgi.enroute.trains.segment.api.Color;
import osgi.enroute.trains.segment.api.SegmentCommand;
import osgi.enroute.trains.util.converter.TrainsConverter;

public class ConverterTest extends TestCase {

	private Converter converter = TrainsConverter.getTrainsConverter();
	
	public void testLongStringConversion() throws Exception {
		DemoObservation msg = new DemoObservation();
		msg.message = "This is a very long string that should be converted correctly!";
		msg.time = System.currentTimeMillis();
		msg.type = DemoObservation.Type.MESSAGE;
		
		byte[] bytes = converter.convert(msg).to(byte[].class);
		DemoObservation result = converter.convert(bytes).to(DemoObservation.class);
		
		Assert.assertEquals(msg.message, result.message);
		Assert.assertEquals(msg.time, result.time);
		Assert.assertEquals(msg.type, result.type);
	}
	
	public void testSignalCommandConversion() throws Exception {
		SegmentCommand c = new SegmentCommand();
		c.type = SegmentCommand.Type.SIGNAL;
		c.segment = "S01";
		c.signal = Color.GREEN;
		
		byte[] bytes = converter.convert(c).to(byte[].class);
		SegmentCommand result = converter.convert(bytes).to(SegmentCommand.class);
		
		Assert.assertEquals(c.type, result.type);
		Assert.assertEquals(c.signal, result.signal);
		Assert.assertEquals(c.segment, result.segment);
	}

	public void testSwitchCommandConversion() throws Exception {
		SegmentCommand c = new SegmentCommand();
		c.type = SegmentCommand.Type.SWITCH;
		c.segment = "X01";
		c.alternate = true;
		
		byte[] bytes = converter.convert(c).to(byte[].class);
		SegmentCommand result = converter.convert(bytes).to(SegmentCommand.class);
		
		Assert.assertEquals(c.type, result.type);
		Assert.assertEquals(c.alternate, result.alternate);
		Assert.assertEquals(c.segment, result.segment);
	}
}
