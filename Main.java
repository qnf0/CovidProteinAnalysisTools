package flyhigh;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import flyhigh.ExpressionProfile.SingleProfile;
import flyhigh.OverlapContacts.Interaction;

/**
 * Entry point and utility methods for the betacoronavirus coevolution project.
 *
 * Responsibilities:
 *  - Parse NSP3–Nucleocapsid interaction CSVs (HDock output via OverlapContacts).
 *  - Map interacting residues from unaligned sequences into aligned MSA coordinates.
 *  - Compute cross-species phosphorylation vs. gap statistics from NetPhos + aligned FASTA.
 *  - Normalize species naming across FASTA, motif TSVs, and phospho CSVs.
 *
 * NOTE: This class currently mixes "script" style code in main() with reusable utilities.
 *       For production, you may eventually want to split into small dedicated runners.
 */
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        // Root directories for this project (hard-coded for now).
        String root = "/Users/quentinflattmann/Desktop/Covid";
        String rootOverlaps = "/Users/quentinflattmann/Desktop/OVERLAPS_MHVJHM/";

        /*
         * Example: normalize species names across FASTA / motif / phospho files.
         * Uncomment to run that pre-processing step.
         */
        // setNames(root);

        /*
         * Example: parse nucleocapsid–NSP3 docking overlaps and map them to
         * aligned positions for a given species (here: MHV_JHM).
         */
        List<Interaction> interactionsNsp3 =
                OverlapContacts.parse(Path.of(rootOverlaps + "overlaps_nucleocapsid-NSP3.csv"));

        Map<String, String> mappedN_NSP3 =
                FastaIO.readFastaToMap(rootOverlaps + "/N-NSP3.fasta", false);

        String species = "$MHV_JHM";

        // Unaligned and aligned sequences for nucleocapsid and NSP3
        Map<String, String> nucleocapsidSeqsUnaligned =
                FastaIO.readFastaToMap(root + "/nucleocapsid_unique.fasta", false);
        Map<String, String> nucleocapsidSeqsAligned =
                FastaIO.readFastaToMap(root + "/nucleocapsid_aligned.fasta", false);
        Map<String, String> nsp3SeqsUnaligned =
                FastaIO.readFastaToMap(root + "/NSP3_unique.fasta", false);
        Map<String, String> nsp3SeqsAligned =
                FastaIO.readFastaToMap(root + "/sars2_nsp3_aligned_chains.fasta", false);

        String nsp3_unaligned = nsp3SeqsUnaligned.get(species);
        String nsp3_aligned   = nsp3SeqsAligned.get(species);
        String nucleocapsid_unaligned = nucleocapsidSeqsUnaligned.get(species);
        String nucleocapsid_aligned   = nucleocapsidSeqsAligned.get(species);

        List<Inter> formattedInteractions = new ArrayList<>();

        System.out.println("interactions ");
        for (Interaction ii : interactionsNsp3) {
            OverlapContacts.InteractionClassification c =
                    OverlapContacts.classifyInteractionProteins(ii, mappedN_NSP3);

            // Will hold aligned positions (N, NSP3); currently only N is used.
            int[] alignedPair = { -1, -1 };

            if (!c.atom1Protein.equals("UNKNOWN")) {

                // atom1Index is in unaligned nucleocapsid coordinates (1-based).
                int unalignedPos = Integer.parseInt(c.atom1Index);
                if (unalignedPos == 0) {
                    unalignedPos = 1;
                }

                // Map nucleocapsid residue to aligned MSA position.
                int alignedPos =
                        Inter.findAlignedPosition(nucleocapsid_unaligned, nucleocapsid_aligned, unalignedPos);
                alignedPair[0] = alignedPos;

                // Get aligned sequence for the NSP3 header we classified to.
                String nsp3AlignedSpec = nsp3SeqsAligned.get(c.atom2Header);

                // Map NSP3 residue to aligned coordinates.
                int alignedPos2 =
                        Inter.findAlignedPosition(c.atom2Seq, nsp3AlignedSpec, Integer.parseInt(c.atom2Index) + 1);

                System.out.println(
                        OverlapContacts.aa1(ii.atom1.resName) + ":" + alignedPos + "," +
                        OverlapContacts.aa1(ii.atom2.resName) + ":" + alignedPos2 + "," +
                        ii.overlap + "," + ii.distance);

                // If desired, you could construct Inter objects for downstream plotting:
                // Inter interactionNew = new Inter(
                //         alignedPos, alignedPos2, "N", "NSP3",
                //         OverlapContacts.aa1(ii.atom1.resName),
                //         OverlapContacts.aa1(ii.atom2.resName),
                //         ii.overlap, ii.distance);
                // formattedInteractions.add(interactionNew);

            } else {
                System.out.println("unknown");
            }
        }

        // Early exit: the block above is typically run as a stand-alone script.
        System.exit(1);

        /*
         * Example: compute phosphorylation vs. gap percentages for a given protein.
         * (This code is unreachable due to System.exit above; remove that if you
         *  want to run the analysis below instead.)
         */
        String outDir     = "/users/quentinflattmann/Desktop/Covid/nucleocapsid_sites_da2.csv";
        String phosphPath = "/Users/quentinflattmann/Desktop/Covid/Nucleocapsid-phosph.csv";
        String fastaPath  = "/Users/quentinflattmann/Desktop/Covid/nucleocapsid_alignedU.fasta";

        findPhosphorylationAndGaps(outDir, phosphPath, fastaPath, "da2");

        System.exit(1);
    }

    /**
     * Sanitize a string so it can be safely embedded into a Latin-1 / WinAnsi
     * text context (e.g. PDFBox text streams).
     *
     *  - Drops control characters U+0000–U+001F and U+007F.
     *  - Keeps characters up to 0xFF as-is.
     *  - Replaces anything above 0xFF with '?'.
     */
    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            // Remove control characters
            if (ch <= 0x1F || ch == 0x7F) {
                continue;
            }

            if (ch <= 0xFF) {
                sb.append(ch);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    /**
     * Wrap a long string every 100 characters with newline breaks.
     * Useful for embedding long sequences in a text file.
     */
    public static String wrapEvery100(String input) {
        StringBuilder sb = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length; i += 100) {
            int end = Math.min(i + 100, length);
            sb.append(input, i, end);
            if (end < length) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Example helper for constructing a new text file where the "insertText"
     * (wrapped every 100 chars) is spliced between a fixed "start" and "end"
     * template.
     *
     * Currently this does not actually edit a PDF; it builds a new text file:
     *   chem_notes_start.txt + wrapped insert + chem_notes_end.txt
     */
    public static void appendInPdf(String path,
                                   String target,
                                   String insertText,
                                   String outputPath) throws IOException {

        String wrapped = wrapEvery100(insertText);

        List<String> firstHalf =
                variousfunctions.loadLines(Path.of("/Users/quentinflattmann/Desktop/chem_notes_start.txt"));
        firstHalf.add("Alternatively***");
        List<String> secondHalf =
                variousfunctions.loadLines(Path.of("/Users/quentinflattmann/Desktop/chem_notes_end.txt"));

        System.out.println(firstHalf.size() + "," + "," + secondHalf.size());

        firstHalf.add(wrapped);
        firstHalf.addAll(secondHalf);

        variousfunctions.writeLinesToCsv(firstHalf,
                "/Users/quentinflattmann/Desktop/chem_notes.txt");
    }

    /**
     * Compute, for each alignment position:
     *  - How many predicted phosphorylation sites land at that position across a whitelist of species.
     *  - What fraction of sequences contain a gap (encoded as 'U') at that position.
     *
     * Inputs:
     *  @param outDir     CSV path for output (site,gapPerc,phosphSites).
     *  @param phosphPath NetPhos output in a loose TSV/space format (one file with all species).
     *  @param fastaPath  aligned protein sequences with gaps encoded as 'U'.
     *  @param da         clade/arrangement identifier (used to load names_da.csv whitelist).
     */
    public static void findPhosphorylationAndGaps(String outDir,
                                                  String phosphPath,
                                                  String fastaPath,
                                                  String da) throws IOException {

        Map<Integer, Integer> sums = new HashMap<>();       // site -> phospho count
        Map<Integer, Integer> gapSums = new HashMap<>();    // site -> gap count
        Map<Integer, Double> gapPercs = new HashMap<>();    // site -> fraction with gap

        List<String> rawPhosph = variousfunctions.loadLines(Path.of(phosphPath));
        List<String> whiteListed =
                variousfunctions.loadLines(Path.of("/Users/quentinflattmann/Desktop/Covid/names_" + da + ".csv"));

        // Collect integer site positions for whitelist species only
        List<Integer> phosphSites = new ArrayList<>();
        for (String line : rawPhosph) {
            System.out.println(line);

            if (line.contains("#")) {
                continue; // skip NetPhos comments
            }

            Integer site = getStartInt(line);
            String name  = getFirstField(line);

            if (site != null && whiteListed.contains(name)) {
                phosphSites.add(site);
            }
        }

        // Initialize counts up to a generous max length (15000).
        for (int x = 1; x < 15000; x++) {
            sums.put(x, 0);
            gapSums.put(x, 0);
        }

        // Count phospho hits per site.
        for (int r : phosphSites) {
            sums.put(r, sums.get(r) + 1);
        }

        // Load aligned sequences and filter to whitelist.
        Map<String, String> sequencesRaw = FastaIO.readFastaToMap(fastaPath, false);
        Map<String, String> sequences = new HashMap<>();

        for (String k : sequencesRaw.keySet()) {
            if (whiteListed.contains(k)) {
                sequences.put(k, sequencesRaw.get(k));
            }
        }

        // Convert sequences into lists of single-character strings.
        Map<String, List<String>> alignment = new HashMap<>();
        for (Map.Entry<String, String> e : sequences.entrySet()) {
            List<String> chars = new ArrayList<>();
            for (char c : e.getValue().toCharArray()) {
                chars.add(String.valueOf(c));
            }
            alignment.put(e.getKey(), chars);
        }

        int totalSeq = alignment.size();
        System.out.println("Total seq " + totalSeq);

        // Assume all aligned sequences are the same length; grab from the first.
        int len = 0;
        for (List<String> seq : alignment.values()) {
            len = seq.size();
            System.out.println("Length set to " + len);
            break;
        }

        // For each alignment column, compute how many sequences have a gap ('U').
        for (int i = 0; i < len; i++) {
            int gapCount = 0;

            for (String k : alignment.keySet()) {
                String c = alignment.get(k).get(i);
                if (c.equals("U")) { // treat 'U' as gap
                    gapCount++;
                }
            }

            int site = i + 1; // convert to 1-based
            gapSums.put(site, gapCount);
            double gapPerc = gapCount * 1.0 / totalSeq;
            gapPercs.put(site, gapPerc);
        }

        // Build CSV output.
        List<String> csvLines = new ArrayList<>();
        csvLines.add("site,gapPerc,phosphSites");

        for (int site = 1; site < 15000; site++) {
            int phosphoCount = sums.getOrDefault(site, 0);
            double gapPerc   = gapPercs.getOrDefault(site, 0.0);
            csvLines.add(site + "," + gapPerc + "," + phosphoCount);
        }

        variousfunctions.writeLinesToCsv(csvLines, outDir);
    }

    /** Format a double with explicit sign and two decimal places (e.g. "+0.45", "-1.20", "0.00"). */
    public static String formatSigned(double value) {
        String sign = (value > 0) ? "+" : "";
        return String.format(Locale.ROOT, "%s%.2f", sign, value);
    }

    /** Midpoint of two ints as int, done via long to avoid overflow. */
    public static int mp(int start, int end) {
        return (int) (((long) start + (long) end) / 2);
    }

    /**
     * Normalize species names across FASTA, motif TSVs, and phospho CSVs.
     *
     * 1. Loads "names.csv" (canonical species names), prefixes each with an index (0:name).
     * 2. Scans all FASTA files, TSV motif files, and phospho CSVs under `path` to collect
     *    all "raw" species identifiers.
     * 3. Uses ChatClient.askAI(...) to map each raw identifier to an index into names.csv.
     * 4. Rewrites:
     *      - FASTA headers to canonical names.
     *      - TSV filenames to canonical names.
     *      - Phospho CSV lines replacing raw species names with canonical names.
     *
     * WARNING: this is destructive ( overwrites files in place ).
     */
    public static void setNames(String path) throws IOException {

        List<String> bestNames = new ArrayList<>();
        List<String> rawNames  = variousfunctions.loadLines(Path.of(path + "/names.csv"));

        int j = 0;
        for (String rn : rawNames) {
            bestNames.add(j + ":" + rn);
            j++;
        }

        List<String> contained = variousfunctions.listFilePathsInDirectory(path);
        List<String> csvPhosphs = new ArrayList<>();
        Set<String> namesAll = new HashSet<>();

        // Collect species names from FASTA headers and phospho CSVs.
        for (String c : contained) {
            System.out.println(c);

            if (c.contains("fasta")) {
                Map<String, String> fastasMap = FastaIO.readFastaToMap(c, false);
                namesAll.addAll(fastasMap.keySet());
            }
            if (c.contains("csv") && c.contains("-Phosph")) {
                csvPhosphs.add(c);
                System.out.println("added csv path " + c);
            }
        }

        // Collect names from motif TSV files in subfolders.
        List<String> containedFolders = variousfunctions.listFolderPaths(path);
        for (String cf : containedFolders) {
            List<String> names = variousfunctions.listFileNames(cf);
            for (String n : names) {
                if (n.contains(".tsv")) {
                    n = n.replace(".tsv", "");
                    namesAll.add(n);
                }
            }
        }

        // Collect species from phospho CSVs.
        for (String cf : csvPhosphs) {
            List<String> raw = variousfunctions.loadLines(Path.of(cf));
            for (String r : raw) {
                if (r.contains("#")) {
                    continue;
                }
                String specName = getFirstField(r);
                namesAll.add(specName);
            }
        }

        // Map "bad" / inconsistent names to canonical names, using ChatClient for disambiguation.
        Map<String, String> badToGood = new HashMap<>();
        for (String n : namesAll) {
            String query =
                    "You come across this term for a specific species : " + n +
                    " and know its one of these " + bestNames +
                    " , please return a response of which you think is the match and only return that name's number (first number). " +
                    "Return no other text but the number it matches to.";

            String curr = badToGood.get(n);
            if (curr == null) {
                String resp = ChatClient.askAI(query);
                int respInt = Integer.parseInt(resp);
                System.out.println(n + " matched to " + rawNames.get(respInt));
                badToGood.put(n, rawNames.get(respInt));
            } else {
                System.out.println(n + " PREmatched to " + curr);
            }
        }

        System.out.println("Renamed->");
        for (String k1 : badToGood.keySet()) {
            System.out.println(k1 + "," + badToGood.get(k1));
        }

        /*
         * Rewrite FASTA files with canonical headers.
         */
        for (String c : contained) {
            if (c.contains("fasta")) {
                Map<String, String> fastasMap = FastaIO.readFastaToMap(c, false);
                Map<String, String> newMap = new HashMap<>();

                for (String oldHeader : fastasMap.keySet()) {
                    String key = "";
                    for (String btg : badToGood.keySet()) {
                        if (btg.equalsIgnoreCase(oldHeader)) {
                            key = btg;
                            break;
                        }
                    }
                    String goodName = badToGood.get(key);
                    newMap.put(goodName, fastasMap.get(oldHeader));
                }

                FastaIO.writeFromMap(newMap, c);
                System.out.println("Rewrote map to " + c);
            }
        }

        /*
         * Rewrite motif TSV filenames to canonical species names.
         */
        for (String cf : containedFolders) {
            List<String> names = variousfunctions.listFileNames(cf);
            List<String> namesFull = variousfunctions.listFilePathsInDirectory(cf);

            for (String n : names) {
                if (!n.contains(".tsv")) continue;

                String baseName = n.replace(".tsv", "");

                String key = "";
                for (String btg : badToGood.keySet()) {
                    if (btg.equalsIgnoreCase(baseName)) {
                        key = btg;
                        break;
                    }
                }
                String goodName = badToGood.get(key);
                if (goodName == null) continue;

                for (String nf : namesFull) {
                    if (nf.contains(baseName)) {
                        System.out.println("Lookin' for " + nf);
                        List<String> theLines = variousfunctions.loadLines(Path.of(nf));
                        String altered = cf + "/" + goodName + ".tsv";
                        variousfunctions.writeLinesToCsv(theLines, altered);
                        System.out.println("Altered " + baseName + " to " + altered);
                    }
                }
            }
        }

        /*
         * Rewrite phospho CSV species names.
         */
        for (String cf : csvPhosphs) {
            List<String> linesRaw = variousfunctions.loadLines(Path.of(cf));
            List<String> editedLines = new ArrayList<>();
            System.out.println("began with " + linesRaw.size());

            for (String el : linesRaw) {

                if (el.contains("#")) {
                    editedLines.add(el);
                    continue;
                }

                String badName = getFirstField(el);
                System.out.println("Bad name " + badName);
                String goodName = badToGood.get(badName);

                if (goodName == null) {
                    editedLines.add(el);
                    continue;
                }

                String editedLine = el.replace(badName, goodName);
                editedLines.add(editedLine);
            }

            variousfunctions.writeLinesToCsv(editedLines, cf);
            System.out.println("ended with  " + editedLines.size() + " written to " + cf);
        }
    }

    /**
     * Extract the first token from a line, robust to tabs and multiple spaces.
     * Used to get the species name from NetPhos / CSV lines.
     */
    public static String getFirstField(String line) {
        if (line == null) return "";

        // Normalize whitespace to single spaces.
        String cleaned = line.replace('\t', ' ');
        cleaned = cleaned.replaceAll(" +", " ").trim();

        if (cleaned.isEmpty()) return "";

        int firstSpace = cleaned.indexOf(' ');
        if (firstSpace == -1) {
            return cleaned;
        }
        return cleaned.substring(0, firstSpace);
    }

    /**
     * Return the first parseable integer token in a line (as String), scanning
     * left-to-right and skipping anything non-numeric.
     */
    public static String getStart(String line) {
        if (line == null) return null;

        String[] parts = line.trim().split("\\s+");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            try {
                Integer.parseInt(p);
                return p;
            } catch (NumberFormatException ignored) {
                // not an integer, keep scanning
            }
        }
        return null;
    }

    /**
     * Same as getStart(...) but returns Integer instead of String.
     */
    public static Integer getStartInt(String line) {
        if (line == null) return null;

        String[] parts = line.trim().split("\\s+");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            try {
                return Integer.parseInt(p);
            } catch (NumberFormatException ignored) {
                // keep scanning
            }
        }
        return null;
    }
}
