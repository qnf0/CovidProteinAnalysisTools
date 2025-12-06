package flyhigh;
import java.util.ArrayList;
import java.util.List;

public class MotifLock {
int site;
int siteAligned;
String species;
Motif motif;
public MotifLock(int site, int siteAligned, String species, Motif motif) {
	super();
	this.site = site;
	this.siteAligned = siteAligned;
	this.species = species;
	this.motif = motif;
}
@Override
public String toString() {
	return "MotifLock [site=" + site + ", siteAligned=" + siteAligned + ", species=" + species + ", motif=" + motif
			+ "]";
}





}
