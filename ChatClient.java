package flyhigh;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class ChatClient {
	
	public static boolean cachedIsOn = false;
	static Map<String,String>queryToResp = new HashMap<>();
	static String cache = "/Users/quentinflattmann/ai_cache.csv";
	
	
	public static void  initCache() throws IOException {
		File fern = new File(cache);
	
				List<String>cached = variousfunctions.loadLines(Path.of(cache));
		for (String c : cached) {
			String[]p = c.split("&&&");
			if (p.length == 2) {
				queryToResp.put(p[0],p[1]);
			}
		}
	}
	
	public static String askAI(String query) {
		if (!cachedIsOn) {
			try {
				initCache();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cachedIsOn = true;
		}
		
		if (queryToResp.containsKey(query)) {
			return queryToResp.get(query);
		}
		
	    String apiKey = "INSERT KEY HEERRE";
	
	    OpenAIClient client = OpenAIOkHttpClient.builder()
	            .apiKey(apiKey)
	            .build();

	    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
	            .model(ChatModel.GPT_4_TURBO)
	            .addUserMessage(query)
	            .build();

	    ChatCompletion chat = client.chat().completions().create(params);

	    String text = chat.choices()
	            .get(0)
	            .message()
	            .content()
	            .orElse("");  // return empty string if no message content

	    queryToResp.put(query, text);
	    String caughtResult = query+"&&&"+text;
	    List<String>newList = new ArrayList<>();
	    newList.add(caughtResult);
	    try {
			variousfunctions.appendLines(cache,newList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return text;
	}
	
	public static Place findPossibleLocation(String symbol){
		System.out.println("finding poss loc");
		System.out.println(symbol);
		String query = "You are extracting genomic positions ONLY.\n"
				+ "Return EXACTLY one tuple formatted like: (chromosome,start,end)\n"
				+ "\n"
				+ "Rules:\n"
				+ "- Chromosome must be one of: 2L, 2R, 3L, 3R, X\n"
				+ "- Start and end must be numeric genomic coordinates\n"
				+ "- No words, no bullets, no explanations, no units, no comments\n"
				+ "- Output must match this pattern exactly: (X,101000,102000)\n"
				+ "\n"
				+ "If you cannot find the gene, respond with exactly: (null,null,null)\n"
				+ "\n"
				+ "Now: Find the D. melanogaster gene coordinates for:"+symbol;
		String response = askAI(query);
		boolean goodResponse = Place.validResponse(response);
		int  x = 0;
		boolean foundGood = false;
		while ((x < 10) && foundGood == false) {
			System.out.println("how can this be possible");
			x += 1; 
			response = askAI(query);
			System.out.println(response);
			goodResponse = Place.validResponse(response);
			if (goodResponse) {
				
				foundGood = true;
				
			}
		}
		if (!foundGood) {
			System.out.println("Unable to find alias after 10 attempts, exitting");
			System.exit(1);
			return null;
		}
		
		System.out.println("fdoneinding poss loc");

		return Place.fromAI(response);
	}

}