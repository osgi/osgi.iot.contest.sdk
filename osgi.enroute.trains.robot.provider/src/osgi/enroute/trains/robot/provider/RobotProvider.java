package osgi.enroute.trains.robot.provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import be.iminds.iot.robot.api.arm.Arm;
import be.iminds.iot.robot.api.omni.OmniDirectional;
import be.iminds.iot.sensor.api.LaserScanner;
import osgi.enroute.debug.api.Debug;
import osgi.enroute.trains.robot.api.RobotController;

@Component(immediate=true,property = {
		Debug.COMMAND_SCOPE + "=robot",
		Debug.COMMAND_FUNCTION + "=reach",
		Debug.COMMAND_FUNCTION + "=close",
		Debug.COMMAND_FUNCTION + "=fetch",
		Debug.COMMAND_FUNCTION + "=put",
		Debug.COMMAND_FUNCTION + "=open",
		Debug.COMMAND_FUNCTION + "=reset",
		Debug.COMMAND_FUNCTION + "=stop",
		Debug.COMMAND_FUNCTION + "=y",
		Debug.COMMAND_FUNCTION + "=h",
		Debug.COMMAND_FUNCTION + "=xoffset",
		Debug.COMMAND_FUNCTION + "=aoffset"
}, service={RobotProvider.class,RobotController.class})
public class RobotProvider implements RobotController {

	@Reference
	protected Arm arm;
	
	@Reference
	protected OmniDirectional base;
	
	@Reference
	protected LaserScanner lidar;
	
	private boolean hasContainer = false;
	
	private float x_grip = 0.0f;
	private float y_grip = 0.3f;
	private float a_grip = 1.57f;
	private float h_grip = 0.088f;
	private float x_offset = 0.13f;
	private float a_offset = 0.0f;
	

	public void stop(){
		base.stop();
		arm.stop();
	}
	
	public float y(){
		return y_grip;
	}
	
	public void y(float y){
		this.y_grip = y;
	}
	
	public float h(){
		return h_grip;
	}
	
	public void h(float h){
		this.h_grip = h;
	}
	
	public float xoffset(){
		return x_offset;
	}
	
	public void xoffset(float o){
		this.x_offset = o;
	}

	public float aoffset(){
		return a_offset;
	}
	
	public void aoffset(float o){
		this.a_offset = o;
	}
	
	public synchronized boolean unload(){
		if(hasContainer){
			System.out.println("We are already carrying a container");
			return false;
		}
		
		System.out.println("Unload container from the train");
		
		try {
			Promise<Arm> toWaitFor = open().then(p -> reach()).then(p -> close()); 
			toWaitFor.then( p -> fetch());
			toWaitFor.getValue();
			hasContainer = true;
		} catch (Exception e) {
			System.out.println("Failed to unload container from the train");
			return false;
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
			toWaitFor.then(p -> hover());
			toWaitFor.getValue();
			hasContainer = false;
		} catch (Exception e) {
			System.out.println("Failed to load container on the train");
			return false;
		}	
		
		return true;
	}
	
	public synchronized void reset(){
		try {
			arm.openGripper().then(// first go to candle before reset
					p -> arm.setPositions(2.92510465f, 1.103709733f, -2.478948503f, 1.72566195f))
					.then(p -> arm.reset()).getValue();
			hasContainer = false;
		} catch (Exception e) {
			System.out.println("Failed to reset robot");
		}
	}
	
	
	public Promise<Arm> reach(){
		try {
			updateGripperTarget(false);
		} catch(Exception e){
			Deferred<Arm> d = new Deferred();
			d.fail(e);
			return d.getPromise();
		}
		
		return arm.moveTo(y_grip, x_grip, 0.25f)
			.then(p -> arm.setPosition(4, a_grip))
			.then(p -> arm.moveTo(y_grip, x_grip, h_grip));
	}
	
	public Promise<Arm> close(){
		return arm.closeGripper();
	}
	
	public Promise<Arm> fetch(){
		return arm.moveTo(y_grip, x_grip, 0.1f)
				.then(p -> arm.moveTo(y_grip, x_grip, 0.25f))
				.then(p -> arm.setPosition(0, 0f));
	}
	
