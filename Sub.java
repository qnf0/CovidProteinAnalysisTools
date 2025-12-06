package flyhigh;

import java.util.List;

public class Sub{
	int start;
	int end;
	double percRaw;
	String peptide;
	String allele;
	boolean used;
	String species;
	public Sub(int start, int end, double percRaw, String allele, String peptide, String species) {
		super();
		this.start = start;
		this.end = end;
		this.allele = allele;
		this.percRaw = percRaw;
		this.peptide = peptide;
		this.species = species;
		boolean used = false;
	}
	
	public boolean equals(Sub comp) {
		
		
		boolean out = false;
		
		if (comp.peptide.equals(this.peptide) && this.start == comp.start && this.end == comp.end && this.species.equals(comp.species)){
			out = true;
		}
		return out;
	}

	
//    * Computes a composite epitope score for a set of Sub entries
//    * that all correspond to the same peptide/motif (different alleles).
//    *
//    * Score = breadth * meanStrength
//    *   breadth      = (# alleles with percRaw <= cutoffPercentile) / (total alleles)
//    *   meanStrength = average of max(0, log10(100 / percRaw)) over those alleles
//    *
//    * @param subs list of Sub, all for the same motif in one species/protein
//    * @param cutoffPercentile binder cutoff in percent (e.g. 2.0 for 2%)
//    * @return composite score (0 if no binders or input empty)
//    */
   public static double computeEpitopeScore(List<Sub> subs, double cutoffPercentile) {
       if (subs == null || subs.isEmpty()) {
           return 0.0;
       }

       int totalAlleles = subs.size();
       int binderCount = 0;
       double strengthSum = 0.0;

       for (Sub s : subs) {
           double p = s.percRaw;  // percentile rank (in %)

           // Skip invalid or non-positive percentiles
           if (p <= 0.0 || Double.isNaN(p)) {
               continue;
           }

           // Only consider binders up to cutoffPercentile (e.g. 2.0%)
           if (p <= cutoffPercentile) {
               binderCount++;

               // Strength: larger for smaller percentiles
               double strength = Math.log10(100.0 / p);

               if (strength < 0.0) {
                   strength = 0.0;
               }

               strengthSum += strength;
           }
       }

       if (binderCount == 0) {
           // No alleles passed the binder cutoff
           return 0.0;
       }

       double breadth = (double) binderCount / (double) totalAlleles;
       double meanStrength = strengthSum / (double) binderCount;
       return breadth * meanStrength;
   }

   /**
    * Convenience overload with a default binder cutoff of 2%,
    * which is a common threshold for predicted binders.
    */
   public static double computeEpitopeScore(List<Sub> subs) {
       return computeEpitopeScore(subs, 2.0);
   }
}