
package webcrawler;

import edu.uci.ics.crawler4j.crawler.Page;
import ExtractionPackage.ExtractMainArticle;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.regex.Pattern;

public class LocalDataCollectorCrawler extends WebCrawler
    {
    int cnt = 0;
    Pattern filters = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|tiff?|mid|mp2|mp3|mp4"
	    + "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
    CrawlStat myCrawlStat;

    public LocalDataCollectorCrawler()
	{
	myCrawlStat = new CrawlStat();
	}

    @Override
    public boolean shouldVisit(WebURL url)
	{
	String href = url.getURL().toLowerCase();
	//bhat 1
	return !filters.matcher(href).matches() && href.contains("http://www.ndtv.com/article") && !href.contains("/rss");
	}

 /*   
    
     @Override
     public void visit(Page page)
	{
	cnt++;
	System.out.println("Visited: " + page.getWebURL().getURL());
	myCrawlStat.incProcessedPages();

	if (page.getParseData() instanceof HtmlParseData)
	    {
	    HtmlParseData parseData = (HtmlParseData) page.getParseData();
	    List<WebURL> links = parseData.getOutgoingUrls();
	    myCrawlStat.incTotalLinks(links.size());
	    try
		{
		myCrawlStat.incTotalTextSize(parseData.getText().getBytes("UTF-8").length);
		Downloader dload = new Downloader();
		dload.writePageToFile(page.getWebURL().getURL(), "output" + cnt + ".txt");

		}
	    catch (UnsupportedEncodingException ignored)
		{
		}
	    }
	// We dump this crawler statistics after processing every 50 pages
	if (myCrawlStat.getTotalProcessedPages() % 50 == 0)
	    {
	    dumpMyData();
	    }
	}
	
    */
    
    @Override
    public void visit(Page page)
	{
	cnt++;
	System.out.println("Visited: " + page.getWebURL().getURL());
	myCrawlStat.incProcessedPages();

	if (page.getParseData() instanceof HtmlParseData)
	    {
	    HtmlParseData parseData = (HtmlParseData) page.getParseData();
	    List<WebURL> links = parseData.getOutgoingUrls();
	    myCrawlStat.incTotalLinks(links.size());
	    try
		{
		myCrawlStat.incTotalTextSize(parseData.getText().getBytes("UTF-8").length);
		
		// Create file 
		String filename;
//		String urldelimiter = "@@@";
		// bhat 2: specify output folder name - should preexist
		filename = "outputs/outputt" + cnt + ".txt";
		FileWriter fstream = new FileWriter(filename);
		BufferedWriter out = new BufferedWriter(fstream);
		
		
	//	out.write("\n"+page.getWebURL().getURL()+"\n\n"+urldelimiter);
		out.write("\n"+page.getWebURL().getURL()+"\n\n");
		
		String cleanArticle = ExtractMainArticle.ExtractMainArticle(parseData.getHtml(),"http://www.ndtv.com/article/india",null);
		out.write(cleanArticle);

		//Close the output stream
		out.close();
		}

    catch (Exception ignored)
		{
		ignored.printStackTrace();
		}
	    }
	// We dump this crawler statistics after processing every 50 pages
	if (myCrawlStat.getTotalProcessedPages() % 50 == 0)
	    {
	    dumpMyData();
	    }
	}
   
    
    // This function is called by controller to get the local data of this
    // crawler when job is finished
    @Override
	public Object getMyLocalData()
	{
	return myCrawlStat;
	}

    // This function is called by controller before finishing the job.
    // You can put whatever stuff you need here.
    @Override
	public void onBeforeExit()
	{
	dumpMyData();
	}

    public void dumpMyData()
	{
	int myId = getMyId();
	// This is just an example. Therefore I print on screen. You may
	// probably want to write in a text file.
	System.out.println("Crawler " + myId + "> Processed Pages: " + myCrawlStat.getTotalProcessedPages());
	System.out.println("Crawler " + myId + "> Total Links Found: " + myCrawlStat.getTotalLinks());
	System.out.println("Crawler " + myId + "> Total Text Size: " + myCrawlStat.getTotalTextSize());
	}

    }