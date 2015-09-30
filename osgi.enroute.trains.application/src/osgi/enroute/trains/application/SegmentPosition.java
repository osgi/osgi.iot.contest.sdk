package osgi.enroute.trains.application;

import org.osgi.dto.DTO;

import osgi.enroute.trains.cloud.api.Color;
import osgi.enroute.trains.cloud.api.Segment;

public class SegmentPosition extends DTO {
	public enum Symbol {
		SWITCH, MERGE, SIGNAL,LOCATOR, BLOCK,PLAIN;
	}
	
	public Segment segment;
	public int x=-1, y=0, width=1;
	public Symbol symbol;
	public boolean alt = false;
	public String rfid;
	public Color color = Color.RED;
	public String title;

}

