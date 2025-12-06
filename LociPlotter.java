package flyhigh;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.VerticalAlignment;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.annotations.XYLineAnnotation;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class LociPlotter {
	private static final double INNER_PAD_FRAC = 0.06;  // 6% inward

	// canonical motif colors
	private static final Map<String, Color> MOTIF_COLORS = new LinkedHashMap<>();
	private static final Map<String, Color> FALLBACK_COLORS = new HashMap<>();
	private static final Random COLOR_RNG = new Random(12345);

	static {
	    MOTIF_COLORS.put("Basophilic serine/threonine kinase group", new Color(60, 120, 216));
	    MOTIF_COLORS.put("Proline-dependent serine/threonine kinase group", new Color(230, 120, 40));
	    MOTIF_COLORS.put("Phosphoserine/threonine binding group", new Color(60, 160, 60));
	    MOTIF_COLORS.put("Kinase binding site group", new Color(200, 60, 60));
	    MOTIF_COLORS.put("DNA damage kinase group", new Color(140, 80, 200));
	    MOTIF_COLORS.put("Acidophilic serine/threonine kinase group", new Color(156, 255, 90));
	    MOTIF_COLORS.put("Phosphotyrosine binding group", new Color(215, 105, 190));
	    MOTIF_COLORS.put("Tyrosine kinase group", new Color(110, 110, 110));
	}

	// return official color or stable randomized fallback
	private static Color getMotifColor(String group) {
	    if (group == null) {
	        return new Color(180, 180, 180); // gray fallback
	    }

	    Color c = MOTIF_COLORS.get(group);
	    if (c != null) return c;

	    // unknown -> generate stable fallback
	    return FALLBACK_COLORS.computeIfAbsent(group, k ->
	            new Color(
	                    80 + COLOR_RNG.nextInt(150),
	                    80 + COLOR_RNG.nextInt(150),
	                    80 + COLOR_RNG.nextInt(150),
	                    210  // alpha
	            )
	    );
	}
    /** Simple geometry holder for each motif box. */
    private static class BarGeom {
        RL rl;
        double x0, x1, y0, y1, yCenter;

        BarGeom(RL rl, double x0, double x1, double y0, double y1, double yCenter) {
            this.rl = rl;
            this.x0 = x0;
            this.x1 = x1;
            this.y0 = y0;
            this.y1 = y1;
            this.yCenter = yCenter;
        }
    }

public static void plotTwoSquareBox(String outPng) throws IOException {

    // Tiny dummy dataset just so JFreeChart will create a plot
    XYSeries s = new XYSeries("dummy");
    s.add(0, 0);
    XYSeriesCollection ds = new XYSeriesCollection(s);

    JFreeChart chart = ChartFactory.createScatterPlot(
            "Two-Square Box Demo",
            "", "", ds
    );

    // Get plot and clean it up
    XYPlot plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.WHITE);
    chart.setBackgroundPaint(Color.WHITE);
    chart.removeLegend();

    // Hide axes, but we still use their coordinate system
    plot.getDomainAxis().setVisible(false);
    plot.getRangeAxis().setVisible(false);
 // Hide axes, but still use them as a coordinate system
    plot.getDomainAxis().setVisible(false);
    plot.getRangeAxis().setVisible(false);

    // OLD
    // plot.getDomainAxis().setRange(0.0, 2.0);
    // plot.getRangeAxis().setRange(0.0, 1.0);

    // NEW: extend Y downward for legend
    plot.getDomainAxis().setRange(0.0, 2.0);
    plot.getRangeAxis().setRange(-0.35, 1.0);   // extra space below
    
    // Simple coordinate system: width 0..2, height 0..1
    plot.getDomainAxis().setRange(0.0, 2.0);
    plot.getRangeAxis().setRange(0.0, 1.0);

    // ---------------------------------------------------
    // Define the two squares (left and right)
    // ---------------------------------------------------
    double margin = 0.05;  // small inset from the border

 // -----------------------------
 // Big squares: left + right
 // -----------------------------
 double marginOuter = 0.05;
 double leftX0  = 0.0 + marginOuter;
 double leftX1  = 1.0 - marginOuter;
 double rightX0 = 1.0 + marginOuter;
 double rightX1 = 2.0 - marginOuter;

 // OLD
 // double y0      = 0.05;
 // double y1      = 0.95;

 // NEW – move squares up, leave bottom band for legend
 double y0      = 0.05;
 double y1      = 0.95;

    // Draw left square
    XYBoxAnnotation leftSquare = new XYBoxAnnotation(
            leftX0, y0, leftX1, y1,
            new BasicStroke(2.0f),
            Color.BLACK,
            Color.WHITE
    );
    plot.addAnnotation(leftSquare);

    // Draw right square
    XYBoxAnnotation rightSquare = new XYBoxAnnotation(
            rightX0, y0, rightX1, y1,
            new BasicStroke(2.0f),
            Color.BLACK,
            Color.WHITE
    );
    

    plot.addAnnotation(rightSquare);
 // -------------------------------------------------
 // Label big squares: α-Cluster (left), β-Cluster (right)
 // -------------------------------------------------
 double boxHeight   = y1 - y0;
 double labelYOffset = boxHeight * 0.03;  // how far below top edge

 double leftCenterX  = (leftX0  + leftX1)  / 2.0;
 double rightCenterX = (rightX0 + rightX1) / 2.0;
 double clusterLabelY = y1 - labelYOffset;

 // left: α-Cluster
 XYTextAnnotation alphaLabel =
         new XYTextAnnotation("α-Cluster", leftCenterX, clusterLabelY);
 alphaLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
 alphaLabel.setTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_CENTER);
 plot.addAnnotation(alphaLabel);

 // right: β-Cluster
 XYTextAnnotation betaLabel =
         new XYTextAnnotation("β-Cluster", rightCenterX, clusterLabelY);
 betaLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
 betaLabel.setTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_CENTER);
 plot.addAnnotation(betaLabel);
    // ---------------------------------------------------
    // Modular hook: called once for each square
    // (later we’ll draw inside them; for now just hello)
    // ---------------------------------------------------
    helloSquare("left",  leftX0,  leftX1, y0, y1);
    helloSquare("right", rightX0, rightX1, y0, y1);
 // center under the whole figure
    double legendCenterX = 1.0;   // middle of x-range [0,2]

    // start a bit below the bottom of the squares
    double legendStartY = y0 - 0.06;   // just below y0 (squares’ bottom)

    // dimensions and spacing in axis units
    double boxW    = 0.025;
    double boxH    = 0.025;
    double rowStep = 0.07;

    // build boxes + labels
    List<XYBoxAnnotation> legendBoxes =
            buildMotifLegendBoxes(legendCenterX - 0.25, legendStartY, boxW, boxH, rowStep);
    List<XYTextAnnotation> legendLabels =
            buildMotifLegendLabelsBelow(legendCenterX - 0.22, legendStartY, rowStep);

    // render them
    for (XYBoxAnnotation b : legendBoxes) {
        plot.addAnnotation(b);
    }
    for (XYTextAnnotation t : legendLabels) {
        plot.addAnnotation(t);
    }
    // Save PNG
    ChartUtils.saveChartAsPNG(new File(outPng), chart, 800, 400);
}

