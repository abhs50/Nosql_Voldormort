package edu.rice.rubis.servlets;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import java.sql.Timestamp;

import voldemort.versioning.Versioned;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.lang.*;
import javax.swing.plaf.synth.Region;

import voldemort.client.ClientConfig;
import voldemort.client.DefaultStoreClient;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.client.protocol.admin.AdminClientConfig;
import voldemort.utils.ByteArray;

/** This servlets displays a list of items belonging to a specific category.
 * It must be called this way :
 * <pre>
 * http://..../SearchItemsByCategory?category=xx&categoryName=yy 
 *    where xx is the category id
 *      and yy is the category name
 * /<pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class SearchItemsByCategory extends RubisHttpServlet
{


  public int getPoolSize()
  {
    return Config.SearchItemsByCategoryPoolSize;
  }

/**
 * Close both statement and connection.
 */
  private void closeConnection(PreparedStatement stmt, Connection conn)
  {
    
  }

/**
 * Display an error message.
 * @param errorMsg the error message value
 */
  private void printError(String errorMsg, ServletPrinter sp)
  {
    sp.printHTMLheader("RUBiS ERROR: Search Items By Category");
    sp.printHTML(
      "<h2>We cannot process your request due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
    
  }

  private void itemList(
    Integer categoryId,
    String categoryName,
    int page,
    int nbOfItems,
    ServletPrinter sp)
  {
    
    PreparedStatement stmt = null;
    Connection conn = null;
    
    String itemName="", endDate="";
    String itemId="";
    float maxBid;
    int nbOfBids = 0;
    ResultSet rs = null;
    

    String bootStrapUrl = "tcp://localhost:6666";
    String storeName = "items";
    StoreClientFactory factory = null;
    factory = getConnection();

    int maxThreads = 300;
    StoreClient<String, Object> client = factory.getStoreClient(storeName);

    
    //HashMap<String, String> listOfRegions = new HashMap<String, String>();
    boolean flag = false;

    // get the list of items
    try
    {
      //conn = getConnection();
      //conn.setAutoCommit(false);
    	int nodeId = 0;
	    List<Integer> partitionList = new ArrayList<Integer>();
	    partitionList.add(0);
	    partitionList.add(1);
	    AdminClient adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());

	    sp.printItemHeader();
	    Iterator<ByteArray> iterator = adminClient.fetchKeys(nodeId, "items", partitionList, null,false);
            int counter =0;
       
        ArrayList bids = new ArrayList();
        HashMap<String, Object> newbid = new HashMap<String, Object>();
	 HashMap<String, Object> value;
	 String key;
	    while (iterator.hasNext()) {
	    	flag = true;
	    	//System.out.println("Inside while");
	        key = new String(iterator.next().get());
	        //System.out.println("counter "+i+" "+key);
	        Versioned v = client.get(key);
	        value = (HashMap<String, Object>)v.getValue();
	        if(value.get("categoryId").equals(categoryId.toString())){
	        	counter++;
		        itemName = (String) value.get("name");
		        itemId = key;
		        endDate = (String) value.get("endDate");
		        maxBid =(Float)value.get("maxBid");
		        nbOfBids = (Integer)value.get("no_of_bids");
		        
		        float initialPrice = (Float)value.get("initialPrice");
		        if (maxBid < initialPrice)
		          maxBid = initialPrice;
		        
		        sp.printItem(itemName, Integer.parseInt(itemId), maxBid, nbOfBids, endDate);
		        
		        if(counter > 10)
		        	break;
		        
	        }
	        
	    }  
	    if(!flag){
	    	sp.printHTML(
            "<h2>Sorry, but there are no items available in this category !</h2>");
	    } 
	    /*
	    if (page == 0)
        {
          sp.printHTML(
            "<h2>Sorry, but there are no items available in this category !</h2>");
        }
*/
        else
        {
          sp.printHTML(
            "<h2>Sorry, but there are no more items available in this category !</h2>");
          sp.printItemHeader();
          sp.printItemFooter(
            "<a href=\"/rubis_servlets/servlet/edu.rice.rubis.servlets.SearchItemsByCategory?category="
              + categoryId
              + "&categoryName="
              + URLEncoder.encode(categoryName)
              + "&page="
              + (page - 1)
              + "&nbOfItems="
              + nbOfItems
              + "\">Previous page</a>",
            "");
        }
	    
    }  
    catch (Exception e)
    {
      sp.printHTML("Failed to executeQuery for item: " + e);
      closeConnection(stmt, conn);
      return;
    }
    
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    Integer page;
    Integer nbOfItems;
    String value = request.getParameter("category");
    ;
    Integer categoryId;
    String categoryName = request.getParameter("categoryName");

    ServletPrinter sp = null;
    sp = new ServletPrinter(response, "SearchItemsByCategory");

    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a category identifier!<br>", sp);
      return;
    }
    else
      categoryId = new Integer(value);

    value = request.getParameter("page");
    if ((value == null) || (value.equals("")))
      page = new Integer(0);
    else
      page = new Integer(value);

    value = request.getParameter("nbOfItems");
    if ((value == null) || (value.equals("")))
      nbOfItems = new Integer(25);
    else
      nbOfItems = new Integer(value);

    if (categoryName == null)
    {
      sp.printHTMLheader("RUBiS: Missing category name");
      sp.printHTML("<h2>Items in this category</h2><br><br>");
    }
    else
    {
      sp.printHTMLheader("RUBiS: Items in category " + categoryName);
      sp.printHTML("<h2>Items in category " + categoryName + "</h2><br><br>");
    }

    itemList(categoryId, categoryName, page.intValue(), nbOfItems.intValue(), sp);
    sp.printHTMLfooter();
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    doGet(request, response);
  }

  
}

