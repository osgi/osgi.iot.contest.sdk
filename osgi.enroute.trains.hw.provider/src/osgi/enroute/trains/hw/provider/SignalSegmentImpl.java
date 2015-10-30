package osgi.enroute.trains.hw.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

import osgi.enroute.trains.cloud.api.Color;
import osgi.enroute.trains.controller.api.SignalSegmentController;

@Designate(ocd=SignalSegmentImpl.Config.class, factory=true)
@Component(name = "osgi.enroute.trains.hw.signal", property="service.exported.interfaces=*", configurationPolicy=ConfigurationPolicy.REQUIRE)
public class SignalSegmentImpl implements SignalSegmentController {
	private GpioPinDigitalOutput green;
	private GpioPinDigitalOutput red;
	private Color color = Color.RED;

	@Reference
	private GpioController gpio;
	private Config config;

	@ObjectClassDefinition
	@interface Config {
		int controller();

		String segment();

		String green();

		String red();
	}

	@Activate
	void activate(Config config) {
		this.config = config;
		green = setup(config.green());
		red = setup(config.red());
		signal(color);
	}

	@Override
	public void signal(Color color) {
		this.color = color;
		switch (color) {
		case GREEN:
			green.setState(true);
			red.setState(false);
			break;
		case YELLOW:
			green.setState(false);
			red.pulse(1);
			break;
		default:
		case RED:
			green.setState(false);
			red.setState(true);
			break;
		}
	}

	@Override
	public Color getSignal() {
		return color;
	}

	private GpioPinDigitalOutput setup(String name) {
		Pin pin = RaspiPin.getPinByName(name);
		if ( pin == null) {
			System.out.println("Pin is " + name + " is null");
			return null;
		}
		for (GpioPin e : gpio.getProvisionedPins()) {
			if (e.getPin().equals(pin)) {
				gpio.unprovisionPin(e);
				break;
			}
		}
		return this.gpio.provisionDigitalOutputPin(pin);
	}

	@Override
	public String toString() {
		return "Signal[green=" + green + ", red=" + red + ", color=" + color + ",cntl=" + config.controller() + ", seg="+config.segment() + "]";
	}

}
