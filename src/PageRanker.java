//Name(s): Jarupong Pajakgo, Archawat Silachote, Dechapon Tongmak
//ID: 2, 3, 2
//Section: 6088107, 6088168, 6088211
import java.io.*;
import java.util.*;

/**
 * This class implements PageRank algorithm on simple graph structure.
 * Put your name(s), ID(s), and section here.
 *
 */
public class PageRanker {
	private Map<Integer, Page> pageMap = new HashMap<Integer, Page>();
	private Map<Integer, Double> newPR = new HashMap<Integer, Double>();
	private Set<Page> sinkNodes = new HashSet<Page>();
	private List<Double> perplexityTracker = new ArrayList<Double>();
	private int unchangedCount = 0;


	/**
	 * This class reads the direct graph stored in the file "inputLinkFilename" into memory.
	 * Each line in the input file should have the following format:
	 * <pid_1> <pid_2> <pid_3> .. <pid_n>
	 * 
	 * Where pid_1, pid_2, ..., pid_n are the page IDs of the page having links to page pid_1. 
	 * You can assume that a page ID is an integer.
	 */
	public void loadData(String inputLinkFilename){
		File file = new File(inputLinkFilename);
		try (FileReader fr = new FileReader(file);
			 BufferedReader br = new BufferedReader(fr))
		{
			String line;
			while ((line = br.readLine()) != null) {

				String[] splitLine = line.split(" ");
				int[] pageIDs = new int[splitLine.length];

				//Put page to pageMap
				for (int i = 0; i < splitLine.length; i++) {
					int pid = Integer.parseInt(splitLine[i]);
					pageIDs[i] = pid;
					if (!pageMap.containsKey(pid)) {
						pageMap.put(pid, new Page(pid));
					}
				}

				//Put in-link (link form other node) to corresponding page
				for (int i = 1; i < pageIDs.length; i++) {
					Page page = pageMap.get(pageIDs[i]);
					pageMap.get(pageIDs[0]).getInLinks().add(page);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * This method will be called after the graph is loaded into the memory.
	 * This method initialize the parameters for the PageRank algorithm including
	 * setting an initial weight to each page.
	 */
	public void initialize(){
		//Initialize PageRank value for every page and
		//put out-links to corresponding page
		for (Page page: pageMap.values()) {
			page.setPageRank(1.0/pageMap.size());
			for (Page inPage : page.getInLinks()) {
					pageMap.get(inPage.getPageID()).getOutLinks().add(page);
			}
		}
		//Initialize sink nodes
		for (Page page: pageMap.values()) {
			if (page.getOutLinks().isEmpty())
				sinkNodes.add(page);
		}
	}
	
	/**
	 * Computes the perplexity of the current state of the graph. The definition
	 * of perplexity is given in the project specs.
	 */
	public double getPerplexity(){
		//calulate entropy
		double entropy = 0;
		for (Page page: pageMap.values()) {
			entropy += page.getPageRank() * (Math.log(page.getPageRank()) / Math.log(2));
		}
		entropy = -entropy;
		return Math.pow(2, entropy);
	}
	
	/**
	 * Returns true if the perplexity converges (hence, terminate the PageRank algorithm).
	 * Returns false otherwise (and PageRank algorithm continue to update the page scores). 
	 */
	public boolean isConverge(){
		double perplexity = getPerplexity();
		perplexityTracker.add(perplexity);
		//Skip first check
		if (perplexityTracker.size() > 1) {
			int currentRound = perplexityTracker.size() - 1;

			//last perplexity
			int lP = (int) Math.floor(perplexityTracker.get(currentRound-1));

			//current perplexity
			int nP = (int) Math.floor(perplexityTracker.get(currentRound));

			int diff = Math.abs(lP-nP);
			if (diff < 1) {
				//No change in unit position (difference less than 1)
				unchangedCount++;
				return unchangedCount >= 3;
			}
			else {
				unchangedCount = 0;
			}
		}
		return false;
	}
	
	/**
	 * The main method of PageRank algorithm. 
	 * Can assume that initialize() has been called before this method is invoked.
	 * While the algorithm is being run, this method should keep track of the perplexity
	 * after each iteration. 
	 * 
	 * Once the algorithm terminates, the method generates two output files.
	 * [1]	"perplexityOutFilename" lists the perplexity after each iteration on each line. 
	 * 		The output should look something like:
	 *  	
	 *  	183811
	 *  	79669.9
	 *  	86267.7
	 *  	72260.4
	 *  	75132.4
	 *  
	 *  Where, for example,the 183811 is the perplexity after the first iteration.
	 *
	 * [2] "prOutFilename" prints out the score for each page after the algorithm terminate.
	 * 		The output should look something like:
	 * 		
	 * 		1	0.1235
	 * 		2	0.3542
	 * 		3 	0.236
	 * 		
	 * Where, for example, 0.1235 is the PageRank score of page 1.
	 * 
	 */
	public void runPageRank(String perplexityOutFilename, String prOutFilename){
		//StringBuilder for writing result to files
		StringBuilder perplexityOut = new StringBuilder();
		StringBuilder prOut = new StringBuilder();

		///Teleportation factor
		final double d = 0.85;

		while (!isConverge()) {
			double sinkPR = 0;
			for (Page sinkPage: sinkNodes) {
				sinkPR += sinkPage.getPageRank();
			}
			for (Page p: pageMap.values()) {
				double newpr = (1-d)/pageMap.size();
				newpr += d*(sinkPR/pageMap.size());
				for (Page q: p.getInLinks()) {
					newpr += d*(q.getPageRank()/q.getOutLinks().size());
				}
				newPR.put(p.getPageID(), newpr);
			}
			for (Page p: pageMap.values()) {
				int pid = p.getPageID();
				Double newpr = newPR.get(pid);
				p.setPageRank(newpr);
			}
		}

		//Append String for file writing
		for (int i = 1; i < perplexityTracker.size(); i++) {
			perplexityOut.append(perplexityTracker.get(i)).append(System.lineSeparator());
		}

		for (Integer pid: pageMap.keySet()) {
			Page p = pageMap.get(pid);
			prOut.append(p.getPageID()).append(" ").append(p.getPageRank()).append(System.lineSeparator());
		}

		//Write String to file
		File pout = new File(perplexityOutFilename);
		File prout = new File(prOutFilename);
		try(FileWriter fwp = new FileWriter(pout);
			FileWriter fwpr = new FileWriter(prout);
			BufferedWriter bwp = new BufferedWriter(fwp);
			BufferedWriter bwpr = new BufferedWriter(fwpr))
		{
			bwp.write(perplexityOut.toString());
			bwpr.write(prOut.toString());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Return the top K page IDs, whose scores are highest.
	 */
	public Integer[] getRankedPages(int K){
		//Sort the score
		List<Page> sortedPage = new ArrayList<>(pageMap.values());
		Collections.sort(sortedPage, Collections.reverseOrder());

		//Prevent index out of bound exception for large K or small data set
		int len = Math.min(sortedPage.size(), K);

		Integer[] results = new Integer[len];
		for (int i = 0; i < len; i++) {
			results[i] = sortedPage.get(i).getPageID();
		}
		return results;
	}
	
	public static void main(String args[])
	{
	long startTime = System.currentTimeMillis();
		PageRanker pageRanker =  new PageRanker();
		pageRanker.loadData("citeseer.dat");
		pageRanker.initialize();
		pageRanker.runPageRank("perplexity.out", "pr_scores.out");
		Integer[] rankedPages = pageRanker.getRankedPages(100);
	double estimatedTime = (double)(System.currentTimeMillis() - startTime)/1000.0;
		
		System.out.println("Top 100 Pages are:\n"+Arrays.toString(rankedPages));
		System.out.println("Proccessing time: "+estimatedTime+" seconds");
	}
}

class Page implements Comparable<Page> {
	private int pageID;
	private double pageRank;
	private Set<Page> inLinks;
	private Set<Page> outLinks;

	public Page(int pid) {
		pageRank = 0;
		pageID = pid;
		inLinks = new HashSet<Page>();
		outLinks = new HashSet<Page>();
	}

	public int getPageID() {
		return pageID;
	}

	public void setPageRank(double pageRank) {
		this.pageRank = pageRank;
	}

	public double getPageRank() {
		return pageRank;
	}

	public Set<Page> getInLinks() {
		return inLinks;
	}

	public Set<Page> getOutLinks() {
		return outLinks;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Page page = (Page) o;

		return pageID == page.pageID;
	}

	@Override
	public int hashCode() {
		return pageID;
	}

	@Override
	public int compareTo(Page o) {
		return Double.compare(pageRank, o.pageRank);
	}

	@Override
	public String toString() {
		return "Page ID: " + pageID + " PR: " + pageRank;
	}
}