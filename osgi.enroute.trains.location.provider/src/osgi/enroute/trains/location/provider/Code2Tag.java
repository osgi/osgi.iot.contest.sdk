package osgi.enroute.trains.location.provider;

public interface Code2Tag {

	Integer getTag(String code);
	
	String getCode(int tag);
	
}
