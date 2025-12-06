package flyhigh;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
public class Site {
int location;
int phosphorylationNumber;
double gapPercent;
public Site(int location, int phosphorylationNumber, double gapPercent) {
	super();
	this.location = location;
	this.phosphorylationNumber = phosphorylationNumber;
	this.gapPercent = gapPercent;
}


public static List<Site> parseSites(String path) throws IOException{
	List<String> raw = variousfunctions.loadLines(Path.of(path));
	List<Site>out = new ArrayList<>();
	raw.remove(0);
	for (String r : raw) {
	String [] p=	r.split(",");
	Site s = new Site (Integer.parseInt(p[0]),Integer.parseInt(p[2]),Double.parseDouble(p[1]));
	out.add(s);
	}
	return out;
}

}