//For now just a stub; later we'll draw content inside the square
private static void helloSquare(String label,
                             double x0, double x1,
                             double y0, double y1) {
 System.out.println("hello world from " + label +
         " square: x=[" + x0 + "," + x1 + "], y=[" + y0 + "," + y1 + "]");
}

//Panel for motif stacks in the right big square:
//horizontal mid-line; N1 motifs stacked in top half, N2 motifs in bottom half.
private static void drawMotifPanel(
     XYPlot plot,
     Nicotine n1,
     Nicotine n2,
     double x0, double x1,
     double y0, double y1) {
	
	
	
	  
 double padX = (x1 - x0) * 0.08;
 double padY = (y1 - y0) * 0.10;

 double innerX0 = x0 + padX;
 double innerX1 = x1 - padX;
 double innerY0 = y0 + padY;
 double innerY1 = y1 - padY;

 double midY = (innerY0 + innerY1) / 2.0;

 // horizontal divider
 org.jfree.chart.annotations.XYLineAnnotation midLine =
         new org.jfree.chart.annotations.XYLineAnnotation(
                 innerX0, midY, innerX1, midY,
                 new BasicStroke(1.2f),
                 Color.BLACK
         );
 plot.addAnnotation(midLine);

 // top half: Nicotine 1 motifs
 drawMotifStack(plot, n1, innerX0, innerX1, midY, innerY1, true);

 // bottom half: Nicotine 2 motifs
 drawMotifStack(plot, n2, innerX0, innerX1, innerY0, midY, false);
}

//Draw a vertical stack of motif boxes between yMin..yMax
private static void drawMotifStack(
     XYPlot plot,
     Nicotine nic,
     double x0, double x1,
     double yMin, double yMax,
     boolean topToBottom) {

  	for (RL real : nic.motifLocations) {
  		System.out.println("real at "+real.midpoint);
  	}

 if (nic == null || nic.motifLocations == null || nic.motifLocations.isEmpty()) {
     return;
 }

 List<RL> motifs = new ArrayList<>(nic.motifLocations);

 // sort by midpoint just for stable order
 motifs.sort(Comparator.comparingInt(r -> r.midpoint));

 int n = motifs.size();
 if (n == 0) return;

 double width  = x1 - x0;
 double height = yMax - yMin;

 double boxHalfW = width * 0.16;
 double stepY    = height / (n + 1);       // spacing between centers
 double boxHalfH = stepY * 0.30;           // leave white space between boxes

 // color map similar to earlier motif plot
 Map<String, Color> colorByGroup = new LinkedHashMap<>();
 colorByGroup.put("Basophilic serine/threonine kinase group", new Color(60, 120, 216));
 colorByGroup.put("Proline-dependent serine/threonine kinase group", new Color(230, 120, 40));
 colorByGroup.put("Phosphoserine/threonine binding group", new Color(60, 160, 60));
 colorByGroup.put("Kinase binding site group", new Color(200, 60, 60));
 colorByGroup.put("DNA damage kinase group", new Color(140, 80, 200));
 colorByGroup.put("Acidophilic serine/threonine kinase group", new Color(155, 120, 90));
 colorByGroup.put("Phosphotyrosine binding group", new Color(215, 105, 190));
 colorByGroup.put("Tyrosine kinase group", new Color(110, 110, 110));

 Random rand = new Random(12345);

 double centerX = (x0 + x1) / 2.0;

 for (int i = 0; i < n; i++) {
     RL r = motifs.get(i);

     double fracIndex = i + 1;
     double yCenter;

     if (topToBottom) {
         // first motif closest to top of its half
         yCenter = yMax - fracIndex * stepY;
     } else {
         // first motif closest to bottom of its half
         yCenter = yMin + fracIndex * stepY;
     }

     double y0Box = yCenter - boxHalfH;
     double y1Box = yCenter + boxHalfH;
     double x0Box = centerX - boxHalfW;
     double x1Box = centerX + boxHalfW;

     Color base = colorByGroup.computeIfAbsent(r.name, k ->
             new Color(rand.nextInt(200) + 30,
                       rand.nextInt(200) + 30,
                       rand.nextInt(200) + 30));

     XYBoxAnnotation box = new XYBoxAnnotation(
             x0Box, y0Box, x1Box, y1Box,
             new BasicStroke(1.2f),
             new Color(40, 40, 40),
             new Color(base.getRed(), base.getGreen(), base.getBlue(), 210)
     );
     plot.addAnnotation(box);
 }
}

//Map a residue position in a Nicotine window to a Y coordinate on the plot,
//using the same geometry as drawNicotineTrack.
private static double residueToY(Nicotine nic, int residue, double y0, double y1) {
 if (nic == null) {
     return (y0 + y1) / 2.0;
 }

 int center = nic.center;
 int buffer = nic.buffer;
 int start  = center - buffer;
 int end    = center + buffer;
 double span = end - start;
 if (span <= 0) {
     return (y0 + y1) / 2.0;
 }

 // same vertical padding as drawNicotineTrack
 double vertPad  = (y1 - y0) * 0.07;
 double innerY0  = y0 + vertPad;
 double innerY1  = y1 - vertPad;
 double innerH   = innerY1 - innerY0;

 double frac = (residue - start) / span;
 if (frac < 0.0) frac = 0.0;
 if (frac > 1.0) frac = 1.0;

 return innerY0 + frac * innerH;
}

