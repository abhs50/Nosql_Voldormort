package edu.rice.rubis.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.Position.Bias;

import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.versioning.Versioned;

/** This servlets display the page allowing a user to put a bid
 * on an item.
 * It must be called this way :
 * <pre>
 * http://..../PutBid?itemId=xx&nickname=yy&password=zz
 *    where xx is the id of the item
 *          yy is the nick name of the user
 *          zz is the user password
 * /<pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class PutBid extends RubisHttpServlet
{
 

  public int getPoolSize()
  {
    return Config.PutBidPoolSize;
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
    sp.printHTMLheader("RUBiS ERROR: PutBid");
    sp.printHTML(
      "<h2>Your request has not been processed due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
    
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
     ServletPrinter sp = null;
     StoreClient<String, Object> client = null;
     StoreClientFactory factory = getConnection();
     
    String itemStr = request.getParameter("itemId");
    String name = request.getParameter("nickname");
    String pass = request.getParameter("password");
    sp = new ServletPrinter(response, "PubBid");

    if ((itemStr == null)
      || (itemStr.equals(""))
      || (name == null)
      || (name.equals(""))
      || (pass == null)
      || (pass.equals("")))
    {
      printError("Item id, name and password are required - Cannot process the request<br>", sp);
      return;
    }
    Integer itemId = new Integer(itemStr);
    
    PreparedStatement stmt = null;
    Connection conn = null;
    //conn = getConnection();

    // Authenticate the user who want to bid
    Auth auth = new Auth(conn, sp);
    int userId = auth.authenticate(name, pass);
    if (userId == -1)
    {
      printError(" You don't have an account on RUBiS!<br>You have to register first.<br>", sp);
      //closeConnection(stmt, conn);
      return;
    }

    // Try to find the Item corresponding to the Item ID
    String itemName, endDate, startDate, description, sellerName,sellerId;
    float maxBid, initialPrice, buyNow, reservePrice;
    int quantity, nbOfBids = 0;
    HashMap<String, Object> item;
    ArrayList<HashMap<String, Object>> bids = new ArrayList<HashMap<String, Object>>();
    //ResultSet rs = null;
    try
    {
    	client = factory.getStoreClient("items");
    	Versioned vc = client.get(itemStr);
    	item = (HashMap<String, Object>)vc.getValue();
    	    	
      /*stmt = conn.prepareStatement("SELECT * FROM items WHERE id=?");
      stmt.setInt(1, itemId.intValue());
      rs = stmt.executeQuery();*/
    }
    catch (Exception e)
    {
      printError("Failed to execute Query for item: " + e, sp);
      closeConnection(stmt, conn);
      return;
    }
    try
    {
      if (item.isEmpty())
      {
        printError("<h2>This item does not exist!</h2>", sp);
        //closeConnection(stmt, conn);
        return;
      }
      itemName = (String)item.get("name");
      description = (String)item.get("description");
      endDate = (String)item.get("endDate");
      startDate = (String)item.get("startDate");
      initialPrice = (Float)item.get("initialPrice");
      reservePrice = (Float)item.get("reservePrice");
      buyNow = (Float)item.get("buyNow");
      quantity = (Integer)item.get("quantity");
      sellerId = (String)item.get("userId");
      sellerName = (String)item.get("sellerName");
      
         if(sellerName.equals(null))    {
          printError("Unknown seller", sp);
          
          return;
        }
        

      }
      catch (Exception e)
      {
        printError("Failed to executeQuery for seller: " + e, sp);
        return;
      }
      
      try
      {
        bids = (ArrayList<HashMap<String, Object>>)item.get("bids");
        maxBid =0;
        for(int j=1;j<bids.size();j++)
        {
        	HashMap<String, Object> bidObj = (HashMap<String, Object>)bids.get(j);
        	if(((Float)bidObj.get("bid"))>maxBid)
        	{
        		maxBid = (Float)bidObj.get("bid");
        	}
        }
    	  
    	  
    	  // Get the current price (max bid)		 
        if (maxBid==0)
        	maxBid = initialPrice;
      }
      catch (Exception e)
      {
        printError("Failed to executeQuery for max bid: " + e, sp);
        
        
        return;
      }
      
      try
      {
        // Get the number of bids for this item
    	  nbOfBids = bids.size();
      
      }
      catch (Exception e)
      {
        printError("Failed to executeQuery for number of bids: " + e, sp);
        return;
      }
      sp.printItemDescription(
        itemId.intValue(),
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
        userId,
        client);
    

    
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
