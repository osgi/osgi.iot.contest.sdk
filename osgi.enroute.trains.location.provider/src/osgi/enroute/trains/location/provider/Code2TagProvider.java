package osgi.enroute.trains.location.provider;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        immediate = true,
        name="osgi.enroute.trains.location.code2tag",
    	configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Code2TagProvider implements Code2Tag {

	private Map<String, Integer> codeToTag;
	private Map<Integer, String> tagToCode;

	@ObjectClassDefinition
	@interface Config {
		/**
		 * short code to RFID tag uuid
		 * 
		 * {@code code:tag}
		 */
		String[] code2tag();
	}
	
	@Activate
	void activate(Config config){
		
		codeToTag = new HashMap<String, Integer>();
		tagToCode = new HashMap<Integer, String>();
		for(String line : config.code2tag()){
			String[] split = line.split(":");
			int tag = Integer.parseInt(split[0]);
			String code = split[1];
			// only keep last 5 characters
			code = code.substring(5);
			codeToTag.put(code, tag);
			tagToCode.put(tag, code);
		}
	}

	@Override
	public Integer getTag(String code) {
		return codeToTag.get(code);
	}

	@Override
	public String getCode(int tag) {
		return tagToCode.get(tag);
	}
}
