package edu.rice.rubis.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.lang.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import voldemort.versioning.Versioned;
import voldemort.client.ClientConfig;
import voldemort.client.DefaultStoreClient;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.client.protocol.admin.AdminClientConfig;
import voldemort.utils.ByteArray;

/** 
 * Add a new item in the database 
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class RegisterItem extends RubisHttpServlet
{
  

  public int getPoolSize()
  {
    return Config.RegisterItemPoolSize;
  }

/**
 * Close both statement and connection.
 */
  
  private void closeConnection(PreparedStatement stmt, Connection conn)
  {
    try
    {
      if (stmt != null)
        stmt.close(); // close statement
      if (conn != null)
        releaseConnection(conn);
    }
    catch (Exception ignore)
    {
    }
  }

/**
 * Display an error message.
 * @param errorMsg the error message value
 */
  private void printError(String errorMsg, ServletPrinter sp)
  {
    sp.printHTMLheader("RUBiS ERROR: Register Item");
    sp.printHTML(
      "<h2>Your registration has not been processed due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
    
  }

  /** Check the values from the html register item form and create a new item */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    String name = "", description = "";
    float initialPrice, buyNow, reservePrice;
    Float stringToFloat;
    int quantity, duration;
    Integer stringToInt;
    String categoryId, userId;
    String startDate="", endDate="";
    int itemId;
    StoreClientFactory factory = null;
    
    factory = getConnection();

    ServletPrinter sp = null;
    sp = new ServletPrinter(response, "RegisterItem");

    String value = request.getParameter("name");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a name!<br>", sp);
      return;
    }
    else
      name = value;

    value = request.getParameter("description");
    if ((value == null) || (value.equals("")))
    {
      description = "No description.";
    }
    else
      description = value;

    value = request.getParameter("initialPrice");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide an initial price!<br>", sp);
      return;
    }
    else
    {
      stringToFloat = new Float(value);
      initialPrice = stringToFloat.floatValue();
    }

    value = request.getParameter("reservePrice");
    if ((value == null) || (value.equals("")))
    {
      reservePrice = 0;
    }
    else
    {
      stringToFloat = new Float(value);
      reservePrice = stringToFloat.floatValue();

    }

    value = request.getParameter("buyNow");
    if ((value == null) || (value.equals("")))
    {
      buyNow = 0;
    }
    else
    {
      stringToFloat = new Float(value);
      buyNow = stringToFloat.floatValue();
    }

    value = request.getParameter("duration");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a duration!<br>", sp);
      return;
    }
    else
    {
      stringToInt = new Integer(value);
      duration = stringToInt.intValue();
      GregorianCalendar now, later;
      now = new GregorianCalendar();
      later = TimeManagement.addDays(now, duration);
      startDate = TimeManagement.dateToString(now);
      endDate = TimeManagement.dateToString(later);
    }

    value = request.getParameter("quantity");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a quantity!<br>", sp);
      return;
    }
    else
    {
      stringToInt = new Integer(value);
      quantity = stringToInt.intValue();
    }

    //userId = new Integer(request.getParameter("userId"));
    userId = request.getParameter("userId");
    //categoryId = new Integer(request.getParameter("categoryId"));
    categoryId = (String)request.getParameter("categoryId");

    Iterator<ByteArray> iterator = null;
    AdminClient adminClient = null;
    String storeName = "items";
    String uniqueItemstore = "ids";
    StoreClient<String, Object> client = factory.getStoreClient(storeName); 
    StoreClient<String, String> uniqueclient = factory.getStoreClient(uniqueItemstore);
    
    try
    {
      /*
      conn = getConnection();
      conn.setAutoCommit(false); // faster if made inside a Tx
	  */
	  
      String bootstrapUrl = "tcp://localhost:6666";
	  int nodeId = 0;
	  List<Integer> partitionList = new ArrayList<Integer>();
	  partitionList.add(0);
	  partitionList.add(1);
	  adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
	  iterator = adminClient.fetchKeys(nodeId, "items", partitionList, null, false);
	  String item_id_value = "";

	  StoreClient<String,String> clientCat = factory.getStoreClient("categories");
           String categoryName = "";
	  categoryName = clientCat.getValue(categoryId);
	   //= (String)vc.getValue();      

	// Try to create a new item
       try
         {
	  HashMap<String,Object> newItem = new HashMap<String,Object>();
	  newItem.put("name",name);
	  newItem.put("description",description);
	  newItem.put("initialPrice",initialPrice);
	  newItem.put("quantity",quantity);
	  newItem.put("reservePrice",reservePrice);
	  newItem.put("buyNow",buyNow);
	  newItem.put("startDate",startDate);
	  newItem.put("endDate",endDate);
	  newItem.put("userId",userId);
	  newItem.put("sellerName",name);
	  newItem.put("maxBid", 0);
	  newItem.put("no_of_bids", 0);
	  newItem.put("categoryId",categoryId);
          newItem.put("categoryName",categoryName);

	  
	 ArrayList<HashMap<String, Object>> itemsBid = new ArrayList<HashMap<String, Object>>();
	 HashMap<String, Object> newBid = new HashMap<String, Object>();
	 newBid.put("bidId", "0");
	 newBid.put("userId", "0");
	 newBid.put("bidderName", "default");
	 newBid.put("itemId", "0");
	 newBid.put("qty", quantity);
	 newBid.put("bid", 0.0f);
	 newBid.put("maxBid", 0.0f);
	 newBid.put("now", "0");
	 itemsBid.add(newBid);
	 

	  //Create hashmap and a default bid
	  newItem.put("bids",itemsBid); 
	  
          
         // Get the last inserted key 
	Versioned v1 = uniqueclient.get("sequence_items_id");	 
	item_id_value = (String)v1.getValue();
	 if ( item_id_value == null ){
	  	client.put("1",newItem); //Dunno how to handle keys for items
		uniqueclient.put("sequence_items_id","1");
	 }
	 else {
		 int val = Integer.parseInt(item_id_value);
	         val = val +1;
	         item_id_value = Integer.toString(val);
		 client.put(item_id_value,newItem);
		 uniqueclient.delete("sequence_items_id"); 
	         uniqueclient.put("sequence_items_id",item_id_value);
	 }

           
       	    int i =1;
            String searchname=""; 
	    client = factory.getStoreClient("users"); 
	    HashMap<String, Object> updateuser = new HashMap<String, Object>();
	    ArrayList<HashMap<String, Object>> itemSold = new ArrayList<HashMap<String, Object>>();
	    Versioned v = client.get(userId);
	    updateuser = (HashMap<String, Object>)v.getValue();
            itemSold = (ArrayList)updateuser.get("itemsSold");
            newItem.remove("bids");
	    newItem.put("itemId",item_id_value);
            itemSold.add(newItem);         
            updateuser.remove("itemsSold");
	    updateuser.put("itemsSold",itemSold);
	    client.put(userId,updateuser);
	       

	
      }
      catch (Exception e)
      {
        //conn.rollback();
        printError(
          "RUBiS internal error: Item registration failed (got exception: "
            + e
            + ")<br>", sp);
       // closeConnection(stmt, conn);
        return;
      }
	
      // To test if the item was correctly added in the database
      try
      {
    	  HashMap<String,Object> checkItem = new HashMap<String,Object>();
	  client = factory.getStoreClient("items");
    	  Versioned vi = client.get(item_id_value); 
    	  checkItem =  (HashMap<String,Object>) vi.getValue();
        if (checkItem.isEmpty())
        {
          printError("This item does not exist in the database.", sp);
          return;
        }
        //itemId = irs.getInt("id");
        
      }
      catch (Exception e)
      {
        //conn.rollback();
        printError("Failed to execute Query for the new item: " + e, sp);
        //closeConnection(stmt, conn);
        return;
      }

      sp.printHTMLheader("RUBiS: Item to sell " + name);
      sp.printHTML("<h2>Your Item has been successfully registered.</h2><br>");
      sp.printHTML(
        "RUBiS has stored the following information about your item:<br>");
      sp.printHTML("Name         : " + name + "<br>");
      sp.printHTML("Description  : " + description + "<br>");
      sp.printHTML("Initial price: " + initialPrice + "<br>");
      sp.printHTML("ReservePrice : " + reservePrice + "<br>");
      sp.printHTML("Buy Now      : " + buyNow + "<br>");
      sp.printHTML("Quantity     : " + quantity + "<br>");
      sp.printHTML("User id      :" + userId + "<br>");
      sp.printHTML("Category id  :" + categoryId + "<br>");
      sp.printHTML("Duration     : " + duration + "<br>");
      sp.printHTML(
        "<br>The following information has been automatically generated by RUBiS:<br>");
      sp.printHTML("Start date   :" + startDate + "<br>");
      sp.printHTML("End date     :" + endDate + "<br>");
      sp.printHTML("item id      : 1");// + itemId + "<br>");
      sp.printHTMLfooter();

    }
    catch (Exception e)
    {
      sp.printHTML("Exception getting comment list: " + e + "<br>");
      /*
	  try
      {
        conn.rollback();
        closeConnection(stmt, conn);
      }
      catch (Exception se)
      {
        sp.printHTML("Transaction rollback failed: " + e + "<br>");
        closeConnection(stmt, conn);
      }
	  */
    }
  }

  /** 
   *	Call the doGet method: check the values from the html register item form 
   *	and create a new item 
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    doGet(request, response);
  }

  /**
   * Clean up the connection pool.
   */
  
  public void destroy()
  {
    super.destroy();
  }

  
}
