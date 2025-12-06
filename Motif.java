package flyhigh;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Motif {

    public final String motifGeneSymbol;
    public final String motifUniprotEntryName;
    public final String motifName;
    public final String motifGroup;
    public final double score;
    public final double percentile;
    public final String protein;
    public final String site;
    public final String siteSequence;
    public final double surfaceAccessibilityValue;
    public int siteAligned;
    
    public Motif(String motifGeneSymbol,
                 String motifUniprotEntryName,
                 String motifName,
                 String motifGroup,
                 double score,
                 double percentile,
                 String protein,
                 String site,
                 String siteSequence,
                 double surfaceAccessibilityValue) {

        this.motifGeneSymbol = motifGeneSymbol;
        this.motifUniprotEntryName = motifUniprotEntryName;
        this.motifName = motifName;
        this.motifGroup = motifGroup;
        this.score = score;
        this.percentile = percentile;
        this.protein = protein;
        this.site = site;
        this.siteSequence = siteSequence;
        this.surfaceAccessibilityValue = surfaceAccessibilityValue;
    }

    @Override
    public String toString() {
        return motifGeneSymbol + " | " + motifName + " | " + site + " | " + score;
    }
    public int getSiteOrig() {
    	return Integer.parseInt(this.site.replaceAll("[A-Za-z]", ""));

    }
    
    public void getAlignedSite(String aligned) {
    	
    	int unalignedLock = Integer.parseInt(this.site.replaceAll("[A-Za-z]", ""));
    	int alignedLock = -1;
    	List<String> alignedCharacters  = splitToChars(aligned);
    	int absolute = 0;
    	int relative = 0;
    	for (String c : alignedCharacters) {
    		absolute +=1; 
    		if (c.equals("-")) {
    			 
    		
    		}
    		else {
    			relative +=1;
    		}
    		if (relative == unalignedLock) {
    			alignedLock = absolute; 
    			break;
    		}
    	}
    	
    	
    	this.siteAligned = alignedLock;

    }
    
    public static List<String> splitToChars(String s) {
        List<String> out = new ArrayList<>(s.length());
        for (int i = 0; i < s.length(); i++) {
            out.add(String.valueOf(s.charAt(i)));
        }
        return out;
    }

}
