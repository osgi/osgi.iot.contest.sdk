package osgi.enroute.trains.robot.provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

import be.iminds.iot.robot.api.arm.Arm;
import be.iminds.iot.robot.api.omni.OmniDirectional;
import be.iminds.iot.sensor.api.LaserScanner;
import osgi.enroute.debug.api.Debug;
import osgi.enroute.trains.robot.api.RobotController;

@Component(immediate=true,property = {
		Debug.COMMAND_SCOPE + "=trains",
		Debug.COMMAND_FUNCTION + "=reach",
		Debug.COMMAND_FUNCTION + "=close",
		Debug.COMMAND_FUNCTION + "=fetch",
		Debug.COMMAND_FUNCTION + "=put",
		Debug.COMMAND_FUNCTION + "=open",
		Debug.COMMAND_FUNCTION + "=reset"
}, service={RobotProvider.class,RobotController.class})
public class RobotProvider implements RobotController {

	@Reference
	protected Arm arm;
	
	@Reference
	protected OmniDirectional base;
	
	@Reference
	protected LaserScanner lidar;
	
	private boolean hasContainer = false;
	
	public synchronized boolean unload(){
		if(hasContainer){
			System.out.println("We are already carrying a container");
			return false;
		}
		
		System.out.println("Unload container from the train");
		
		// TODO check if there is a train with container present?!

		
		try {
			Promise<Arm> toWaitFor = open().then(p -> reach()).then(p -> close()); 
			toWaitFor.then( p -> fetch());
			toWaitFor.getValue();
			hasContainer = true;
		} catch (Exception e) {
		}
		
		return true;
	}
	
	
	
	public synchronized boolean load(){
		if(!hasContainer){
			System.out.println("We don't have a container to load on the train");
			return false;
		}
		
		System.out.println("Load container on the train");
		
		// TODO check if there is a train to load the container on?
		
		try {
			Promise<Arm> toWaitFor = put().then(p -> open());
			toWaitFor.then(p -> reset());
			toWaitFor.getValue();
			hasContainer = false;
		} catch (Exception e) {
		}	
		
		return true;
	}
	
	
	public Promise<Arm> reach(){
		return arm.setPosition(4, 1.57f)
			.then(p -> arm.moveTo(0.3f, 0f, 0.3f))
			.then(p -> arm.moveTo(0.3f, 0f, 0.1f))
			.then(p -> arm.moveTo(0.3f, 0f, 0.05f));
	}
	
	public Promise<Arm> close(){
		return arm.closeGripper();
	}
	
	public Promise<Arm> fetch(){
		return arm.moveTo(0.3f, 0f, 0.1f)
				.then(p -> arm.moveTo(0.3f, 0f, 0.25f))
				.then(p -> arm.setPosition(0, 0f))
				.then(p -> arm.setPositions(0f, 1.2f, -1.5f));
	}
	
	public Promise<Arm> put(){
		return arm.setPositions(0.0f, 0.92f, -1.54f)
				.then(p -> arm.setPosition(0, 2.94f))
				.then(p -> arm.moveTo(0.3f, 0f, 0.1f))
				.then(p -> arm.moveTo(0.3f, 0f, 0.05f));
	}
	
	public Promise<Arm> open(){
		return arm.openGripper();
	}
	
	public Promise<Arm> reset(){
		return arm.moveTo(0.3f, 0f, 0.1f)
			.then(p -> arm.moveTo(0.3f, 0f, 0.25f))
			.then(p -> arm.reset());
	}
}
