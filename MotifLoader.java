package flyhigh;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

/**
 * Loader and convenience utilities for motif data produced by Scansite.
 *
 * Responsibilities:
 *  - Load motif TSV files for all species from a directory.
 *  - Filter to a whitelist of species for a given arrangement (da1 / da2 etc.).
 *  - Convert motif calls into Motif / MotifLock / RL instances used in plotting.
 *  - Provide a main() that runs a concrete analysis/plot for specific NSP3/N regions.
 */
public class MotifLoader {

    // Project root for this analysis. Used by several helper methods in this class.
    static String root = "/Users/quentinflattmann/Desktop/Covid/";

    /**
     * Load all motif TSV files in a directory for a given arrangement "da".
     *
     * @param motifDir directory containing motif .tsv files.
     * @param da       arrangement identifier ("da1", "da2") used to select whitelist.
     * @return map from species name (header key) -> list of Motif objects.
     *
     * Notes:
     *  - Only species whose filenames contain entries from names_da.csv are kept.
     *  - Filenames that contain spaces or lack the '$' marker are skipped.
     */
    public static Map<String, List<Motif>> loadAll(String motifDir, String da) throws IOException {
        Map<String, List<Motif>> map = new HashMap<>();

        // White list of canonical species names for this arrangement.
        List<String> whiteList =
                variousfunctions.loadLines(Path.of(root + "names_" + da + ".csv"));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(motifDir), "*.tsv")) {
            for (Path file : stream) {
                String speciesName = stripExtension(file.getFileName().toString());

                // Must be in whitelist (substring match).
                boolean foundSelf = false;
                for (String wl : whiteList) {
                    if (speciesName.contains(wl)) {
                        foundSelf = true;
                        break;
                    }
                }
                if (!foundSelf) continue;

                // Skip files that don't use $ and have spaces; these likely aren't the canonical headers.
                if (!speciesName.contains("$")) continue;
                if (speciesName.contains(" ")) continue;

                speciesName = speciesName.replace("tsv", "").replace("\\.", "");

                List<Motif> motifs = loadFile(file);
                map.put(speciesName, motifs);
            }
        }

        return map;
    }

    /**
     * Load a single Scansite motif TSV file into a list of Motif objects.
     * The file is assumed to have a header line followed by tab-delimited data.
     *
     * Expected columns (0-based indices):
     *   0 motif_gene_symbol
     *   1 motif_uniprot_entry_name
     *   2 motif_name
     *   3 motif_group
     *   4 score
     *   5 percentile
     *   6 protein
     *   7 site
     *   8 site_sequence
     *   9 surface_accessibility_value
     */
    private static List<Motif> loadFile(Path file) throws IOException {
        List<Motif> list = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(file)) {
            String header = br.readLine(); // skip header
            if (header == null) return list;

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Prefer true TSV; fall back to whitespace split if tabs absent.
                String[] t;
                if (line.indexOf('\t') >= 0) {
                    t = line.split("\t");
                } else {
                    t = line.trim().split("\\s+");
                }

                // Trim each field.
                for (int i = 0; i < t.length; i++) {
                    t[i] = t[i].trim();
                }

                // Require at least 10 columns; skip malformed lines.
                if (t.length < 10) {
                    System.err.println("WARNING: Skipping malformed line in " + file + ": " + line);
                    continue;
                }

                try {
                    Motif m = new Motif(
                            t[0].toLowerCase(),            // motif_gene_symbol
                            t[1].toLowerCase(),            // motif_uniprot_entry_name
                            t[2],                          // motif_name
                            t[3],                          // motif_group
                            Double.parseDouble(t[4]),      // score
                            Double.parseDouble(t[5]),      // percentile
                            t[6],                          // protein
                            t[7],                          // site
                            t[8],                          // site_sequence
                            Double.parseDouble(t[9])       // surface_accessibility_value
                    );
                    list.add(m);
                } catch (NumberFormatException e) {
                    System.err.println("WARNING: numeric parse error in " + file + " line:");
                    System.err.println(line);
                    e.printStackTrace();
                }
            }
        }

        return list;
    }

    /** Strip the file extension from a filename. */
    private static String stripExtension(String name) {
        int idx = name.lastIndexOf('.');
        return (idx == -1 ? name : name.substring(0, idx));
    }

    /**
     * Main driver for motif / phosphorylation / interaction visualization.
     *
     * What this run does (with current hard-coded values):
     *  1. Loads residue–residue interaction CSVs for SARS-CoV-2 and MHV_JHM.
     *  2. Chooses a region of interest (ROI) in N and finds a best-matching region in NSP3.
     *  3. Gets motif sites and phosphorylation site statistics for those regions in da1/da2.
     *  4. Builds Nicotine objects (encapsulating motif + phospho data).
     *  5. Calls LociPlotter.plotTwoNicotines(...) to generate a composite PNG.
     */
    public static void main(String[] args) throws Exception {

        // ---------------------------------------------------------------------
        // Example: immunogenicity compilation (currently commented out).
        // ---------------------------------------------------------------------
        // int interval = 10;
        // int dist = 500;
        // String outImmuneData = root + "immune.csv";
        // String outPath2      = root + "immune2_0.csv";
        // compileImmunogenicityData(outImmuneData, outPath2, root, "nucleocapsid", interval, dist);
        // System.exit(1);

        // ---------------------------------------------------------------------
        // Example: running phosphorylation/gap analysis for multiple proteins.
        // ---------------------------------------------------------------------
        // Main.setNames(root);
        // Main.findPhosphorylationAndGaps(root + "nucleocapsid_sites_da1.csv", root + "Nucleocapsid-Phosph.csv", root + "nucleocapsid_alignedU.fasta", "da1");
        // Main.findPhosphorylationAndGaps(root + "nucleocapsid_sites_da2.csv", root + "Nucleocapsid-Phosph.csv", root + "nucleocapsid_alignedU.fasta", "da2");
        // Main.findPhosphorylationAndGaps(root + "NSP3_sites_da2.csv",        root + "NSP3-Phosph.csv",       root + "NSP3_alignedU.fasta",        "da2");
        // ...

        // ---------------------------------------------------------------------
        // Interaction + motif visualization for a single N–NSP3 pair.
        // ---------------------------------------------------------------------
        List<ResInteraction> riList  = ResInteraction.fromCsv(root + "interactions_sars2.csv");
        List<ResInteraction> riList2 = ResInteraction.fromCsv(root + "interactions_mhvjhm.csv");

        String nameOfProtein  = "Nucleocapsid";
        String nameOfProtein2 = "NSP3";

        // Region of interest (in N, in aligned coordinates).
        int roi  = 400;
        int roi2 = 600;  // candidate ROI in NSP3 (used for labeling)

        String da1 = "da1";
        String da2 = "da2";

        int buff = 100;
        int[] r1 = { roi - buff, roi + buff };

        // Let ResInteraction find the NSP3 range best matching the N ROI.
        int[] r2 = ResInteraction.bestMatchingRange(riList, r1);

        // Convert relative ROI to "absolute aligned" positions. Currently, r1/r2
        // are already aligned, so we just pass through.
        int[] absPos1 = r1;
        int[] absPos2 = r2;

        String outName = nameOfProtein + " at residue " + roi + " vs " +
                         nameOfProtein2 + " at residue " + roi2;

        // Load motif sites in each arrangement (da1 / da2) and region.
        List<RL> motifSites  =
                getMotifSites(da1, nameOfProtein,  outName, absPos1[0], absPos1[1]);
        List<RL> motifSites2 =
                getMotifSites(da1, nameOfProtein2, outName, absPos2[0], absPos2[1]);

        String sitesCSV      = root + nameOfProtein  + "_sites_" + da1 + ".csv";
        String sitesCSV2     = root + nameOfProtein2 + "_sites_" + da1 + ".csv";
        String sitesCSV_b    = root + nameOfProtein  + "_sites_" + da2 + ".csv";
        String sitesCSV2_b   = root + nameOfProtein2 + "_sites_" + da2 + ".csv";

        String outPNG        = "/Users/quentinflattmann/Desktop/" + nameOfProtein + "_DA1.png";

        // Nicotine encapsulates motif + phospho data for a single region.
        Nicotine n1 = new Nicotine(
                da1,
                nameOfProtein + ",Residue:" + r1[0] + "-" + r1[1],
                motifSites,
                Site.parseSites(sitesCSV),
                absPos1[0],
                absPos1[1]
        );

        Nicotine n2 = new Nicotine(
                da1,
                nameOfProtein2 + ",Residue:" + r2[0] + "-" + r2[1],
                motifSites2,
                Site.parseSites(sitesCSV2),
                absPos2[0],
                absPos2[1]
        );

        List<RL> motifSites_b  =
                getMotifSites(da2, nameOfProtein,  outName, absPos1[0], absPos1[1]);
        List<RL> motifSites2_b =
                getMotifSites(da2, nameOfProtein2, outName, absPos2[0], absPos2[1]);

        Nicotine n1_b = new Nicotine(
                da2,
                nameOfProtein + ",Residue:" + r1[0] + "-" + r1[1],
                motifSites_b,
                Site.parseSites(sitesCSV_b),
                absPos1[0],
                absPos1[1]
        );

        Nicotine n2_b = new Nicotine(
                da2,
                nameOfProtein2 + ",Residue:" + r2[0] + "-" + r2[1],
                motifSites2_b,
                Site.parseSites(sitesCSV2_b),
                absPos2[0],
                absPos2[1]
        );

        // Final figure: two Nicotine objects (alpha vs beta) for N and NSP3.
        LociPlotter.plotTwoNicotines(
                n1, n2,
                n1_b, n2_b,
                "/Users/quentinflattmann/Desktop/nicotine_pair.png",
                "Nucleocapsid Primary C-Terminal Region & NSP3 Region With Interaction In Alpha & Beta Clades ",
                riList,
                riList2
        );

        // Example of plotting just bars (kept for reference).
        // LociPlotter.plotBarsStackedColored(motifSites, Site.parseSites(sitesCSV), outPNG, outName);
    }

    /**
     * Compile immunogenicity data across all species and intervals for a given protein.
     *
     *  - Reads per-species CSV output from IEDB's TepiTool in the "Immune" subfolder.
     *  - Maps unaligned start/end positions through an alignment to get aligned coordinates.
     *  - Assigns each epitope to an interval Pos object and stores the Sub (epitope) there.
     *  - Computes summary stats per interval and writes:
     *      1. Candlestick summary CSV (outPath).
     *      2. Long format "x,score" CSV (outPath2) for scatter plotting.
     */
    public static void compileImmunogenicityData(String outPath,
                                                 String outPath2,
                                                 String root,
                                                 String proteinName,
                                                 int interval,
                                                 int longestDistance) throws IOException {

        String immuneFolder = root + "/Immune";

        List<String> seqImmunes = variousfunctions.listFilePathsInDirectory(immuneFolder);
        List<Pos> positions = Pos.buildIntervals(longestDistance, interval);

        for (String si : seqImmunes) {
            List<String> raw = variousfunctions.loadLines(Path.of(si));
            String[] parts  = stripExtension(si).split("/");
            String name     = parts[parts.length - 1].replace(".csv", "");

            Map<Integer, Integer> mappedPos = unalignedToAlignedMap(proteinName, name);

            // Remove header row
            raw.remove(0);

            for (String r : raw) {
                String[] pLine = r.split(",");
                int s = mappedPos.get(Integer.parseInt(pLine[1]));
                int e = mappedPos.get(Integer.parseInt(pLine[2]));

                double mp = (e + s * 1.0) / 2.0;
                double perc = Double.parseDouble(pLine[4]);

                // Find nearest Pos interval to this midpoint.
                Pos bestPos = null;
                double closest = 1000;
                for (Pos pos : positions) {
                    double dist = Math.abs(mp - pos.start);
                    if (dist < closest) {
                        closest = dist;
                        bestPos = pos;
                    }
                }

                Sub ps = new Sub(s, e, perc, pLine[5], pLine[3], name);
                bestPos.subs.add(ps);
            }
        }

        List<String> csvLines  = new ArrayList<>();
        List<String> otherForm = new ArrayList<>();

        csvLines.add(CandlestickRow.CSV_HEADER);
        otherForm.add("x,score");

        for (Pos p : positions) {
            p.computeSubStats();

            if (!p.scores.isEmpty()) {
                CandlestickRow csr = new CandlestickRow(p.mp(), p.scores);
                csvLines.add(csr.toCsvLine());

                for (double score : p.scores) {
                    otherForm.add(p.mp() + "," + score);
                }
            }
        }

        variousfunctions.writeLinesToCsv(csvLines, outPath);
        variousfunctions.writeLinesToCsv(otherForm, outPath2);
    }

    /**
     * Build a mapping from unaligned amino-acid positions (1-based) to aligned
     * positions (1-based) for a specific species and protein.
     *
     * Uses the alignedU FASTA for the given protein and species, treating 'U'
     * as a gap character. Every non-gap character increments the "relative"
     * index; every character increments the aligned index.
     */
    public static Map<Integer, Integer> unalignedToAlignedMap(String proteinName,
                                                              String specName) throws IOException {

        Map<Integer, Integer> outMap = new HashMap<>();

        String fastaAlignedRAW = root + "/" + proteinName + "_alignedU.fasta";

        Map<String, String> alignedMap = FastaIO.readFastaToMap(fastaAlignedRAW, false);
        String alignedU = alignedMap.get(specName);

        List<Character> alignedChars = toCharList(alignedU);

        int relLock = 0;  // position in unaligned sequence
        int absLock = 0;  // position in aligned sequence

        for (int i = 0; i < alignedChars.size(); i++) {
            absLock += 1;
            char c = alignedChars.get(i);

            if (c != 'U') {
                relLock += 1;
                outMap.put(relLock, absLock);
            }
        }

        return outMap;
    }

    /**
     * Map region-of-interest (ROI) coordinates (roiStart, roiEnd) from ungapped
     * reference to MSA coordinates.
     *
     * @param da         arrangement identifier ("da1" or "da2").
     * @param nameOfProtein protein name e.g., "Nucleocapsid".
     * @param roiStart   start in ungapped reference coordinates (1-based).
     * @param roiEnd     end in ungapped reference coordinates (1-based).
     * @return {absStart, absEnd} in aligned coordinates (1-based).
     */
    public static int[] getAbsolutePosition(String da,
                                            String nameOfProtein,
                                            int roiStart,
                                            int roiEnd) throws IOException {

        String fastaAligned = root + nameOfProtein + "_aligned.fasta";

        // Different reference per arrangement.
        String reference = "$HCoV_SARS2";
        if ("da2".equals(da)) {
            reference = "$MHV_JHM";
        }

        Map<String, String> seqs = FastaIO.readFastaToMap(fastaAligned, false);
        System.out.println("Available keys: " + seqs.keySet());

        String refSeq = seqs.get(reference);
        if (refSeq == null) {
            throw new IllegalArgumentException("Reference sequence not found for key: " + reference);
        }

        int absStart = 0;
        int absEnd   = 0;

        List<Character> chars = toCharList(refSeq);
        int absIter = 0;
        int relIter = 0;

        for (char c : chars) {
            absIter++;

            if (c != '-') {
                relIter++;

                if (relIter == roiStart) {
                    absStart = absIter;
                }
                if (relIter == roiEnd) {
                    absEnd = absIter;
                }
            }
        }

        if (absStart == 0 || absEnd == 0) {
            throw new IllegalArgumentException(
                    "roiStart/roiEnd are out of range for reference sequence: " +
                    "roiStart=" + roiStart + ", roiEnd=" + roiEnd +
                    ", ref length (non-gap)=" + relIter);
        }

        return new int[]{absStart, absEnd};
    }

    /**
     * Get all motif sites for the given arrangement, protein, and aligned ROI.
     *
     * Workflow:
     *  1. Load motif TSVs (loadAll).
     *  2. Load aligned FASTA for this protein.
     *  3. For each motif, compute its aligned site (Motif.getAlignedSite).
     *  4. Wrap in MotifLock (site orig, site aligned, species, motif).
     *  5. Convert to RL objects (midpoint and metadata), then filter by ROI.
     */
    public static List<RL> getMotifSites(String da,
                                         String nameOfProtein,
                                         String outName,
                                         int absPos_s,
                                         int absPos_e) throws IOException {

        Map<String, List<MotifLock>> motifsBySpec = new HashMap<>();

        String fastaAligned = root + nameOfProtein + "_aligned.fasta";
        String motifDir     = root + nameOfProtein + "-Motifs";

        String reference = "$HCoV_SARS2";
        if (da.equals("da2")) {
            reference = "$MHV_JHM";
        }

        Map<String, List<Motif>> data = loadAll(motifDir, da);
        Map<String, String> seqs = FastaIO.readFastaToMap(fastaAligned, false);
        String refSeq = seqs.get(reference);

        System.out.println("ON DA " + da + " for " + nameOfProtein);

        // Compute aligned site positions for each motif in each species.
        for (String specKey : data.keySet()) {
            for (Motif m : data.get(specKey)) {
                String relSeq = null;
                for (String k2 : seqs.keySet()) {
                    if (k2.equalsIgnoreCase(specKey)) {
                        relSeq = seqs.get(k2);
                        break;
                    }
                }
                if (relSeq == null) {
                    System.out.println("RELEVANT SEQ NULL, exiting...");
                    System.exit(1);
                }

                m.getAlignedSite(relSeq);
                System.out.println(m.motifGroup + " reassigned " + m.site + " -> " + m.siteAligned);

                MotifLock ml = new MotifLock(m.getSiteOrig(), m.siteAligned, specKey, m);

                List<MotifLock> mlRel = motifsBySpec.get(specKey);
                if (mlRel == null) {
                    mlRel = new ArrayList<>();
                    motifsBySpec.put(specKey, mlRel);
                }
                mlRel.add(ml);
            }
        }

        // motifsBySpec: species -> list of MotifLocks
        Map<String, List<RL>> rangeMap = new HashMap<>();
        List<RL> rangesList = new ArrayList<>();

        for (String specKey : motifsBySpec.keySet()) {
            List<RL> ranges = new ArrayList<>();
            List<MotifLock> locks = motifsBySpec.get(specKey);

            for (MotifLock ml : locks) {
                RL real = new RL(ml.siteAligned, ml.motif.motifGroup, specKey.replace("$", ""));
                ranges.add(real);
            }

            rangeMap.put(specKey, ranges);
            rangesList.addAll(ranges);
        }

        System.out.println("Filtering to " + absPos_s + ":" + absPos_e);

        // Keep only those RL entries whose midpoint falls within [absPos_s, absPos_e].
        List<RL> out = RL.filterByMidpointRange(rangesList, absPos_s, absPos_e);
        return out;
    }

    /** Round down an integer to the nearest even number. */
    public static int roundDownTo2(int x) {
        return x - (x % 2);
    }

    /** Convert a String to a List<Character>. */
    public static List<Character> toCharList(String s) {
        List<Character> list = new ArrayList<>(s.length());
        for (int i = 0; i < s.length(); i++) {
            list.add(s.charAt(i));
        }
        return list;
    }

    /** Extract all unique motifGroup values from a motif map. */
    public static Set<String> extractUniqueMotifGroups(Map<String, List<Motif>> data) {
        Set<String> out = new HashSet<>();
        for (List<Motif> motifs : data.values()) {
            for (Motif m : motifs) {
                if (m.motifGroup != null && !m.motifGroup.isBlank()) {
                    out.add(m.motifGroup.trim());
                }
            }
        }
        return out;
    }

    /**
     * Filter motifs to only those with a given motifGroup, preserving the
     * species->motifs structure.
     */
    public static Map<String, List<Motif>> filterByMotifGroup(
            Map<String, List<Motif>> data,
            String motifGroup) {

        Map<String, List<Motif>> out = new HashMap<>();

        for (Map.Entry<String, List<Motif>> entry : data.entrySet()) {
            String species = entry.getKey();
            List<Motif> motifs = entry.getValue();

            if (motifs == null || motifs.isEmpty()) {
                continue;
            }

            List<Motif> filtered = new ArrayList<>();
            for (Motif m : motifs) {
                if (m.motifGroup != null &&
                    m.motifGroup.equalsIgnoreCase(motifGroup)) {
                    filtered.add(m);
                }
            }

            if (!filtered.isEmpty()) {
                out.put(species, filtered);
            }
        }
        return out;
    }
}