public static void plotTwoNicotines(
        Nicotine alpha1,
        Nicotine alpha2,
        Nicotine beta1,
        Nicotine beta2,
        String outPng,
        String title,
        List<ResInteraction> alphaInteractions,
        List<ResInteraction> betaInteractions) throws IOException {

    // Dummy dataset so JFreeChart will give us an XYPlot
    XYSeries s = new XYSeries("dummy");
    s.add(0, 0);
    XYSeriesCollection ds = new XYSeriesCollection(s);

    JFreeChart chart = ChartFactory.createScatterPlot(
            title,
            "", "", ds
    );

    XYPlot plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.WHITE);
    chart.setBackgroundPaint(Color.WHITE);
    chart.removeLegend();

    // Hide axes, but still use them as a coordinate system
    plot.getDomainAxis().setVisible(false);
    plot.getRangeAxis().setVisible(false);

    // --- IMPORTANT: allow space *below* the squares ---
    plot.getDomainAxis().setRange(0.0, 2.0);
    plot.getRangeAxis().setRange(-0.35, 1.0);   // bottom band for legend

    // -----------------------------
    // Big squares: left + right
    // -----------------------------
    double marginOuter = 0.05;
    double leftX0  = 0.0 + marginOuter;
    double leftX1  = 1.0 - marginOuter;
    double innerPadFrac = 0.06;  // 6% of the box width inside

    double rightX0 = 1.0 + marginOuter;
    double rightX1 = 2.0 - marginOuter;

    // keep squares in [0.05 .. 0.95]; legend lives below 0
    double y0      = 0.05;
    double y1      = 0.95;

    // Left big square
    XYBoxAnnotation leftSquare = new XYBoxAnnotation(
            leftX0, y0, leftX1, y1,
            new BasicStroke(2.0f),
            Color.BLACK,
            Color.WHITE
    );
    plot.addAnnotation(leftSquare);

    // Right big square
    XYBoxAnnotation rightSquare = new XYBoxAnnotation(
            rightX0, y0, rightX1, y1,
            new BasicStroke(2.0f),
            Color.BLACK,
            Color.WHITE
    );
    plot.addAnnotation(rightSquare);

    // α-cluster panel in LEFT square
    drawClusterPanel(
            plot,
            alpha1, alpha2,
            leftX0, leftX1,
            y0, y1,
            "α-Cluster",
            alphaInteractions
    );

 // β-cluster panel in RIGHT square
    drawClusterPanel(
            plot,
            beta1, beta2,
            rightX0, rightX1,
            y0, y1,
            "β-Cluster",
            betaInteractions
    );
 // --------------------------------------------------------
 // MOTIF LEGEND BELOW BOTH SQUARES (only used groups)
 // --------------------------------------------------------
 Set<String> usedGroupsSet = collectUsedMotifGroups(alpha1, alpha2, beta1, beta2);
 if (!usedGroupsSet.isEmpty()) {

     List<String> usedGroups = new ArrayList<>(usedGroupsSet);

     double legendCenterX = 1.0;      // mid of [0,2]
     double legendStartY  = -0.05;    // first row
     double rowStep       = 0.06;     // vertical spacing

     double boxW = 0.025;
     double boxH = 0.025;

     // place boxes + labels
     double boxesX = legendCenterX - 0.35;   // left-ish
     double textX  = boxesX + 0.06;

     List<XYBoxAnnotation> legendBoxes =
             buildMotifLegendBoxes(
                     boxesX,
                     legendStartY,
                     boxW, boxH, rowStep,
                     usedGroups
             );

     List<XYTextAnnotation> legendLabels =
             buildMotifLegendLabelsBelow(
                     textX,
                     legendStartY,
                     rowStep,
                     usedGroups
             );

     // --- draw legend frame box ---
     int n = usedGroups.size();
     double legendTopY    = legendStartY + boxH * 0.8;
     double legendBottomY = legendStartY - (n - 1) * rowStep - boxH * 0.8;

     double frameLeftX  = boxesX - 0.08;
     double frameRightX = textX + 0.62;   // enough for text

     XYBoxAnnotation legendFrame = new XYBoxAnnotation(
             frameLeftX, legendBottomY,
             frameRightX, legendTopY,
             new BasicStroke(1.2f),
             Color.BLACK,
             new Color(255, 255, 255, 240) // white-ish fill
     );
     plot.addAnnotation(legendFrame);

     // legend title
     double titleY = legendTopY + 0.03;
     XYTextAnnotation legendTitle =
             new XYTextAnnotation("Motif groups", legendCenterX, titleY);
     legendTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
     legendTitle.setTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_CENTER);
     plot.addAnnotation(legendTitle);

     // now draw boxes + labels on top of frame
     for (XYBoxAnnotation b : legendBoxes) {
         plot.addAnnotation(b);
     }
     for (XYTextAnnotation t : legendLabels) {
         plot.addAnnotation(t);
     }
 }

  

    // Save
    ChartUtils.saveChartAsPNG(new File(outPng), chart, 1400, 700); // a bit taller
}

