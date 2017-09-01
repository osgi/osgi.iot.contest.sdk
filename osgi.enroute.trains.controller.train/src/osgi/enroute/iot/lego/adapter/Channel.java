package osgi.enroute.iot.lego.adapter;

public enum Channel {
	CH1(0b0000_0000_0000_0000), CH2(0b0001_0000_0000_0000), CH3(
			0b0010_0000_0000_0000), CH4(0b0011_0000_0000_0000);

	short mask;

	Channel(int s) {
		this.mask = (short) s;
	}
}

