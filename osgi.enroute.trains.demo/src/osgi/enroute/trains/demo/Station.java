package osgi.enroute.trains.demo;

public class Station {

	public enum Type {
		CARGO, PARKING, REGULAR
	}
	
	public String name;

	public String segment;

	public Type type;
	
	public String train;
}
