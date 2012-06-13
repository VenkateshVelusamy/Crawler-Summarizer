package ExtractionPackage;

import java.io.*;

import java.net.*;
import java.util.*;
import org.htmlparser.*;
import org.htmlparser.nodes.*;
import org.htmlparser.parserapplications.StringExtractor;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.visitors.TagFindingVisitor;
import org.jsoup.Jsoup;
import org.w3c.tidy.Tidy;
//import org.jsoup.parser.Parser;


class Tag
{
    public int sts = -1;
    public int txtLen = 0;
}

public class ExtractMainArticle
{
    private static int TXT_NODE = 1;
    private static int TXT_EMPTY_NODE = 2;    //Nodes whose textual content is not to be counted but is part of the article
    private static int TAG_NODE = 3;
    private static int DEAD_NODE = 4;

    //inTags represent the tags which are automatically reconised as Text_Nodes
    //exTags are non text tags
    //blanTags are ones which may be textnodes but we want to ignore their texual weight, ie the text in a anchor tag

    private static String[] inTags = new String[]{"font","b","p","a","br","blockquote","address","big","blink","bq","cite","credit","dfn","em","kbd","listing","li","ol","object","pre","samp","small","strong","sub","sup","var","xmp","i","blackface","code","del","ins","q","strike","s","u","tt","img"};
    private static String[] exTags = new String[]{"script","noscript","head"};
    private static String[] blankTags = new String[]{"a","br","option","style"};

    
    

    public static ArrayList<NodeList> GetArticlesBySize()    //Returns all the articles in the page sorted by size, bounded below by by half the length of the largest article
    {
        return articles;
    }

    //Returns the main article in html format it including tags
    public static String ExtractMainArticle(String webPage,String url,String title) throws Exception
    {
        String marticle = ExtractMainArticleNodes(webPage,url,title);
            //   String cleanArticle = Jsoup.parse(marticle).text();
                return marticle;
    }


    private static String url = null;
    private static String title = null;
    private static ArrayList<NodeList> articles = new ArrayList<NodeList>();
    public static String ExtractMainArticleNodes(String webPage,String url,String title) throws Exception
    {   NodeList mainArticle = new NodeList();
        ExtractMainArticle.url = url;
        ExtractMainArticle.title = title;
       //System.out.println("webpage:\n" + webPage);
        articleLen = 0;
        listArticle = new NodeList();
        articles = new ArrayList<NodeList>();
       
        webPage = GetTidyPage(webPage);
       
        if(webPage.length() == 0)
            return "";

        Parser p = new Parser();
        p.setResource(webPage);

        Node[] topNodes = p.parse(null).toNodeArray();
       
        SetTag(topNodes);
        SetCharCount(topNodes);
        SetStatus(topNodes);

        FindArticle(topNodes);
              CleanArticle();
       

        int maxLen = articleLen;
        int minLen = (int) (maxLen / 2);    //Lower Bound

        if(maxLen < 750)
            minLen = (int) (maxLen / 3);    //Deal with small articles relatively large comments
        if(maxLen < 500)
            minLen = (int) (maxLen / 4);

        minLen = Math.min(maxLen/2,Math.max(minLen,75));
        String totalarticles="";
        boolean getMoreArticles = true;
        while(getMoreArticles)    //Have The Largest Article Now Get The Rest Which Are At Least minLen
        {
            String temparticles = listArticle.toHtml();
            String cleanArticle = Jsoup.parse(temparticles).text();
            totalarticles += cleanArticle + "\n\n";

            EraseArticleLen();
            articleLen = 0;
            listArticle = new NodeList();

            FindArticle(topNodes);
            CleanArticle();

            for(int i=0;i<articles.size();i++)    //Infite Loop Not sure what causes this but it has happedned once before (out of aprox 30000 articles)
            {
                if( NodeListsMatch(listArticle,articles.get(i)) )
                {
                    getMoreArticles = false;
                    break;
                }
            }

            if(articleLen < 250)
                getMoreArticles = false;
        }
     /*  String mArticle="",main="";
        for(int i=0;i<articles.size();i++)
        {
       mArticle += (articles.get(i).toHtml()) + "\n";
      
        }*/
       // main= Jsoup.parse(mArticle).text();
       // System.out.println(main);

  /*  int minOffset = webPage.length() * 100;
      //  NodeList mainArticle = new NodeList();

        for(int i=0;i<articles.size();i++)
        {
            if( articles.get(i).size() > 0)
            {
                int offset = articles.get(i).elementAt(0).getStartPosition();

                if(offset < minOffset)
                {
                    minOffset = offset;
                    mainArticle = articles.get(i);
                }
            }
        }*/


        //Print(listArticle.toNodeArray()); //For debuging purposes

        return totalarticles;
    }

