package webcrawler;
//import Lineindexer.src.LineIndexer;
import java.util.List;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class LocalDataCollectorController
    {
    public static void main(String[] args) throws Exception
	{
	if (args.length != 2)
	    {
	    System.out.println("Needed parameters: ");
	    System.out.println("\t rootFolder (it will contain intermediate crawl data)");
	    System.out.println("\t numberOfCralwers (number of concurrent threads)");
	    return;
	    }
	String rootFolder = args[0];
	int numberOfCrawlers = Integer.parseInt(args[1]);

	CrawlConfig config = new CrawlConfig();
	config.setCrawlStorageFolder(rootFolder);
//	config.setMaxPagesToFetch(20);
	//config.setPolitenessDelay(200);  // bhat 2 - delay
	config.setMaxDepthOfCrawling(5);
	

	PageFetcher pageFetcher = new PageFetcher(config);
	RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
	RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
	CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

	// Bhat 1
	controller.addSeed("http://www.ndtv.com/");
	controller.start(LocalDataCollectorCrawler.class, numberOfCrawlers);


	List<Object> crawlersLocalData = controller.getCrawlersLocalData();
	long totalLinks = 0;
	long totalTextSize = 0;
	int totalProcessedPages = 0;
	for (Object localData : crawlersLocalData)
	    {
	    CrawlStat stat = (CrawlStat) localData;
	    totalLinks += stat.getTotalLinks();
	    totalTextSize += stat.getTotalTextSize();
	    totalProcessedPages += stat.getTotalProcessedPages();
	    }
	System.out.println("Aggregated Statistics:");
	System.out.println("   Processed Pages: " + totalProcessedPages);
	System.out.println("   Total Links found: " + totalLinks);
	System.out.println("   Total Text Size: " + totalTextSize);
	}
    
    }
