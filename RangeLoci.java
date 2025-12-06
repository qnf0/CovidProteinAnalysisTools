package flyhigh;

public class RangeLoci {

	int start;
	int end;
	String chromosome;
	boolean withinHotspot = false;
	public RangeLoci(int start, int end) {
		super();
		this.start = start;
		this.end = end;
		
	}
	
	  /**
     * Returns true if this RangeLoci is fully contained within the given HotSpot,
     * or if at least 50% of its length overlaps the hotspot region.
     */
    public boolean isContainedWithin(HotSpot spot) {
        if (spot == null) {
        	System.out.println("null hotspot in RangeLoci, exitting program");
            System.exit(1);
           // return false;

        }
        
        

        int rangeLength = this.end - this.start;
        if (rangeLength <= 0) return false;

        // Compute overlap length between [start, end] and [spot.start, spot.end]
        int overlapStart = Math.max(this.start, spot.start);
        int overlapEnd = Math.min(this.end, spot.end);
        int overlapLength = Math.max(0, overlapEnd - overlapStart);

        // Calculate fraction overlap
        double overlapFraction = (double) overlapLength / rangeLength;

        // Full containment or ≥50% overlap counts as true
        boolean result = (this.start >= spot.start && this.end <= spot.end) || overlapFraction >= 0.5;
//        if (result == true) {
//        	System.out.println("in spot of "+this.start+";"+this.end+" within "+spot.start+":"+spot.end);
//        }
        return result;
    }
	
    
    public boolean overlaps(RangeLoci other) {
        if (other == null) return false;
        return this.end > other.start && this.start < other.end;
    }
    public int mp() {
        return (int) ((((long) start + (long) end)) / 2);
    }
    
    public int getLen() {
    	return Math.abs(this.end-this.start);
    }
}