private static void drawNicotineTrack(
        XYPlot plot,
        Nicotine nic,
        double x0, double x1,
        double y0, double y1,
        String label,
        boolean invertBars) {
	
	

    if (nic == null || nic.siteLocations == null || nic.siteLocations.isEmpty()) {
        return;
    }

    int center = nic.center;
    int buffer = nic.buffer;
    int start  = center - buffer;
    int end    = center + buffer;
    double span = end - start;
    if (span <= 0) return;

    int daSize = Math.max(1, nic.da_size);   // avoid div-by-zero

    // total horizontal space for this Nicotine
    double columnWidth = (x1 - x0);

    double midX;
    if (!invertBars) {
        // move closer to LEFT edge
        midX = x0 + columnWidth * 0.22;   // was 0.30
    } else {
        // move closer to RIGHT edge
        midX = x0 + columnWidth * 0.78;   // was 0.70
    }

    // ------------------ vertical padding / inner box ------------------
    double vertPad  = (y1 - y0) * 0.07;
    double innerY0  = y0 + vertPad;
    double innerY1  = y1 - vertPad;
    double innerH   = innerY1 - innerY0;

    // axis line inside inner region
    XYBoxAnnotation axisLine = new XYBoxAnnotation(
            midX - 0.001, innerY0, midX + 0.001, innerY1,
            new BasicStroke(1.2f),
            Color.BLACK,
            Color.BLACK
    );
    plot.addAnnotation(axisLine);

    // name above inner region
    double nameY = innerY1 + vertPad * 0.4;
    XYTextAnnotation nameText =
            new XYTextAnnotation(label, midX, nameY);
    nameText.setFont(new Font("SansSerif", Font.PLAIN, 14));
    nameText.setTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_CENTER);
    plot.addAnnotation(nameText);

    // ------------------ bar geometry ------------------
    double baseBarWidth = columnWidth * 0.10;   // max bar width
    double barGap       = columnWidth * 0.03;   // distance from axis to bar
    double barWidth     = baseBarWidth;

    // height & spacing
    double heightScale = 0.55;
    double rawHeight   = (y1 - y0) / span;
    double rectHalfHeight = (rawHeight * heightScale) / 2.0;
    double minH = 0.004 * (y1 - y0);
    double maxH = 0.025 * (y1 - y0);
    rectHalfHeight = Math.max(minH, Math.min(maxH, rectHalfHeight));

    // gap still scaled by maxGap
    double maxGap = nic.siteLocations.stream()
            .mapToDouble(s -> s.gapPercent)
            .max().orElse(1.0);

    double minFrac = 0.30;  // minimum visible width fraction

    for (Site s : nic.siteLocations) {
        double frac = (s.location - start) / span;
        if (frac < 0 || frac > 1) continue;

        double yCenter = innerY0 + frac * innerH;
        double yBottom = yCenter - rectHalfHeight;
        double yTop    = yCenter + rectHalfHeight;

        // ---------- GAP BAR ----------
        if (s.gapPercent > 0 && maxGap > 0) {
            double widthFrac   = s.gapPercent / maxGap;
            widthFrac = Math.max(minFrac, Math.min(1.0, widthFrac));
            double scaledWidth = barWidth * widthFrac;

            double gx0, gx1;
            if (!invertBars) {
                // normal: GAP on left
                gx0 = midX - barGap - scaledWidth;
                gx1 = midX - barGap;
            } else {
                // inverted: GAP on right
                gx0 = midX + barGap;
                gx1 = midX + barGap + scaledWidth;
            }

            XYBoxAnnotation gapBox = new XYBoxAnnotation(
                    gx0, yBottom, gx1, yTop,
                    new BasicStroke(1.0f),
                    Color.BLACK,
                    new Color(18, 167, 171)   // #12A7AB
            );
            plot.addAnnotation(gapBox);
        }

        // ---------- PHOSPHORYLATION BAR ----------
        if (s.phosphorylationNumber > 0) {
            // scale by da_size instead of maxPhos
            double widthFrac   = s.phosphorylationNumber / (double) daSize;
            widthFrac = Math.max(minFrac, Math.min(1.0, widthFrac));
            double scaledWidth = barWidth * widthFrac;

            double px0, px1;
            if (!invertBars) {
                // normal: PHOS on right
                px0 = midX + barGap;
                px1 = midX + barGap + scaledWidth;
            } else {
                // inverted: PHOS on left
                px0 = midX - barGap - scaledWidth;
                px1 = midX - barGap;
            }

            XYBoxAnnotation phosBox = new XYBoxAnnotation(
                    px0, yBottom, px1, yTop,
                    new BasicStroke(1.0f),
                    Color.BLACK,
                    new Color(251, 177, 79)   // #FBB14F
            );
            plot.addAnnotation(phosBox);
        }
    }

    // ------------------ bottom “100% ...” labels + line ------------------
    // ------------------ bottom “100% ...” labels + line ------------------
    // ------------------ bottom “100% ...” labels + line ------------------
    // move labels a bit higher from the very bottom
    double labelY = y0 + vertPad * 0.8;   // was 0.4

    // positions for left & right labels under the bar region
    // push farther out from the center
    double labelOffsetX = columnWidth * 0.22;   // was 0.15
    double leftLabelX   = midX - labelOffsetX;
    double rightLabelX  = midX + labelOffsetX;

    String leftText, rightText;
    if (!invertBars) {
        // Nicotine 1: 100% Gap — 100% Phosph
        leftText  = "100% Gap";
        rightText = "100% Phosph";
    } else {
        // Nicotine 2: 100% Phosph — 100% Gap
        leftText  = "100% Phosph";
        rightText = "100% Gap";
    }

    XYTextAnnotation leftAnn =
            new XYTextAnnotation(leftText, leftLabelX, labelY);
    leftAnn.setFont(new Font("SansSerif", Font.PLAIN, 11));
    leftAnn.setTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_CENTER);
    plot.addAnnotation(leftAnn);

    XYTextAnnotation rightAnn =
            new XYTextAnnotation(rightText, rightLabelX, labelY);
    rightAnn.setFont(new Font("SansSerif", Font.PLAIN, 11));
    rightAnn.setTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_CENTER);
    plot.addAnnotation(rightAnn);

    // -------- horizontal line + tick marks + "0" label --------
    double baseH   = (y1 - y0);

    // move the line slightly ABOVE the text (toward the plot)
    double lineY   = labelY + 0.01 * baseH;      // was labelY - 0.012 * baseH
    double tickH   = 0.018 * baseH;

    double lineStartX = leftLabelX  + 0.02 * columnWidth;
    double lineEndX   = rightLabelX - 0.02 * columnWidth;
    double midTickX   = (lineStartX + lineEndX) / 2.0;

    // main horizontal line
    XYLineAnnotation line = new XYLineAnnotation(
            lineStartX, lineY,
            lineEndX,   lineY,
            new BasicStroke(1.0f),
            Color.BLACK
    );
    plot.addAnnotation(line);

    // left end tick
    XYLineAnnotation leftTick = new XYLineAnnotation(
            lineStartX, lineY - tickH / 2.0,
            lineStartX, lineY + tickH / 2.0,
            new BasicStroke(1.0f),
            Color.BLACK
    );
    plot.addAnnotation(leftTick);

    // right end tick
    XYLineAnnotation rightTick = new XYLineAnnotation(
            lineEndX, lineY - tickH / 2.0,
            lineEndX, lineY + tickH / 2.0,
            new BasicStroke(1.0f),
            Color.BLACK
    );
    plot.addAnnotation(rightTick);

    // middle tick
    XYLineAnnotation midTick = new XYLineAnnotation(
            midTickX, lineY - tickH / 2.0,
            midTickX, lineY + tickH / 2.0,
            new BasicStroke(1.0f),
            Color.BLACK
    );
    plot.addAnnotation(midTick);

    // "0" label at the middle, just below the line
    double zeroLabelY = lineY - tickH / 2.0 - 0.006 * baseH;
    XYTextAnnotation zeroAnn =
            new XYTextAnnotation("0", midTickX, zeroLabelY);
    zeroAnn.setFont(new Font("SansSerif", Font.PLAIN, 11));
    zeroAnn.setTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_CENTER);
    plot.addAnnotation(zeroAnn);
}

    public static void plotBarsStackedColored(List<RL> loci,
                                              List<Site> sites,
                                              String outPng,
                                              String chartName) throws IOException {

        Set<String> usedGroups = new LinkedHashSet<>();

        // ---- copy + sort RLs by midpoint ----
        List<RL> sorted = new ArrayList<>(loci);
        sorted.sort(Comparator.comparingInt(r -> r.midpoint));

        // ---- global min/max X from RLs ----
        double minX = sorted.stream().mapToDouble(r -> r.midpoint).min().orElse(0.0);
        double maxX = sorted.stream().mapToDouble(r -> r.midpoint).max().orElse(minX + 1.0);

        // tiny dataset so chart has axes
        XYSeries series = new XYSeries("sites");
        series.add(minX, 0.0);
        series.add(maxX, 0.0);
        XYSeriesCollection ds = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createScatterPlot(
                chartName,
                "Position",
                "",
                ds
        );

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);

        // ------------------------------------------------------------------
        // HARD-CODED COLORS + fallback random colors for unknown groups
        // ------------------------------------------------------------------
        Map<String, Color> colorByGroup = new LinkedHashMap<>();
        colorByGroup.put("Basophilic serine/threonine kinase group",
                new Color(60, 120, 216));   // blue
        colorByGroup.put("Proline-dependent serine/threonine kinase group",
                new Color(230, 120, 40));   // orange
        colorByGroup.put("Phosphoserine/threonine binding group",
                new Color(60, 160, 60));    // green
        colorByGroup.put("Kinase binding site group",
                new Color(200, 60, 60));    // red
        colorByGroup.put("DNA damage kinase group",
                new Color(140, 80, 200));   // purple
        colorByGroup.put("Acidophilic serine/threonine kinase group",
                new Color(155, 120, 90));   // brown
        colorByGroup.put("Phosphotyrosine binding group",
                new Color(215, 105, 190));  // pink
        colorByGroup.put("Tyrosine kinase group",
                new Color(110, 110, 110));  // gray

        Random rand = new Random(12345); // fallback colors for unknown groups

        // ----------------------------------------------------------
        // Motif bar + stacking params
        // ----------------------------------------------------------
        double barHeight = 1.0;
        double barHalfH  = barHeight / 2.0;
        double barWidth  = 6.0;
        double tierStep  = barHeight;    // bars touch vertically
        double minDx     = 8.0;

        // First pass: compute tiers & geometry (no drawing yet)
        List<Double> lastXperTier = new ArrayList<>();
        int maxTier = -1;
        List<BarGeom> barGeoms = new ArrayList<>();

        for (RL r : sorted) {
            double x = r.midpoint;

            int tier = 0;
            for (; tier < lastXperTier.size(); tier++) {
                if (Math.abs(x - lastXperTier.get(tier)) >= minDx) {
                    break;
                }
            }
            if (tier == lastXperTier.size()) {
                lastXperTier.add(x);
            } else {
                lastXperTier.set(tier, x);
            }
            if (tier > maxTier) maxTier = tier;

            // (temporarily) put tier 0 just above 0; we’ll shift later
            double yCenter = tier * tierStep + 0.5;

            double y0 = yCenter - barHalfH;
            double y1 = yCenter + barHalfH;
            double x0 = x - barWidth / 2.0;
            double x1 = x + barWidth / 2.0;

            barGeoms.add(new BarGeom(r, x0, x1, y0, y1, yCenter));
            usedGroups.add(r.name);
        }

        // ---- axes ----
        plot.getDomainAxis().setRange(minX - 8, maxX + 8);

        double maxY = maxTier >= 0
                ? (maxTier * tierStep + 0.5) + barHalfH + 0.8
                : 1.0;

        // We will use [ -phosRegionHeight .. +maxY ] initially
        double phosRegionHeight = maxY;

        plot.getRangeAxis().setAutoRange(false);
        plot.getRangeAxis().setRange(-phosRegionHeight, maxY);
        plot.getRangeAxis().setVisible(false);

        plot.getDomainAxis().setLabel("Position");

        // -----------------------------------------------------------------
        // Draw site bars: phosphorylation up, gapPercent down from midline
        // -----------------------------------------------------------------
        double maxPhosTopY = Double.NEGATIVE_INFINITY;

        if (sites != null && !sites.isEmpty()) {
            int maxPhos = sites.stream()
                    .mapToInt(s -> s.phosphorylationNumber)
                    .max().orElse(0);

            double maxGap = sites.stream()
                    .mapToDouble(s -> s.gapPercent)
                    .max().orElse(0.0);

            if (maxPhos > 0 || maxGap > 0.0) {
                // negative region is [-phosRegionHeight, 0]
                // we split it: upper half for phos, lower half for gap
                double halfRegion = phosRegionHeight / 2.0;
                double midLineY   = -halfRegion;  // midpoint between 0 and axis min

                for (Site s : sites) {
                    if (s.location < minX || s.location > maxX) continue;

                    double x = s.location;
                    double x0 = x - 0.4;
                    double x1 = x + 0.4;

                    // --- phosphorylation: from midLineY UP ---
                    if (maxPhos > 0 && s.phosphorylationNumber > 0) {
                        double phosHeight = (s.phosphorylationNumber / (double) maxPhos) * halfRegion;
                        double yPhosBottom = midLineY;
                        double yPhosTop    = midLineY + phosHeight;

                        Color phosFill = new Color(80, 140, 230, 70); // soft blue
                        XYBoxAnnotation phosBar = new XYBoxAnnotation(
                                x0, yPhosBottom, x1, yPhosTop,
                                null, null, phosFill
                        );
                        plot.addAnnotation(phosBar);

                        if (yPhosTop > maxPhosTopY) {
                            maxPhosTopY = yPhosTop;
                        }
                    }

                    // --- gapPercent: from midLineY DOWN ---
                    if (maxGap > 0.0 && s.gapPercent > 0.0) {
                        double gapDepth = (s.gapPercent / maxGap) * halfRegion;
                        double yGapTop    = midLineY;
                        double yGapBottom = midLineY - gapDepth;

                        Color gapFill = new Color(210, 160, 120, 60); // soft peach
                        XYBoxAnnotation gapBar = new XYBoxAnnotation(
                                x0, yGapBottom, x1, yGapTop,
                                null, null, gapFill
                        );
                        plot.addAnnotation(gapBar);
                    }
                }

                // Draw midline for visual separation of phos vs gap
                ValueMarker mid = new ValueMarker(-halfRegion);
                mid.setPaint(new Color(220, 220, 220));
                mid.setStroke(new BasicStroke(0.7f));
                plot.addRangeMarker(mid);
            }
        }

        // -----------------------------------------------------------------
        // Move motif boxes so their bottom sits on the tallest phospho bar
        // -----------------------------------------------------------------
        if (maxPhosTopY != Double.NEGATIVE_INFINITY && !barGeoms.isEmpty()) {
            // current lowest motif bottom (tier 0 box bottom)
            double minMotifY0 = barGeoms.stream()
                    .mapToDouble(g -> g.y0)
                    .min()
                    .orElse(0.0); // should be ~0

            double shift = maxPhosTopY - minMotifY0;

            for (BarGeom g : barGeoms) {
                g.y0      += shift;
                g.y1      += shift;
                g.yCenter += shift;
            }

            // update maxY and axis range so everything fits
            maxY += shift;
            plot.getRangeAxis().setRange(-phosRegionHeight, maxY + 0.5);
        }

        // Zero line (baseline for site bars)
        ValueMarker zero = new ValueMarker(0.0);
        zero.setPaint(new Color(160, 160, 160));
        zero.setStroke(new BasicStroke(1.2f));
        plot.addRangeMarker(zero);

        // -----------------------------------------------------------------
        // Draw motif boxes & labels on top of the background bars
        // -----------------------------------------------------------------
        for (BarGeom g : barGeoms) {
            RL r = g.rl;

            Color base = colorByGroup.computeIfAbsent(r.name, k ->
                    new Color(rand.nextInt(200) + 30,
                              rand.nextInt(200) + 30,
                              rand.nextInt(200) + 30));

            // motif boxes: darker outline, slightly lighter fill
            XYBoxAnnotation box = new XYBoxAnnotation(
                    g.x0, g.y0, g.x1, g.y1,
                    new BasicStroke(1.4f),
                    new Color(40, 40, 40),
                    new Color(base.getRed(), base.getGreen(), base.getBlue(), 210)
            );
            plot.addAnnotation(box);

            String label = r.speciesName;
            double textX = (g.x0 + g.x1) / 2.0;
            double textY = g.yCenter;

            XYTextAnnotation txt = new XYTextAnnotation(label, textX, textY);
            txt.setFont(new Font("SansSerif", Font.BOLD, 10));
            txt.setPaint(Color.BLACK);
            txt.setTextAnchor(org.jfree.chart.ui.TextAnchor.CENTER);
            plot.addAnnotation(txt);
        }

        // ---- legend items (color -> motif group) ----
        LegendItemCollection legendItems = new LegendItemCollection();
        Rectangle2D rect = new Rectangle2D.Double(-4, -4, 8, 8);

        for (String group : usedGroups) {
            Color c = colorByGroup.get(group);
            legendItems.add(new LegendItem(group, null, null, null, rect, c));
        }
        plot.setFixedLegendItems(legendItems);

        var legend = chart.getLegend();
        if (legend != null) {
            legend.setPosition(RectangleEdge.RIGHT);
            legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
            legend.setVerticalAlignment(VerticalAlignment.TOP);
            legend.setItemFont(new Font("SansSerif", Font.PLAIN, 14));
            legend.setFrame(new BlockBorder(new Color(180, 180, 180)));
            legend.setBackgroundPaint(new Color(255, 255, 255, 245));
        }

        var domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        domainAxis.setLowerMargin(0.02);
        domainAxis.setUpperMargin(0.02);
        ((org.jfree.chart.axis.NumberAxis) domainAxis).setTickUnit(
                new org.jfree.chart.axis.NumberTickUnit(5)
        );

        int width  = 1600;
        int height = 600;
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 22));
        plot.getRangeAxis().setLabel("Motif tiers / site metrics (relative)");
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));

        chart.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundPaint(Color.WHITE);

        ChartUtils.saveChartAsPNG(new File(outPng), chart, width, height);
    }
    
    
 // Central motif column between Nicotine 1 and Nicotine 2,
 // in the SAME big square. Motifs fan out from the center line,
 // with width based on the max number of motifs at any single site.
    private static void drawCentralMotifColumn(
            XYPlot plot,
            Nicotine n1,
            Nicotine n2,
            double centerX,     // motif axis x
            double leftAnchorX, // right edge of left scale bar (100% Phosph)
            double rightAnchorX,// left edge of right scale bar (100% Phosph)
            double y0, double y1,
            int maxStack) {
    	if (maxStack < 1) maxStack = 1;

    	double vertPad  = (y1 - y0) * 0.07;
    	double innerY0  = y0 + vertPad;
    	double innerY1  = y1 - vertPad;
    	double innerH   = innerY1 - innerY0;

    	// distance from motif axis to the 100% Phosph edges
    	double distLeft  = Math.abs(leftAnchorX  - centerX);
    	double distRight = Math.abs(rightAnchorX - centerX);

    	// add a little breathing room so boxes don't touch the scales
    	double bufferFrac = 0.08;   // 8% buffer; tweak 0.05–0.10 as you like
    	distLeft  *= (1.0 - bufferFrac);
    	distRight *= (1.0 - bufferFrac);

    	double cellWidthLeft  = distLeft  / maxStack;
    	double cellWidthRight = distRight / maxStack;

    	double halfH = (y1 - y0) * 0.012;

      

         // center axis line
         XYBoxAnnotation motifAxis = new XYBoxAnnotation(
                 centerX - 0.001, innerY0,
                 centerX + 0.001, innerY1,
                 new BasicStroke(1.2f),
                 Color.BLACK,
                 Color.BLACK
         );
         plot.addAnnotation(motifAxis);

         // "Motifs" title
         double titleY = innerY1 + vertPad * 0.4;
         XYTextAnnotation title = new XYTextAnnotation("Motifs", centerX, titleY);
         title.setFont(new Font("SansSerif", Font.BOLD, 18));
         title.setTextAnchor(org.jfree.chart.ui.TextAnchor.BOTTOM_CENTER);
         plot.addAnnotation(title);

         Color motifFill = new Color(213, 230, 247); // pale blue

         // ---------- Nicotine 1 motifs (LEFT of centerX) ----------
         if (n1 != null && n1.motifLocations != null && !n1.motifLocations.isEmpty()) {

             int center1 = n1.center;
             int buffer1 = n1.buffer;
             int start1  = center1 - buffer1;
             int end1    = center1 + buffer1;
             double span1 = end1 - start1;

             if (span1 > 0) {
                 Map<Integer,Integer> countAtMid = new HashMap<>();

                 for (RL r : n1.motifLocations) {
                     int mid = r.midpoint;
                     double frac = (mid - start1) / span1;
                     if (frac < 0.0 || frac > 1.0) continue;

                     int idx = countAtMid.merge(mid, 1, Integer::sum) - 1;

                     double yCenter = innerY0 + frac * innerH;
                     double yBottom = yCenter - halfH;
                     double yTop    = yCenter + halfH;

                     // closest to axis first, then fan outward to the LEFT
                     double right = centerX - idx * cellWidthLeft;
                     double left  = right - cellWidthLeft;

                     Color fill = getMotifColor(r.name);

                     XYBoxAnnotation box = new XYBoxAnnotation(
                             left, yBottom, right, yTop,
                             new BasicStroke(1.0f),
                             Color.BLACK,
                             fill
                     );
                     plot.addAnnotation(box);
                 }
             }
         }

         // ---------- Nicotine 2 motifs (RIGHT of centerX) ----------
         if (n2 != null && n2.motifLocations != null && !n2.motifLocations.isEmpty()) {

             int center2 = n2.center;
             int buffer2 = n2.buffer;
             int start2  = center2 - buffer2;
             int end2    = center2 + buffer2;
             double span2 = end2 - start2;

             if (span2 > 0) {
                 Map<Integer,Integer> countAtMid = new HashMap<>();

                 for (RL r : n2.motifLocations) {
                     int mid = r.midpoint;
                     double frac = (mid - start2) / span2;
                     if (frac < 0.0 || frac > 1.0) continue;

                     int idx = countAtMid.merge(mid, 1, Integer::sum) - 1;

                     double yCenter = innerY0 + frac * innerH;
                     double yBottom = yCenter - halfH;
                     double yTop    = yCenter + halfH;

                     // closest to axis first, then fan outward to the RIGHT
                  // closest to axis first, then fan outward to the RIGHT
                     double left  = centerX + idx * cellWidthRight;
                     double right = left + cellWidthRight;

                     Color fill = getMotifColor(r.name);   // <— add this

                     XYBoxAnnotation box = new XYBoxAnnotation(
                             left, yBottom, right, yTop,
                             new BasicStroke(1.0f),
                             Color.BLACK,
                             fill
                     );
                     plot.addAnnotation(box);
                 }
             }
         }
}
 // One cluster panel (either α or β) inside a big square.
    private static void drawClusterPanel(
            XYPlot plot,
            Nicotine n1,
            Nicotine n2,
            double squareX0,
            double squareX1,
            double y0,
            double y1,
            String clusterLabel,
            List<ResInteraction> interactions) {

        if (n1 == null || n2 == null) {
            return;
        }

        // -----------------------------
        // Inside this big square:
        // two sub-columns for Nicotine 1 and 2
        // -----------------------------
        double innerGap = 0.04 * (squareX1 - squareX0);   // horizontal padding
        double colWidth = (squareX1 - squareX0 - 3 * innerGap) / 2.0;

        // pad both inner columns away from the square edges
        double pad = (squareX1 - squareX0) * INNER_PAD_FRAC;

        double n1x0 = squareX0 + pad + innerGap;
        double n1x1 = n1x0 + colWidth;

        double n2x0 = n1x1 + innerGap;
        double n2x1 = n2x0 + colWidth - pad;  // symmetrical inward shift

        double colW1  = n1x1 - n1x0;
        double colW2  = n2x1 - n2x0;
        // axis positions  **MATCH drawNicotineTrack**
        double axis1X = n1x0 + colW1 * 0.22;
        double axis2X = n2x0 + colW2 * 0.78;

        // scale labels & bar for Nicotine 1 (invertBars = false)
        double labelOffset1 = colW1 * 0.22;
        double leftLabelX1  = axis1X - labelOffset1;
        double rightLabelX1 = axis1X + labelOffset1;
        double lineStartX1  = leftLabelX1  + 0.02 * colW1;
        double lineEndX1    = rightLabelX1 - 0.02 * colW1;

        double leftAnchorX = lineEndX1;      // 100% Phosph edge (left track)

        // scale labels & bar for Nicotine 2 (invertBars = true)
        double labelOffset2 = colW2 * 0.22;
        double leftLabelX2  = axis2X - labelOffset2;
        double rightLabelX2 = axis2X + labelOffset2;
        double lineStartX2  = leftLabelX2  + 0.02 * colW2;
        double lineEndX2    = rightLabelX2 - 0.02 * colW2;

        double rightAnchorX = lineStartX2;   // 100% Phosph edge (right track)

        // Draw the two tracks (unchanged)
        drawNicotineTrack(plot, n1, n1x0, n1x1, y0, y1, n1.name, false);
        drawNicotineTrack(plot, n2, n2x0, n2x1, y0, y1, n2.name, true);

        // Motif column between Nicotine 1 and Nicotine 2
        double motifsX0 = n1x1 + innerGap * 0.5;
        double motifsX1 = n2x0 - innerGap * 0.5;
        double motifCenterX = (motifsX0 + motifsX1) / 2.0;

        int maxStack = Math.max(maxMotifsAtSite(n1), maxMotifsAtSite(n2));
        if (maxStack < 1) maxStack = 1;

        drawCentralMotifColumn(
                plot,
                n1, n2,
                motifCenterX,
                leftAnchorX,
                rightAnchorX,
                y0, y1,
                maxStack
        );

        // Cluster label
        double boxHeight   = y1 - y0;
        double labelYOffset = boxHeight * 0.03;
        double centerX     = (squareX0 + squareX1) / 2.0;
        double clusterLabelY = y1 - labelYOffset;

        XYTextAnnotation clusterAnn =
                new XYTextAnnotation(clusterLabel, centerX, clusterLabelY);
        clusterAnn.setFont(new Font("SansSerif", Font.BOLD, 16));
        clusterAnn.setTextAnchor(org.jfree.chart.ui.TextAnchor.TOP_CENTER);
        plot.addAnnotation(clusterAnn);

     // Interaction lines (if requested)
        if (interactions != null && !interactions.isEmpty()) {
            for (ResInteraction ri : interactions) {

                boolean inLeft  = residueInRange(n1, ri.site1);
                boolean inRight = residueInRange(n2, ri.site2);

                // If neither residue is actually in the plotted window, skip this line
                if (!inLeft && !inRight) {
                    continue;
                }

                double yLeft  = residueToY(n1, ri.site1, y0, y1);
                double yRight = residueToY(n2, ri.site2, y0, y1);

                XYLineAnnotation link = new XYLineAnnotation(
                        axis1X, yLeft,
                        axis2X, yRight,
                        new BasicStroke(
                                1.4f,
                                BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND,
                                1.0f,
                                new float[]{4.0f, 4.0f},
                                0.0f
                        ),
                        new Color(0, 0, 0, 120)
                );
                plot.addAnnotation(link);
            }
        }

    }

