package osgi.enroute.trains.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.ConverterFunction;
import org.osgi.util.converter.Converters;

@Component(immediate=true)
public class TrainsConverter {

	private final static Converter converter = Converters.standardConverter();
	
	@Activate
	void activate(BundleContext context){
		// register trains converter
		context.registerService(Converter.class.getName(), getTrainsConverter(), new Hashtable<String, Object>());
	}
	
	public static Converter getTrainsConverter(){
		ConverterBuilder builder = Converters.newConverterBuilder();
		
		return builder.rule(new  ConverterFunction() {
			
			@Override
			public Object apply(Object obj, Type targetType) throws Exception {
				if(targetType.equals(byte[].class)){
					// convert to byte array using toString representation and get the bytes
					// works for DTOs (should we check whether obj is DTO?)
					return obj.toString().getBytes();
				} else {
					return ConverterFunction.CANNOT_HANDLE;
				}
			}
		}).rule(new  ConverterFunction() {
			
			@Override
			public Object apply(Object obj, Type targetType) throws Exception {
				if(obj instanceof byte[]){
					// first create a String from the bytes
					String s = new String((byte[])obj);
					// parse JSON
					JSONObject jsonObject = new JSONObject(s);
					Map<String, Object> map = mapFromJson(jsonObject);
					return converter.convert(map).to(targetType);
				} else {
					return ConverterFunction.CANNOT_HANDLE;
				}
			}
		}).build();
		
	}

	private static Map<String, Object> mapFromJson(JSONObject jsonObject) throws Exception{
		Map<String, Object> map = new HashMap<String, Object>(); 
		Iterator<?> keyset = jsonObject.keys();
        while (keyset.hasNext()) {
            String key =  (String) keyset.next();
            Object value = jsonObject.get(key);
            if(value.equals(JSONObject.NULL)){
            	map.put(key, null);
            } else if(value instanceof JSONArray){
            	// convert arrays to Object[]
            	map.put(key, arrayFromJson((JSONArray)value));
            } else if(value instanceof JSONObject){
            	map.put(key, mapFromJson((JSONObject)value));
            } else {
            	map.put(key, value);
            }
        }
        return map;
	}
	
	private static Object[] arrayFromJson(JSONArray jsonArray) throws Exception{
    	Object[] array = new Object[jsonArray.length()];
    	for(int i=0;i<array.length;i++){
    		array[i] = jsonArray.get(i);
    		if(array[i].equals(JSONObject.NULL)){
    			array[i] = null;
    		} else if(array[i] instanceof JSONArray){
    			array[i] = arrayFromJson((JSONArray)array[i]);
    		} else if(array[i] instanceof JSONObject){
    			array[i] = mapFromJson((JSONObject)array[i]);
    		}
    	}
    	return array;
	}

}
