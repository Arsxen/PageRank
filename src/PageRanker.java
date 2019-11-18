//Name(s):
//ID
//Section
import java.io.*;
import java.util.*;
import java.util.function.Function;

/**
 * This class implements PageRank algorithm on simple graph structure.
 * Put your name(s), ID(s), and section here.
 *
 */
public class PageRanker {
	Map<Integer, Page> pageMap = new HashMap<Integer, Page>();
	Set<Page> sinkNodes = new HashSet<Page>();
	Double lastPerplexity = null;
	int unchangeCount = 0;

	List<Page> sortedPage = null;


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
				for (int i = 0; i < splitLine.length; i++) {
					int pid = Integer.parseInt(splitLine[i]);
					pageIDs[i] = pid;
					if (!pageMap.containsKey(pid)) {
						pageMap.put(pid, new Page(pid));
					}
				}
				for (int i = 1; i < pageIDs.length; i++) {
					Page page = pageMap.get(pageIDs[i]);
					pageMap.get(pageIDs[0]).getOutLinks().add(page);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println("Load Sucessful!");
	}
	
	/**
	 * This method will be called after the graph is loaded into the memory.
	 * This method initialize the parameters for the PageRank algorithm including
	 * setting an initial weight to each page.
	 */
	public void initialize(){
		for (Page page: pageMap.values()) {
			page.setPageRank(1.0/pageMap.size());
			if (page.getOutLinks().isEmpty()) {
				sinkNodes.add(page);
			}
			else {
				for (Page outPage : page.getOutLinks()) {
					pageMap.get(outPage.getPageID()).getInLinks().add(page);
				}
			}
		}
		System.out.println("Init Success");
	}
	
	/**
	 * Computes the perplexity of the current state of the graph. The definition
	 * of perplexity is given in the project specs.
	 */
	public double getPerplexity(){
		double entropySum = 0;
		for (Page page: pageMap.values()) {
			double entropy = page.getPageRank() * (Math.log(page.getPageRank()) / Math.log(2));
			entropySum += entropy;
		}
		double exponent = -entropySum;
		return Math.pow(2, exponent);
	}
	
	/**
	 * Returns true if the perplexity converges (hence, terminate the PageRank algorithm).
	 * Returns false otherwise (and PageRank algorithm continue to update the page scores). 
	 */
	public boolean isConverge(){
		double perplexity = getPerplexity();
		if (lastPerplexity != null) {
			int lP = (int) Math.floor(lastPerplexity);
			int nP = (int) Math.floor(perplexity);
			int diff = Math.abs(lP-nP);
			if (diff < 1) {
				unchangeCount++;
				if (unchangeCount >= 4) {
					return true;
				}
			}
			else {
				unchangeCount = 0;
			}
		}
//		System.out.println("Not Coverges Last: " + (lastPerplexity == null? "Null":lastPerplexity) + " Current: " + perplexity);
		lastPerplexity = perplexity;
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
		StringBuilder perplexityOut = new StringBuilder();
		StringBuilder prOut = new StringBuilder();

		final double d = 0.85;
		int i=0;
		while (!isConverge()) {
			double sinkPR = 0;
			for (Page sinkPage: sinkNodes) {
				sinkPR += sinkPage.getPageRank();
			}
			for (Page p: pageMap.values()) {
				double newPR = (1-d)/pageMap.size();
				newPR += d*sinkPR/pageMap.size();
				for (Page q: p.getInLinks()) {
					newPR += d*q.getPageRank()/q.getOutLinks().size();
				}
				p.setPageRank(newPR);
			}
			perplexityOut.append(getPerplexity()).append(System.lineSeparator());
		}
		sortedPage = new ArrayList<>(pageMap.values());
		Collections.sort(sortedPage);
		for (Integer pid: pageMap.keySet()) {
			Page p = pageMap.get(pid);
			prOut.append(p.getPageID()).append(" ").append(p.getPageRank()).append(System.lineSeparator());
		}

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
		pageRanker.loadData("test.dat");
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
		return -Double.compare(pageRank, o.pageRank);
	}
}