    private static void SetTag(Node[] nodes)
    {
        NodeList children;
        for(int i=0;i<nodes.length;i++)
        {
            nodes[i].setTag(new Tag());

            children = nodes[i].getChildren();

            if(children!=null)
                SetTag(children.toNodeArray());
        }
    }

    private static void SetCharCount(Node[] nodes)
    {
        Node node;
        Tag tag;

        for(int i=0;i<nodes.length;i++)
        {
            node = nodes[i];
            tag = (Tag) node.getTag();

            if(node instanceof TextNode)
            {
                tag.txtLen = GetCharCount(node.getText());
            }
            else if(node instanceof TagNode)
            {
                Node[] cNodes = node.getNodes();
                SetCharCount(cNodes);

                if( Contains(blankTags,GetTagName(node),true) )
                    tag.txtLen = 0;
                else
                {
                    for(int j=0;j<cNodes.length;j++)
                        tag.txtLen += ( (Tag) cNodes[j].getTag() ).txtLen;
                }
            }
        }
     }

    private static void SetStatus(Node[] nodes)
    {
        String tagName;
        Node node;
        Tag tag;

        for(int i=0;i<nodes.length;i++)
        {
            node = nodes[i];
            tag = (Tag) node.getTag();
          
            if(node instanceof TextNode)
            {
                tag.sts = TXT_NODE;
            }
            else if(node instanceof TagNode)
            {
                tagName = GetTagName(node);

                if( Contains(exTags,tagName,true) )
                {
                    tag.sts = DEAD_NODE;
                }
                else if( Contains(inTags,tagName,true))
                {
                    tag.sts = TXT_NODE;
                }else if( Contains(blankTags,tagName,true))
                {
                    tag.sts = TXT_EMPTY_NODE;
                }else
                {
                    tag.sts = TAG_NODE;
                    SetStatus(node.getNodes());
                }
            }
            else if(node instanceof RemarkNode)
            {
                tag.sts = TXT_EMPTY_NODE;
            }
            else
            {
                tag.sts = DEAD_NODE;
            }
        }
    }

    static int articleLen = 0;
    static NodeList listArticle = new NodeList();
    private static void FindArticle(Node[] nodes)
    {
        int len = 0;
        NodeList list = new NodeList();

        Node node;
        Tag tag;

        boolean addNode;
        for(int i=0;i<nodes.length;i++)
        {
            addNode = false;

            node = nodes[i];
            tag = (Tag) node.getTag();

            if(tag.sts == TXT_NODE || tag.sts == TXT_EMPTY_NODE)
            {
                addNode = true;
            }else if(tag.sts == TAG_NODE)
            {
                Node[] nodesInner = node.getNodes();

                if(nodesInner.length>0)
                {
                    addNode = true;

                    Tag tagInner;
                    for(int j=0;j<nodesInner.length && addNode;j++)
                    {
                        tagInner = (Tag) nodesInner[j].getTag();
                        addNode = tagInner.sts == TXT_NODE || tagInner.sts == TXT_EMPTY_NODE;
                    }
                }

                if(!addNode)
                    FindArticle(nodesInner);
            }

            if(addNode)
            {
                list.add(node);
                len += tag.txtLen;
            }else
            {
                if( len > articleLen )
                {
                    listArticle = list;
                    articleLen = len;
                }

                len = 0;
                list = new NodeList();
            }
        }

        if( len > articleLen )
        {
            listArticle = list;
            articleLen = len;
        }
    }

