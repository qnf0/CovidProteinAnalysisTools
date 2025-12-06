package flyhigh;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.Collectors;

public class ResInteraction {

	int site1;
	int site2;
	public ResInteraction(int site1, int site2) {
		super();
		this.site1 = site1;
		this.site2 = site2;
	}
	
	
	
	
	public static List<ResInteraction> fromCsv(String input) throws IOException{
		List<String> raw= variousfunctions.loadLines(Path.of(input));
		List<ResInteraction>out = new ArrayList<>();
		for (String r : raw) {
			String[] p = r.split(",");
			int res1 = Integer.parseInt(p[0].split(":")[1]);
			int res2 = Integer.parseInt(p[1].split(":")[1]);

			ResInteraction ri = new ResInteraction(res1,res2);
			out.add(ri);
		}
		
		return out;
	}
	public static int[] bestMatchingRange(
	        List<ResInteraction> riList,
	        int[] rangeA  // {startA, endA}
	) {

	    if (riList == null || riList.isEmpty()) {
	        return new int[]{0, 0};
	    }

	    int startA = rangeA[0];
	    int endA   = rangeA[1];
	    int len    = endA - startA;

	    // Extract all site2 positions and sort
	    List<Integer> site2s = riList.stream()
	            .map(ri -> ri.site2)
	            .sorted()
	            .collect(Collectors.toList());

	    if (site2s.isEmpty()) {
	        return new int[]{0, 0};
	    }

	    int bestStart = site2s.get(0);
	    int bestCount = -1;

	    // Try windows starting at each unique site2 as candidate left boundary
	    for (int left : site2s) {

	        int right = left + len;
	        int count = 0;

	        // Count matches inside [left, right]
	        for (int s2 : site2s) {
	            if (s2 >= left && s2 <= right) {
	                count++;
	            }
	        }

	        if (count > bestCount) {
	            bestCount = count;
	            bestStart = left;
	        }
	    }

	    return new int[]{bestStart, bestStart + len};
	}

}
