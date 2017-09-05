package osgi.enroute.trains.controller.segment;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

import osgi.enroute.trains.segment.api.Color;
import osgi.enroute.trains.segment.api.SignalSegmentController;

@Designate(ocd = SignalSegmentImpl.Config.class, factory = true)
@Component(name = "osgi.enroute.trains.controller.signal", property = "service.exported.interfaces=*",
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SignalSegmentImpl implements SignalSegmentController {
    private GpioPinDigitalOutput green;
    private GpioPinDigitalOutput red;
    private Color color;

    @Reference
    private GpioController gpio;
    private Config config;

    @ObjectClassDefinition
    @interface Config {
    	String controller_segment();
    	
    	String green();

        String red();
    }

    @Activate
    void activate(Config config) {
        this.config = config;
        info("activate");
        green = setup(config.green());
        red = setup(config.red());
        signal(Color.YELLOW);
    }

    @Deactivate
    void deactivate() {
        info("deactivate");
        green.setState(false);
        red.setState(false);
        red.blink(0);
    }

    @Override
    public void signal(Color color) {
        this.color = color;
        switch (color) {
        case GREEN:
            green.setState(true);
            red.setState(false);
            red.blink(0);
            break;

        case YELLOW:
            green.setState(false);
            red.setState(false);
            red.blink(500);
            break;

        default:
        case RED:
            green.setState(false);
            red.setState(true);
            red.blink(0);
            break;
        }
    }

    @Override
    public Color getSignal() {
        return color;
    }

    private GpioPinDigitalOutput setup(String name) {
        Pin pin = RaspiPin.getPinByName(name);
        if (pin == null) {
            info("Pin<{}> is null", name);
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
        return "Signal[green=" + green + ", red=" + red + ", color=" + color + ",cntl=" + config.controller_segment()
                + "]";
    }

    private void info(String fmt, Object... args) {
        String ident = String.format("Signal<%d>: ", config.controller_segment());
        System.out.printf(ident + fmt.replaceAll("\\{}", "%s") + "\n", args);
    }

}