    //Remove the headline\date trim the article, convert urls to absolute
    private static void CleanArticle() throws ParserException
    {
        TrimArticle();

        if(RemoveHeadline(title))
        {
            TrimArticle();
            if(RemoveArticleDate())
                TrimArticle();
        }else
        {
            if(RemoveArticleDate())
            {
                TrimArticle();
                if(RemoveHeadline(title))
                    TrimArticle();
            }
        }

        ConvertArticleRelToAbsURL(url);
    }

    //Remove all nodes from future scans
    private static void EraseArticleLen()
    {
        EraseNodeCharCount(listArticle.toNodeArray());
    }

    private static void EraseNodeCharCount(Node[] nodes)
    {
        Node node;
        for(int i=0;i<nodes.length;i++)
        {
            ((Tag) nodes[i].getTag()).txtLen = 0;
            EraseNodeCharCount(nodes[i].getNodes());
        }
    }

    //Attempt to remove the headline from the article
    //The arg may not actually be the headline but it helps if it is
    private static boolean RemoveHeadline(String title) throws ParserException
    {
        boolean titleRemoved = false;

        if(title!=null)
            title = title.trim();

        String[] tagNames = new String[]{"H1","H2","H3"};

        if(title!=null && title.length() != 0)
        {
            TagFindingVisitor vis = new TagFindingVisitor(tagNames);
            listArticle.visitAllNodesWith(vis);

            Node[] nodes;
            for(int i=0;i<tagNames.length && !titleRemoved;i++)
            {
                nodes = vis.getTags(i);

                for(int j=0;j<nodes.length && !titleRemoved;j++)
                {
                    if(title.equalsIgnoreCase(GetNodeText(nodes[j])))
                    {
                        listArticle.remove(nodes[j]);
                        titleRemoved = true;
                    }
                }
            }
        }

        if(!titleRemoved)
        {
            String tagName;
            Node node;
            int txtCount = 0;

            for(int j=0;j<3 && !titleRemoved && j<listArticle.size();j++)
            {
                node = listArticle.elementAt(j);

                if(node instanceof TagNode)
                {
                    tagName = ((TagNode)node).getTagName();
                        for(int i=0;i<tagNames.length && !titleRemoved;i++)
                        if(tagName.equalsIgnoreCase(tagNames[i]))
                        {
                            listArticle.remove(node);
                            titleRemoved = true;
                        }
                }

                if(!titleRemoved)
                {
                    txtCount += GetNodeText(node).length();
                    if(txtCount > 50)
                        break;
                }
            }
        }

        return titleRemoved;
    }

    //Truncates the Article removes empty tag nodes from start and end
    private static void TrimArticle()
    {
        NodeList nodesRemovedTop = new NodeList();
        NodeList nodesRemovedBottom = new NodeList();

        TrimArticle(listArticle,nodesRemovedTop,true);    //Send imgs at the very top to the bottom of the article //Done for personal reasons
        //TrimArticle(listArticle,nodesRemovedBottom,false);

        {
            String[] tagNameAddBack = new String[]{"IMG"};

            try
            {
                TagFindingVisitor vis = new TagFindingVisitor(tagNameAddBack);
                nodesRemovedBottom.visitAllNodesWith(vis);

                Node[] nodes = vis.getTags(0);

                for(int i=nodes.length-1;i>=0;i++)
                    listArticle.add(nodes[i]);
            }catch(Exception e){}

            try
            {
                TagFindingVisitor vis = new TagFindingVisitor(tagNameAddBack);
                nodesRemovedTop.visitAllNodesWith(vis);

                Node[] nodes = vis.getTags(0);

                for(int i=0;i<nodes.length;i++)
                    listArticle.add(nodes[i]);
            }catch(Exception e){}
        }
    }

