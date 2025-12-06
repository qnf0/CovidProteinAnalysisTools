package flyhigh;

import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.FastaSequenceIndexCreator;
import htsjdk.samtools.reference.FastaSequenceIndexEntry;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FastaIO {
    /**
     * Read a multi-sequence FASTA into a Map: header -> sequence (A/C/G/T/N...).
     * @param fastaPath path to .fa/.fasta/.fna
     * @param firstTokenOnly if true, keep header only up to first whitespace
     * @throws IOException 
     */
    public static Map<String,String> readFastaToMap(String fastaPath, boolean firstTokenOnly) throws IOException {
        Map<String,String> out = new LinkedHashMap<>(); // preserves file order
        File f = new File(fastaPath);
        if (!f.exists()) throw new IllegalArgumentException("FASTA not found: " + f.getAbsolutePath());

        try (ReferenceSequenceFile ref =
                 ReferenceSequenceFileFactory.getReferenceSequenceFile(f)) {
            ReferenceSequence rs;
            while ((rs = ref.nextSequence()) != null) {
                String header = rs.getName();            // already excludes leading '>'
                if (firstTokenOnly) {
                    int sp = header.indexOf(' ');
                    if (sp >= 0) header = header.substring(0, sp);
                }
                String seq = new String(rs.getBases(), StandardCharsets.US_ASCII)
                                .replaceAll("\\s+", ""); // just in case
                out.put(header, seq);
            }
        }
        return out;
    }
    
    /**
     * Remove all spaces and other whitespace from a FASTA file in-place.
     * Rewrites the same file cleanly (sequence lines only changed).
     */
    public static void stripSpacesInFasta(String fastaPath) throws IOException {
        File f = new File(fastaPath);
        if (!f.isFile()) throw new IllegalArgumentException("FASTA not found: " + f.getAbsolutePath());

        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        List<String> cleaned = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith(">")) {
                cleaned.add(line.trim()); // keep headers
            } else {
                // remove all whitespace, including spaces, tabs, weird Unicode ones
                String noSpaces = line.replaceAll("\\s+", "");
                cleaned.add(noSpaces);
            }
        }

        Files.write(f.toPath(), cleaned, StandardCharsets.UTF_8);
    }
    public static Map<String, Integer> contigLengths(String fastaPath) {
    
        File fasta = new File(fastaPath);
        if (!fasta.isFile()) {
            throw new IllegalArgumentException("FASTA not found: " + fasta.getAbsolutePath());
        }

        File fai = new File(fastaPath + ".fai");
        if (!fai.exists()) {
        	System.out.println(fastaPath);
        	FaiBuilder.buildFai(fastaPath);
        	
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        try {
            // Use on-disk index if available; otherwise build from the FASTA contents
            FastaSequenceIndex idx = fai.exists()
                    ? new FastaSequenceIndex(fai)
                    : FastaSequenceIndexCreator.buildFromFasta(fasta.toPath());

            for (FastaSequenceIndexEntry e : idx) {
                long len = e.getSize(); // length in bases
                // You asked for Integer; clamp if ever >2.1Gbp
                out.put(e.getContig(), (int) Math.min(len, Integer.MAX_VALUE));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read/build FASTA index for: " + fastaPath, e);
        }
        return out;
    }
    
    /** Split a multi-FASTA into individual FASTA files (map-based; simple, cleans whitespace). */
    public static void splitFastaToFiles(String fastaPath, String outputDir) throws IOException {
        File fastaFile = new File(fastaPath);
        if (!fastaFile.isFile()) {
            throw new IllegalArgumentException("FASTA not found: " + fastaFile.getAbsolutePath());
        }

        // Read all sequences (first token of header only for filenames)
        Map<String, String> seqs = readFastaToMap(fastaPath, true);

        Path outDir = (outputDir == null || outputDir.isBlank())
                ? fastaFile.getParentFile().toPath()
                : Path.of(outputDir);
        Files.createDirectories(outDir);

        for (Map.Entry<String, String> e : seqs.entrySet()) {
            String header = e.getKey();
            String fileStem = header.replaceAll("[^A-Za-z0-9._-]", "_"); // sanitize filename
            Path outFile = outDir.resolve(fileStem + ".fasta");

            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outFile.toFile()), StandardCharsets.US_ASCII))) {

                // Write header
                bw.write(">");
                bw.write(header.trim());
                bw.newLine();

                // Clean sequence: remove *all* whitespace, including spaces/tabs/unicode
                String seq = e.getValue().replaceAll("\\s+", "");

                // Write 80 characters per line (FASTA convention)
                for (int i = 0; i < seq.length(); i += 80) {
                    int end = Math.min(i + 80, seq.length());
                    bw.write(seq, i, end - i);
                    bw.newLine();
                }
            }
        }
    }

    /** Write a multi-sequence FASTA from a Map<header, sequence> to outputPath. */
    public static void writeFromMap(Map<String, String> sequences, String outputPath) throws IOException {
        if (sequences == null) throw new IllegalArgumentException("sequences map is null");
        if (outputPath == null || outputPath.isBlank()) throw new IllegalArgumentException("outputPath is null/blank");

        File outFile = new File(outputPath);
        File parent = outFile.getParentFile();
        if (parent != null) {
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create parent directory: " + parent.getAbsolutePath());
            }
            if (parent.exists() && !parent.isDirectory()) {
                throw new IOException("Parent path exists but is not a directory: " + parent.getAbsolutePath());
            }
        }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), java.nio.charset.StandardCharsets.US_ASCII))) {

            // Iteration order will follow the map implementation (e.g., LinkedHashMap preserves insertion order).
            for (Map.Entry<String, String> e : sequences.entrySet()) {
                String header = e.getKey();
                if (header == null || header.isBlank()) {
                    throw new IllegalArgumentException("Encountered null/blank header in sequences map");
                }
                String seq = (e.getValue() == null ? "" : e.getValue()).replaceAll("\\s+", ""); // strip all whitespace

                // Header
                bw.write(">");
                bw.write(header.trim());
                bw.newLine();

                // Sequence wrapped at 80 chars/line (FASTA convention)
                for (int i = 0; i < seq.length(); i += 80) {
                    int end = Math.min(i + 80, seq.length());
                    bw.write(seq, i, end - i);
                    bw.newLine();
                }
            }
        }
    }

}
