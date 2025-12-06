package flyhigh;

import java.util.ArrayList;
import java.util.List;

/**
 * Nicotine
 *
 * Wrapper object that bundles together:
 *  - A distinct arrangement (da1 / da2).
 *  - A human-readable label for this region (e.g. "Nucleocapsid, Residue:400–600").
 *  - Motif locations (RL objects) in aligned coordinates.
 *  - Site locations (phosphorylation or other sites) in aligned coordinates.
 *  - The absolute aligned start/end of the region of interest (absPos_s / absPos_e).
 *
 * This object is mainly used as a convenient container for plotting in LociPlotter,
 * so you can pass around “N region in da1” or “NSP3 region in da2” as a single unit.
 */
public class Nicotine {

    /** Distinct arrangement identifier ("da1" or "da2"). */
    String da;

    /** Label for this Nicotine instance (e.g. "Nucleocapsid,Residue:300-500"). */
    String name;

    /** List of motif locations in this region (typically kinase motifs), in aligned coords. */
    List<RL> motifLocations;

    /** List of site locations (e.g. phosphorylation sites) in this region, in aligned coords. */
    List<Site> siteLocations;

    /**
     * Number of species / sequences in this arrangement.
     *  - For da1: 7
     *  - For da2: 8
     * This is used when aggregating across clades.
     */
    int da_size;

    /** Absolute start position (in aligned coordinates) of this Nicotine's region. */
    final int absPos_s;

    /** Absolute end position (in aligned coordinates) of this Nicotine's region. */
    final int absPos_e;

    /**
     * Half the width of the region (absPos_e - absPos_s)/2.
     * Useful for defining symmetric windows around the center.
     */
    int buffer;

    /**
     * Center of the region in aligned coordinates:
     *   center = (absPos_s + absPos_e) / 2.
     */
    int center;

    /**
     * A random identifier for debugging / distinguishing instances.
     * Printed in the constructor so you can track which Nicotine is which.
     */
    final double random;

    /**
     * Construct a Nicotine object representing one region in one arrangement.
     *
     * @param da              distinct arrangement ID ("da1" or "da2").
     * @param name            label to use in plots/legends.
     * @param motifLocations  motif positions (RL) in this region.
     * @param siteLocations   site positions (Site) in this region.
     * @param absPos_s        absolute aligned start position of the region.
     * @param absPos_e        absolute aligned end position of the region.
     */
    public Nicotine(String da,
                    String name,
                    List<RL> motifLocations,
                    List<Site> siteLocations,
                    int absPos_s,
                    int absPos_e) {

        this.da             = da;
        this.name           = name;
        this.motifLocations = motifLocations;
        this.siteLocations  = siteLocations;

        System.out.println("Input motifs to Nicotine of " + this.motifLocations.size());
        System.out.println("pos " + absPos_s + ":" + absPos_e);
        for (RL real : motifLocations) {
            System.out.println("real at " + real.midpoint);
        }

        // da_size depends on which clade/arrangement this Nicotine represents.
        if (da.equals("da1")) {
            this.da_size = 7;
        } else {
            this.da_size = 8;
        }

        this.absPos_s = absPos_s;
        this.absPos_e = absPos_e;

        // Derive buffer and center for this region.
        this.buffer = (this.absPos_e - this.absPos_s) / 2;
        this.center = (this.absPos_e + this.absPos_s) / 2;

        // Assign a random ID for debugging.
        this.random = Math.random();
        System.out.println("My name is " + random + " and I have ;");
    }
}