    //Trims the Article removes empty tag nodes from start and end
    private static void TrimArticle(NodeList nodes,NodeList removedNodes,boolean start)
    {
        int idx;
        Node node;
        Tag tag;

        while( nodes.size() > 0 )
        {
            idx = start ? 0 : nodes.size() -1;

            node = nodes.elementAt(idx);
            tag = (Tag) node.getTag();

            if( GetNodeText(node).trim().length() == 0)
            {
                removedNodes.add(node);
                nodes.remove(idx);
            }else if( node instanceof TagNode)
            {
                NodeList children = node.getChildren();

                if(children != null)
                    TrimArticle(children,removedNodes,start);

                break;
            }else
                break;
        }
    }

    //If found removes the tag which  merely states the articles author\date
    private static boolean RemoveArticleDate()
    {
        boolean dateRemoved = false;
        try
        {
            String text;    //First Text Node
            Node node;

            if(listArticle.size() > 0)
                node = listArticle.elementAt(0);
            else
                return false;

            while(true)
            {
                if( node instanceof TextNode)
                {
                    text = ((TextNode) node).getText().trim().toLowerCase();
                    break;
                }else if(node instanceof TagNode)
                {
                    Node[] children = node.getNodes();

                    if(children!=null && children.length>0)
                        node = children[0];
                    else
                        return false;
                }else
                    return false;
            }

            int textCharLen = GetCharCount(text);

            if(textCharLen  > 12 && textCharLen  < 120)
            {
                boolean haveDate = false;
                boolean haveText = false;

                //Have Date
                {
                    ArrayList<String> listTokens = new ArrayList<String>();
                    StringBuilder builder = new StringBuilder(20);
                    char[] c = text.toCharArray();

                    //Build Tokens
                    {
                        for(int i=0;i<c.length;i++)
                        {
                            if(Character.isLetterOrDigit(c[i]) || c[i]==':')
                                builder.append(c[i]);
                            else
                            {
                                if(builder.length()>0)
                                {
                                    listTokens.add(builder.toString());
                                    builder = new StringBuilder(20);
                                }
                            }
                        }

                        if(builder.length()>0)
                            listTokens.add(builder.toString());
                    }

                    //Remove Strings Other Than Numbers
                    {
                        String[] accept = new String[]{"Jan","Feb","Mar","Apr","Mar","Jun","Jul","Aug","Sep","Oct","Nov","Dec","January","February","March","April","May","June","July","August","September","October","November","December"};
                        String token;
                        boolean isNum;
                        boolean  remove = false;

                        for(int i=0;i<listTokens.size();i++)
                        {
                            token = listTokens.get(i);
                            isNum = false;
                            remove = true;

                            //Is Num
                            {
                                try
                                {
                                    Integer.parseInt(token);
                                    isNum = true;
                                }catch(Exception e){}
                            }

                            if(!isNum)
                            {
                                for(int j=0;j<accept.length && remove;j++)
                                {
                                    remove = !token.equalsIgnoreCase(accept[j]);

                                    if(!remove)
                                        listTokens.set(i,String.valueOf(j+1));
                                }
                            }else
                                remove = false;

                            if(remove)
                            {
                                listTokens.remove(i);
                                i--;
                            }
                        }
                    }

                    int[] nums = new int[listTokens.size()];

                    //Fill Array
                    {
                        for(int i=0;i<nums.length;i++)
                            nums[i] = Integer.parseInt(listTokens.get(i));
                    }

                    if(nums.length == 3)
                    {
                        int[][] perm = new int[6][3];

                        //Fill Perm
                        {
                            perm[0][0] = nums[0];
                            perm[0][1] = nums[1];
                            perm[0][2] = nums[2];

                            perm[1][0] = nums[0];
                            perm[1][1] = nums[2];
                            perm[1][2] = nums[1];

                            perm[2][0] = nums[1];
                            perm[2][1] = nums[0];
                            perm[2][2] = nums[2];

                            perm[3][0] = nums[1];
                            perm[3][1] = nums[2];
                            perm[3][2] = nums[0];

                            perm[4][0] = nums[2];
                            perm[4][1] = nums[0];
                            perm[4][2] = nums[1];

                            perm[5][0] = nums[2];
                            perm[5][1] = nums[1];
                            perm[5][2] = nums[0];
                        }

                        Calendar permCal = Calendar.getInstance();
                        long millisNow = permCal.getTimeInMillis();

                        for(int i=0;i<perm.length && !haveDate;i++)
                        {
                            permCal.set(perm[i][2],perm[i][1],perm[i][0]);

                            long millisPerm = permCal.getTimeInMillis();
                            long diffDays = Math.abs(millisPerm - millisNow) / (24 * 60 * 60 * 1000);

                            if( diffDays <= (365 * 5))
                                haveDate = true;
                        }
                    }
                }

                //Have Text
                {
                    String[] keyWords = new String[]{"post","update","create"};

                    for(int i=0;i<keyWords.length && !haveText;i++)
                        haveText = text.indexOf(keyWords[i]) > -1;
                }

                if(haveDate && haveText)
                {
                    node.setText("");
                    dateRemoved = true;
                }
            }

        }catch(Exception e)
        {
            //No need to throw, this is an attempt only method
        }

        return dateRemoved;
    }

