package osgi.enroute.trains.operator.api;

import java.util.List;

import org.osgi.dto.DTO;


public class TrainOperatorInfo extends DTO {

	public String id;
	public String url;
	public String icon;

	public List<String> trains;
	public List<String> stations;
	
}
