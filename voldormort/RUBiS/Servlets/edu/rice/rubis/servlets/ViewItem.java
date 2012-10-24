package edu.rice.rubis.servlets;


import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import joptsimple.OptionParser;
//import joptsimple.OptionSet;
import voldemort.client.ClientConfig;
import voldemort.client.DefaultStoreClient;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClientFactory;
import voldemort.client.protocol.RequestFormatType;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.client.protocol.admin.AdminClientConfig;
import voldemort.cluster.Node;
import voldemort.cluster.failuredetector.FailureDetector;
import voldemort.serialization.SerializationException;
import voldemort.serialization.json.EndOfFileException;
import voldemort.serialization.json.JsonReader;
import voldemort.utils.ByteArray;
import voldemort.utils.ByteUtils;
import voldemort.utils.Pair;
import voldemort.utils.Utils;
import voldemort.versioning.Versioned;
import voldemort.client.StoreClient;
import java.util.HashMap;


public class ViewItem extends RubisHttpServlet {

	public int getPoolSize()
	  {
	    return Config.ViewItemPoolSize;
	  }

	/**
	 * Close both statement and connection to the database.
	 */
	  /*private void closeConnection(PreparedStatement stmt, Connection conn)
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
*/
	/**
	 * Display an error message.
	 * @param errorMsg the error message value
	 */
	  private void printError(String errorMsg, ServletPrinter sp)
	  {
	    sp.printHTMLheader("RUBiS ERROR: View item");
	    sp.printHTML(
	      "<h2>We cannot process your request due to the following error :</h2><br>");
	    sp.printHTML(errorMsg);
	    sp.printHTMLfooter();
	    
	  }

	  public void doGet(HttpServletRequest request, HttpServletResponse response)
	    throws IOException, ServletException
	  {
	    ServletPrinter sp = null;
	    sp = new ServletPrinter(response, "ViewItem");
	    StoreClientFactory factory = getConnection();

	    String value = request.getParameter("itemId");
	    if ((value == null) || (value.equals("")))
	    {
	      printError("No item identifier received - Cannot process the request<br>", sp);
	      return;
	    }
	    //Integer itemId = new Integer(value);
		String itemId = value;
	    // get the item
	    try
	    {
	      
	    	//StringReader strReader = new StringReader(null);
	    	StoreClient<String, Object> client = factory.getStoreClient("items");
	    	
	    	Versioned v = client.get(itemId);
	    	HashMap<String, Object> itemDesc = (HashMap<String, Object>)v.getValue();
	    	
	    	
	    
	   
	    /**
	    try
	    {
	      if (!rs.first())
	      {
	        stmt.close();
	        stmt = conn.prepareStatement("SELECT * FROM old_items WHERE id=?");
	        stmt.setInt(1, itemId.intValue());
	        rs = stmt.executeQuery();
	      }
	    }
	    catch (Exception e)
	    {
	      sp.printHTML("Failed to execute Query for item in table old_items: " + e);
	      closeConnection(stmt, conn);
	      return;
	    }
	    */
	    
	      if (itemDesc.isEmpty())
	      {
	        sp.printHTML("<h2>This item does not exist!</h2>");
	        //closeConnection(stmt, conn);
	        return;
	      }
	      String itemName, endDate, startDate, description, sellerId,sellerName="";
	      float maxBid, initialPrice, buyNow, reservePrice;
	      int quantity, nbOfBids = 0;
	      maxBid =0;
	      
	      itemName = (String)itemDesc.get("name");
	      description = (String)itemDesc.get("description");
	      //DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	      //endDate = df.format((Date)itemDesc.get("endDate"));
	      //startDate = df.format((Date)itemDesc.get("startDate"));
	      endDate = (String)itemDesc.get("endDate");
	      startDate = (String)itemDesc.get("startDate");
	      initialPrice = (Float)itemDesc.get("initialPrice");
	      reservePrice = (Float)itemDesc.get("reservePrice");
	      buyNow = (Float)itemDesc.get("buyNow");
	      quantity = (Integer)itemDesc.get("quantity");
	      sellerId = (String)itemDesc.get("userId");
	     
	      maxBid = (Float)itemDesc.get("maxBid");
	      nbOfBids = (Integer)itemDesc.get("no_of_bids");
	      sellerName = (String)itemDesc.get("sellerName");
	     
	      maxBid = initialPrice;

	     	sp.printItemDescription(
	        Integer.parseInt(itemId),
	        itemName,
	        description,
	        initialPrice,
	        reservePrice,
	        buyNow,
	        quantity,
	        maxBid,
	        nbOfBids,
	        sellerName,
	        Integer.parseInt(sellerId),
	        startDate,
	        endDate,
	        -1,
	        client);
	       
	    }
	    catch (Exception e)
	    {
	      printError("Exception getting item list: " + e + "<br>", sp);
	    }
	   
	    sp.printHTMLfooter();
	  }

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