    // converts "/files/file.txt" to "www.basehost.ie/files/file.txt"
    private static void ConvertArticleRelToAbsURL(String urlStr) throws ParserException
    {
        String[] tagNames = new String[]{"A","IMG","LINK"};
        TagFindingVisitor vis = new TagFindingVisitor(tagNames);
        listArticle.visitAllNodesWith(vis);

        //Modify All <a> tags
        {
            Node[] nodes = vis.getTags(0);

            LinkTag tag;
            for(int i=0;i<nodes.length;i++)
            {
                if( nodes[i] instanceof LinkTag)
                {
                     tag = (LinkTag) nodes[i];

                     if(!tag.isJavascriptLink())
                     {
                         try
                         {
                            tag.setLink( GetAbsURI(urlStr,tag.getLink()) );
                         }catch(Exception e)
                         {
                         }
                     }
                }
            }
        }

        //Modify the rest
        {
            for(int i=0;i<tagNames.length;i++)
            {
                Node[] nodes = vis.getTags(i);

                String attrib;
                TagNode node;
                for(int j=0;j<nodes.length;j++)
                {
                    if( nodes[j] instanceof TagNode)
                    {
                         node = (TagNode) nodes[j];

                         if( (attrib = node.getAttribute("src")) != null )
                         {
                             try
                             {
                                node.setAttribute("src",GetAbsURI(urlStr,attrib));
                             }catch(Exception e)
                             {
                             }
                         }
                    }
                }
            }
        }
    }

    private static boolean Contains(String[] array,String val,boolean ignoreCase)
    {
        if(ignoreCase)
        {
            for(int i=0;i<array.length;i++)
                if( val.equalsIgnoreCase(array[i]) )
                    return true;
        }else
        {
            for(int i=0;i<array.length;i++)
                if( val.equals(array[i]) )
                    return true;
        }

        return false;
    }

    private static String GetNodeText(Node node)
    {
        StringBuilder builder = new StringBuilder();
        GetNodeText(new Node[]{node},builder);
        return builder.toString().trim();
    }

