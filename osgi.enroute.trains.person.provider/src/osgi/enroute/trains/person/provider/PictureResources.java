package osgi.enroute.trains.person.provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

@Component(immediate=true)
public class PictureResources {

	@Reference
	void setHttpService(HttpService http){
		try {
			http.registerResources("/osgi.enroute.trains/pictures", "pictures", null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
