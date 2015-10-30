package osgi.enroute.iot.pi.lirc.provider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.naming.ConfigurationException;

import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.gpio.util.Wave;

/**
 * It is hard to find any information about the LIRC interface. From
 * http://www.nishioka.com/train/ we learned that the LIRC is basically a
 * sequence of int's where each int specifies the width of a pulse. The first
 * one is on, the second one off, third on. The total number must be odd, the
 * LIRC driver switches off at the end automatically.
 * 
 * According to our sources, the option softcarrier=1 must be chosen but not
 * found anywhere this is defined. On a Raspberry, make sure you configure the
 * Pi. On modern OS's this requires that you use the device tree.
 * 
 * <pre>
 * 	/boot/config.txt:
 * 	dtoverlay=dtoverlay=lirc-rpi,softcarrier=0
 * </pre>
 * 
 * By default the IR out signal is on GPIO17 (GPIO00 for Pi4J numbering).
 * However, with the dtoverlay you can override it.
 * 
 */

public class LIRCImpl extends ICAdapter<Wave, Void> implements Wave {
	private ByteOrder endianness = ByteOrder.nativeOrder();
	private File file;

	public LIRCImpl() throws Exception {
		String path = "/dev/lirc0";
		this.file = new File(path);
		if (!file.exists())
			throw new ConfigurationException(
					path
							+ " does not exist. LIRC requires device tree + dtoverlay=lirc-rpi,softcarrier=0 in /boot/config.txt");
		
		endianness = ByteOrder.LITTLE_ENDIAN;
	}


	@Override
	public void send(int[] times) throws Exception {
		ByteBuffer pulses = ByteBuffer.allocate(times.length * 4);
		pulses.order(endianness);

		int length = times.length;

		// lirc_rpi doesn't want to see trailing space (requires count to be
		// odd)

		if ((times.length & 1) == 0)
			length--;

		for (int i = 0; i < length; i++) {
			pulses.putInt(times[i] & 0xFF_FF_FF);
		}

		byte[] array = pulses.array();

		try (FileOutputStream fd = new FileOutputStream(file)) {
			fd.write(array, 0, length * 4);
		}
	}

	@Override
	public void setCircuitBoard(CircuitBoard board) {
		super.setCircuitBoard(board);
	}
}