    private static void GetNodeText(Node[] nodes,StringBuilder builder)
    {
        NodeList children;
        for(int i=0;i<nodes.length;i++)
        {
            if(nodes[i] instanceof TextNode)
            {
                builder.append(nodes[i].getText()).append(" ");
            }else if(nodes[i] instanceof TagNode)
            {
                children = nodes[i].getChildren();

                if(children!=null)
                    GetNodeText(children.toNodeArray(),builder);
            }
        }
    }

    private static String GetTagStatus(Tag tag)
    {
        if(tag.sts == TXT_NODE) return "TXT_NODE";
        else if (tag.sts == TXT_EMPTY_NODE) return "TXT_EMPTY_NODE";
        else if(tag.sts == TAG_NODE) return "TAG_NODE";
        else if(tag.sts == DEAD_NODE) return "DEAD_NODE";
        else return "??????_NODE";
    }

    private static String GetTagName(Node node)
    {
        if(node instanceof TagNode)
            return ( (TagNode) node).getTagName();
        else if(node instanceof TextNode)
            return "TEXT";
        else if(node instanceof RemarkNode)
            return "REMARK";
        else
            return "????";
    }

    private static void Print(Node node)
    {
        Print(new Node[]{node});
    }

    private static void Print(Node[] nodes)
    {
        Print(nodes,"");
    }

    private static void Print(Node[] nodes,String pad)
    {
        Tag tag;
        for(int i=0;i<nodes.length;i++)
        {
            tag = (Tag) nodes[i].getTag();

            System.err.print(pad + GetTagName(nodes[i]) + " " + GetTagStatus( (Tag) nodes[i].getTag()) + " " + tag.txtLen + " ");

            if( nodes[i] instanceof TextNode)
            {
                String text = nodes[i].getText().replaceAll("\r","").replaceAll("\n","");

                if(text.length() > 500)
                    text = text.substring(0,500);

                System.err.print(text);
            }

            System.err.println();

            Print(nodes[i].getNodes(),pad + "  ");
        }
    }

    private static boolean NodeListsMatch(NodeList a,NodeList b)
    {
        if(a.size() != b.size())
            return false;

        for(int i=0;i<a.size();i++)
        {
            if( !b.contains(a.elementAt(i)) )
                return false;
        }

        return true;
    }

    private static String RemoveScriptAndClean(String webPage)
    {
        StringBufferInputStream strIn = new StringBufferInputStream(webPage);
        StringBuilder strOut = new StringBuilder(webPage.length());
        StringBuilder buffer = new StringBuilder(10);

        int idx = 0;
        boolean inScript = false;

        char[] startStr = "<script".toCharArray();
        char[] endStr = "</script>".toCharArray();

        char c;
        while( strIn.available()> 0)
        {
            c = (char) strIn.read();

            if(!inScript)
            {
                if(c==startStr[idx])
                {
                    idx++;

                    if(idx==startStr.length)
                    {
                        buffer.delete(0,buffer.length());
                        inScript = true;
                        idx = 0;
                        continue;
                    }
                }else
                    idx=0;
            }else
            {
                if(c==endStr[idx])
                {
                    idx++;

                    if(idx==endStr.length)
                    {
                        buffer.delete(0,buffer.length());
                        inScript = false;
                        idx = 0;
                        continue;
                    }
                }else
                    idx = 0;
            }


            if(!inScript)
            {
                if(idx>0)
                {
                    if(CharValid(c))
                        buffer.append(c);
                }else
                {
                    if(buffer.length()>0)
                    {
                        strOut.append(buffer);
                        buffer.delete(0,buffer.length());
                    }

                    if(CharValid(c))
                        strOut.append(c);
                }
            }else
            {
                if(CharValid(c))
                    buffer.append(c);
            }
        }

        strOut.append(buffer);
        return strOut.toString();
    }

    private static boolean CharValid(char c)
    {
        return (c >=32 && c <= 126) || c=='\r' || c=='\n' || c=='\t';
    }

