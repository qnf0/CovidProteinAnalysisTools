<div align="center">🧬 Betacoronavirus Co-evolution Toolkit</div>
<div align="center"> A Java-based analysis suite for structural interactions, phosphorylation mapping, motif profiling, and clade-wide comparative virology.<br><br> <b>Main entry points:</b><br> <strong>flyhigh.Main</strong> – preprocessing, alignment-based analytics<br> <strong>flyhigh.MotifLoader</strong> – high-level motif + interaction visualization<br> </div> <br>
📁 Project Overview

This toolkit analyzes:

N–NSP3 structural interactions

Phosphorylation vs. gap frequencies across aligned betacoronavirus sequences

Motif distributions for kinase/functional elements

Distinct-arrangement comparisons (da1 / da2)

Immunogenicity windows and epitope density

The project expects a dataset structure like:

Covid/
  names.csv
  names_da1.csv
  names_da2.csv
  
  nucleocapsid_unique.fasta
  nucleocapsid_aligned.fasta
  nucleocapsid_alignedU.fasta
  NSP3_unique.fasta
  NSP3_aligned.fasta
  NSP3_alignedU.fasta
  
  Nucleocapsid-Phosph.csv
  NSP3-Phosph.csv
  
  Nucleocapsid-Motifs/
  NSP3-Motifs/
  
  Immune/
  interactions_sars2.csv
  interactions_mhvjhm.csv

OVERLAPS_MHVJHM/
  overlaps_nucleocapsid-NSP3.csv
  N-NSP3.fasta

⚙️ Building & Running
Compile
javac -cp . flyhigh/*.java

Run
java -cp . flyhigh.Main
java -cp . flyhigh.MotifLoader


⚠️ Both classes contain multiple script blocks inside main(...).
Toggle tasks by commenting/uncommenting and adjusting any System.exit(...) calls.

💡 Using flyhigh.Main

Main is the “preprocessing core” — it performs mapping, alignment, normalization, and per-site statistical calculation.

<div align="center">🧩 Task A — Map N–NSP3 interactions into aligned coordinates</div>

Purpose:
Convert residue-level interactions (from HDock/overlap CSV) into MSA-aligned positions for any species (e.g., $MHV_JHM).

How to run:

Ensure these are present:

overlaps_nucleocapsid-NSP3.csv
N-NSP3.fasta
nucleocapsid_unique.fasta
nucleocapsid_aligned.fasta
NSP3_unique.fasta
sars2_nsp3_aligned_chains.fasta


Edit:

String species = "$MHV_JHM";


Uncomment the “interaction mapping” block in Main.main.

Leave System.exit(1) after that block so nothing else runs.

Execute:

java -cp . flyhigh.Main > mappedInteractions.txt


Output: Aligned interaction pairs such as:

R:412  K:245  overlap=0.82  distance=3.4Å

<div align="center">🧬 Task B — Compute phosphorylation vs. gap frequencies</div>

Purpose:
For each aligned site across species:

Count NetPhos predictions

Compute proportion of gaps ('U')

How to run:

Comment out other tasks

Insert:

findPhosphorylationAndGaps(
    "/path/out.csv",
    "/path/Nucleocapsid-phosph.csv",
    "/path/nucleocapsid_alignedU.fasta",
    "da1"
);


Make sure names_da1.csv or names_da2.csv exists (for whitelist filtering).

Run:

java -cp . flyhigh.Main


Output CSV:

site,gapPerc,phosphSites
100,0.14,2
101,0.00,0
...

<div align="center">🧭 Task C — Normalize species names (FASTA/TSV/CSV rewriting)</div>

Purpose:
Standardize species identifiers across all data types.

⚠️ Destructive operation — modifies files in place.
Make a backup if necessary.

How to run:

setNames("/Users/.../Covid");
System.exit(0);


setNames(...) will:

Rewrite FASTA headers

Rename motif TSV files

Rewrite phospho CSVs

Use ChatClient to map ambiguous names

Use once per dataset.

🎨 Using flyhigh.MotifLoader

MotifLoader is the analysis + visualization engine.
It loads motifs, phosphorylation data, interactions, and alignment info, then produces integrated PNG plots using LociPlotter.

<div align="center">🌈 Task D — Generate combined N + NSP3 motif/interaction plots</div>

This is the “final” analysis most users run.

What it does:

Loads interactions (interactions_sars2.csv, interactions_mhvjhm.csv)

Loads motif TSVs from Protein-Motifs/

Loads phosphorylation site CSVs (output of Main)

Maps everything to aligned coordinates

Constructs Nicotine objects representing regions

Plots both arrangements (da1, da2) side-by-side

How to run:

Ensure files exist:

interactions_sars2.csv
interactions_mhvjhm.csv
Nucleocapsid-Motifs/
NSP3-Motifs/
Nucleocapsid_sites_da1.csv
Nucleocapsid_sites_da2.csv
NSP3_sites_da1.csv
NSP3_sites_da2.csv


In MotifLoader.main, adjust:

int roi = 400;
int buff = 100;


Ensure the block containing:

LociPlotter.plotTwoNicotines(...)


is active.

Run:

java -cp . flyhigh.MotifLoader

Output:

A high-resolution PNG (e.g., nicotine_pair.png) showing:

Kinase motif clusters

Phosphorylation density

Gaps

N–NSP3 interaction regions

Clade comparison (da1 vs da2)

<div align="center">🔬 Task E — Generate immunogenicity windows (optional)</div>

Purpose:
Aggregate IEDB TepiTool epitope predictions across sliding windows.

How to run:

Place per-species CSVs in:

Covid/Immune/


In MotifLoader.main, enable:

compileImmunogenicityData(...)
System.exit(1);


Run:

java -cp . flyhigh.MotifLoader


Outputs:

immune.csv — candlestick-style summary

immune2_0.csv — long-form x,score table

📌 Recommended Workflow
<ol> <li><b>Normalize species names</b> (run once) <br><code>Main.setNames(root)</code></li> <li><b>Generate phosphorylation/gap statistics</b> <br>(run <code>findPhosphorylationAndGaps</code> for each protein × arrangement)</li> <li><b>(Optional) Generate immunogenicity statistics</b></li> <li><b>Run MotifLoader</b> to create integrated motif + phospho + interaction figures</li> </ol>
<div align="center">🧭 Want CLI mode?</div>
