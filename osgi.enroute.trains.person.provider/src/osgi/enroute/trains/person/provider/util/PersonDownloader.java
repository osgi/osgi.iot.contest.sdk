package osgi.enroute.trains.person.provider.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import osgi.enroute.trains.passenger.api.Person;

/**
 * This is a standalone utility that reaches out to the ECE website and downloads all attendee information and 
 * pictures and input those as person information for the system. The resulting json/pictures should not be stored
 * in the git repo 
 * 
 * @author tverbele
 *
 */
public class PersonDownloader  {

	private static int count = 0;
	
	public static void main(String[] args) {
		
		File output = new File("resources/persons.json");
		
		try(
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)))){
		
			writer.write("{ \n");
			
			String rootURI = "https://www.eclipsecon.org/europe2016/community?field_profile_first_value=&field_profile_last_value=&field_profile_org_value=";

			for(int page=0;page<6;page++){
				String uri = rootURI + (page==0 ? "" : "&page="+page);
				Document doc = Jsoup.connect(uri).get();
				Elements persons = doc.select(".solstice-user-profile");
				for(Element p : persons){
					
					String name = "";
					String firstName = "";
					String lastName = "";
					String company = null;
					String image = null;
					
					Elements nameLinks = p.select("h3 > a[href]");
					for(Element link : nameLinks){
						name = link.html();
					}
					
					Elements pictureLinks = p.select("div.user-picture > a[href]");
					for(Element link : pictureLinks){
						Element child = link.children().first();
						if(child!= null && child.hasAttr("src")){
							image = child.attr("src");
						}
					}
					
					Elements h5 = p.select("h5");
					for(Element e : h5){
						company = e.html();
					}
					
					String[] split = name.split(" ");
					firstName = split[0];
					for(int i=1;i<split.length;i++){
						if(split[i].length() > 2)
							lastName += split[i]+" ";
					}
					lastName = lastName.trim();
		
					if(firstName.isEmpty() || lastName.isEmpty())
						continue;
					
					
					Person person = new Person();
					person.id = ""+count++;
					person.firstName = firstName;
					person.lastName = lastName;
					person.company = company;
					
					// download picture
					try {
						String picURL = "pictures/"+person.id+".jpg";
						URL website = new URL(image);
						try (InputStream in = website.openStream()) {
						    Files.copy(in, new File(picURL).toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
						
						person.picture = picURL;
					} catch(Exception e){}
					
					System.out.println(person);
	
					String personString = String.format("{"
							+ "\"id\": \"%s\","
							+ "\"firstName\": \"%s\","
							+ "\"lastName\": \"%s\","
							+ "\"company\": \"%s\","
							+ "\"picture\": \"%s\""
							+ "}", person.id, person.firstName, person.lastName,
							person.company, person.picture);
					
					
					writer.write("\""+person.id+"\":"+personString+", \n");
				}
			}
			
			writer.write("}");
			writer.flush();
		} catch(Exception e){
			e.printStackTrace();
		}
		
	}


}
