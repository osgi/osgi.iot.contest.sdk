package osgi.enroute.trains.robot.provider;

import java.nio.ByteBuffer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.converter.Converter;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.mqtt.api.MQTTService;
import osgi.enroute.trains.robot.api.RobotCommand;
import osgi.enroute.trains.robot.api.RobotController;
import osgi.enroute.trains.robot.api.RobotObservation;

/**
 * 
 */
@Component(immediate = true, property = {
		Debug.COMMAND_SCOPE +"=trains",
		Debug.COMMAND_FUNCTION + "=load", //
		Debug.COMMAND_FUNCTION + "=unload"
}, service=RobotCommands.class)
public class RobotCommands {

	@Reference
	protected MQTTService mqtt;
	
	@Reference
	protected Converter converter;
	
	@Reference
	protected RobotController robot;
	
	@Activate
	void activate(){
		try {
			mqtt.subscribe(RobotCommand.TOPIC).forEach(msg ->{
				RobotCommand c = converter.convert(msg.payload().array()).to(RobotCommand.class);
				switch(c.type){
				case LOAD:
					System.out.println("Load container on the train");
					load();
					break;
				case UNLOAD:
					System.out.println("Unload container from the train");
					unload();
					break;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}	
	
	public void load(){
		RobotObservation o = new RobotObservation();
		o.type = RobotObservation.Type.LOADED;
		o.succes = robot.load();
		o.time = System.currentTimeMillis();
		observation(o);
	}
	
	public void unload(){
		RobotObservation o = new RobotObservation();
		o.type = RobotObservation.Type.UNLOADED;
		o.succes = robot.unload();
		o.time = System.currentTimeMillis();
		observation(o);
	}
	
	private void observation(RobotObservation o){
		try {
			mqtt.publish(RobotObservation.TOPIC, ByteBuffer.wrap( converter.convert(o).to(byte[].class)));
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