//Max number of motifs that share the same midpoint (site)
private static int maxMotifsAtSite(Nicotine n) {
  if (n == null || n.motifLocations == null || n.motifLocations.isEmpty()) {
      return 1;
  }
  Map<Integer, Integer> countByMid = new HashMap<>();
  for (RL r : n.motifLocations) {
      countByMid.merge(r.midpoint, 1, Integer::sum);
  }
  return countByMid.values().stream().mapToInt(i -> i).max().orElse(1);
}




private static Set<String> collectUsedMotifGroups(Nicotine... nics) {
    Set<String> used = new LinkedHashSet<>();
    for (Nicotine n : nics) {
        if (n == null || n.motifLocations == null) continue;
        for (RL r : n.motifLocations) {
            if (r.name != null) {
                used.add(r.name);
            }
        }
    }
    return used;
}

//General versions that take an explicit list of groups
private static List<XYBoxAnnotation> buildMotifLegendBoxes(
     double legendX, double legendStartY,
     double boxW, double boxH, double rowStepY,
     List<String> groups
) {
 List<XYBoxAnnotation> list = new ArrayList<>();
 double y = legendStartY;

 for (String group : groups) {
     Color col = getMotifColor(group);

     double x0 = legendX - boxW / 2.0;
     double x1 = legendX + boxW / 2.0;
     double y0 = y - boxH / 2.0;
     double y1 = y + boxH / 2.0;

     XYBoxAnnotation box = new XYBoxAnnotation(
             x0, y0, x1, y1,
             new BasicStroke(1.0f),
             Color.BLACK,
             new Color(col.getRed(), col.getGreen(), col.getBlue(), 220)
     );
     list.add(box);
     y -= rowStepY;
 }
 return list;
}