	public Promise<Arm> put(){
		try {
			updateGripperTarget(true);
		} catch(Exception e){
			Deferred<Arm> d = new Deferred();
			d.fail(e);
			return d.getPromise();
		}
		
		return arm.setPosition(0, 2.94f) 
				.then(p -> arm.setPosition(4, a_grip))
				.then(p -> arm.moveTo(y_grip, x_grip, 0.25f))
				.then(p -> arm.moveTo(y_grip, x_grip, h_grip+0.01f));	
	}
	
	public Promise<Arm> open(){
		return arm.openGripper();
	}
	
	public Promise<Arm> hover(){
		return arm.moveTo(y_grip, x_grip, 0.12f)
			.then(p -> arm.moveTo(y_grip, x_grip, 0.25f))
			.then(p -> arm.moveTo(0.3f, 0.0f, 0.25f));
	}
	
	private void updateGripperTarget(boolean load) throws Exception {
		
		float[] lidarData = lidar.getValue().data;
		float minAngle = lidar.getMinAngle();
		float maxAngle = lidar.getMaxAngle();
		float step = (maxAngle-minAngle)/lidarData.length;

		float[] x = new float[lidarData.length];
		float[] y = new float[lidarData.length];
		float angle = minAngle;
		for(int i =0; i<lidarData.length;i++){
			x[i] = (float) (lidarData[i]*Math.sin(angle));
			y[i] = (float) (lidarData[i]*Math.cos(angle));
			angle+=step;
			System.out.println(i+" "+angle);
		}

		System.out.println("i  raw  x  y");
		for(int i=0;i<lidarData.length;i++){
			System.out.println(i+" "+lidarData[i]+" "+x[i]+" "+y[i]);
		}
		
		int s1 = -1;
		int e1 = -1;
		int s2 = -1;
		int e2 = -1;
		
		for(int i=lidarData.length-1;i>0;i--){
			if(lidarData[i] < 0.3 && lidarData[i] > 0 && y[i] < 0.2f){
				if(s1 == -1){
					s1 = i;
				} else if(e1 != -1 && s2 == -1){
					s2 = i;
				}
			} else if(lidarData[i] > 0){
				if(s2 != -1 && e2 == -1){
					e2 = i+1;
				} else if(s1 != -1 && e1 == -1){
					e1 = i+1;
				}
			}
		}
		
		System.out.println(s1+" "+e1+" "+s2+" "+e2);
		
		if(load){
			// load container on the train
			if( s2 != -1 && e2 != -1
					&& s2 - e2 > 4 ){
				System.out.println("Container detected?!");
				throw new Exception("There is already a container on this train?!");
			}
			
			System.out.println("Train start "+s1 + " " +x[s1]
					+" - Train end "+e1+ " "+ x[e1]);
			
			x_grip = x[e1] - x_offset;
			//y_grip  = 0.185f + y[e1];
			a_grip = 1.57f - (float)Math.tan(x_grip/y_grip) + a_offset;
			
			System.out.println("Pose "+x_grip+" "+y_grip);
			System.out.println("Angle "+a_grip);
		} else {
			// unload container from the train
			if(s1 == -1 || e1 == -1 || s2 == -1 || e2 == -1
					|| s2 - e2 < 4 ){
				System.out.println("No container detected?!");
				throw new Exception("No container found to unload");
			}
			
			System.out.println("Train start "+s1 + " " +x[s1]
					+" - Train end "+e1+ " "+ x[e1]
					+" - Container start "+s2+ " "+ x[s2]
					+" - Container end "+e2+ " "+ x[e2]);
			
			x_grip = (x[s2]+x[e2])/2;
			//y_grip = 0.185f + y[(s2+e2)/2];
			a_grip = 1.57f - (float)Math.tan(x_grip/y_grip) + a_offset;
			
			System.out.println("Pose "+x_grip+" "+y_grip);
			System.out.println("Angle "+a_grip);
		}
	}
	
}