    private static String GetTidyPage(String webPage) throws Exception
    {
        String scriptLessWebPage = RemoveScriptAndClean(webPage);

           Tidy tidy = new Tidy();

        tidy.setQuiet(true);
        tidy.setShowWarnings(false);
        tidy.setShowErrors(0);

        tidy.setMakeClean(true);
        tidy.setMakeBare(false);
        tidy.setFixUri(true);
        tidy.setTrimEmptyElements(true);
        tidy.setWrapScriptlets(true);
        tidy.setWrapPhp(true);

        StringReader strIn = new StringReader(scriptLessWebPage);
        StringWriter strOut = new StringWriter();

        tidy.parse(strIn,strOut);

        strIn.close();
        strOut.close();

        String tidyWebPage = strOut.toString();

        if(tidyWebPage==null || tidyWebPage.length()==0)
            return scriptLessWebPage;
        else
            return tidyWebPage;
    }

    public static String GetPage(String url) throws Exception
    {
        BufferedReader buffer = new BufferedReader( new InputStreamReader(GetURLStream(url),"UTF-8") );

        StringBuilder builder = new StringBuilder();
        int byteRead;
        while ((byteRead = buffer.read()) != -1)
        {
            builder.append((char) byteRead);
        }

        buffer.close();

        return builder.toString();
    }

    public static InputStream GetURLStream(String url) throws Exception
    {
        InputStream strIn = null;

        boolean isFile = false;
        boolean isURL = false;

        if(!isURL)
        {
                try
                {
                    strIn = new FileInputStream(url);
                    isFile = true;
                }catch(Exception e){}
        }

        if(!isFile)
        {
            try
            {
                strIn = new URL(url).openStream();
                isURL = true;
            }catch(Exception e)    {}
        }

        if(!isFile && !isURL)
        {
            throw new FileNotFoundException("Can not locate as either file or url\n" + url);
        }

        return strIn;
    }

    private static int GetCharCount(String str)
    {
        int count = 0;

        for (int i = 0; i<str.length(); i++)
        {
            char c = str.charAt(i);

            if( (c>='a' && c<='z') || (c>='A' && c<='Z') )
                count++;
        }

        return count;
    }
    public static String GetAbsURI(String baseUrl,String url) throws MalformedURLException
    {
        if(url.length() > 0 && url.charAt(0)=='#')
            return url;
        else
        {
            return (new URL(new URL(baseUrl),url)).toExternalForm();
        }
    }

    public static boolean StrNullOrEmpty(String str)
    {
        return str==null || str.length()==0;
    }

    public static String NullToEmptyStr(String str)
    {
        if(str==null)
            return "";
        else
            return str;
    }
        public static void main(String[] args) throws Exception
    {
        String urltoget;
        
                   // urltoget="http://timesofindia.indiatimes.com/opinions/13876761.cms";
                  urltoget= "http://timesofindia.indiatimes.com/india/Chidambaram-to-face-trial-over-his-win-in-2009-Lok-Sabha-polls/articleshow/13890998.cms";
               // urltoget= "http://everysort.wordpress.com/2011/11/17/tambrahm-spouse/";
               // urltoget = "http://stackoverflow.com/questions/2358366/ideal-java-library-for-cleaning-html-and-escaping-malformed-fragments";
                //urltoget= "http://blogs.espncricinfo.com/btw/archives/2011/12/three_teams_vie.php";
                //urltoget = "http://blogs.espncricinfo.com/thelonghandle/";
                //urltoget="http://digg.com/news/offbeat/bradley_manning_faces_the_hacker_who_turned_him_in";
                FileOutputStream fout= new FileOutputStream("output.txt");
                
                System.err.println("Getting " + urltoget);
                System.err.println("Main Article:");
                System.err.println();

                String mainArticle = ExtractMainArticle(GetPage(urltoget),urltoget,null);
                fout.write(mainArticle.getBytes());
                System.out.println(mainArticle);
                
                
                        
    }

}