private static List<XYTextAnnotation> buildMotifLegendLabelsBelow(
     double textX, double legendStartY, double rowStepY,
     List<String> groups
) {
 List<XYTextAnnotation> list = new ArrayList<>();
 double y = legendStartY;

 for (String group : groups) {
     XYTextAnnotation label = new XYTextAnnotation(group, textX, y);
     label.setFont(new Font("SansSerif", Font.PLAIN, 11));
     label.setTextAnchor(org.jfree.chart.ui.TextAnchor.CENTER_LEFT);
     list.add(label);
     y -= rowStepY;
 }
 return list;
}

//Optional: no-arg convenience overloads (if you still use them elsewhere)
private static List<XYBoxAnnotation> buildMotifLegendBoxes(
     double legendX, double legendStartY,
     double boxW, double boxH, double rowStepY
) {
 return buildMotifLegendBoxes(
         legendX, legendStartY, boxW, boxH, rowStepY,
         new ArrayList<>(MOTIF_COLORS.keySet())
 );
}

private static List<XYTextAnnotation> buildMotifLegendLabelsBelow(
     double textX, double legendStartY, double rowStepY
) {
 return buildMotifLegendLabelsBelow(
         textX, legendStartY, rowStepY,
         new ArrayList<>(MOTIF_COLORS.keySet())
 );
}

private static boolean residueInRange(Nicotine nic, int residue) {
    if (nic == null) return false;

    int center = nic.center;
    int buffer = nic.buffer;
    int start  = center - buffer;
    int end    = center + buffer;

    return residue >= start && residue <= end;
}

}



