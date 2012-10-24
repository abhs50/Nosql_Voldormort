package edu.rice.rubis.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.versioning.Versioned;
import java.util.GregorianCalendar;

/** This servlet records a bid in the database and display
 * the result of the transaction.
 * It must be called this way :
 * <pre>
 * http://..../StoreBid?itemId=aa&userId=bb&minBid=cc&maxQty=dd&bid=ee&maxBid=ff&qty=gg 
 *   where: aa is the item id 
 *          bb is the user id
 *          cc is the minimum acceptable bid for this item
 *          dd is the maximum quantity available for this item
 *          ee is the user bid
 *          ff is the maximum bid the user wants
 *          gg is the quantity asked by the user
 * </pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class StoreBid extends RubisHttpServlet
{


  public int getPoolSize()
  {
    return Config.StoreBidPoolSize;
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
    sp.printHTMLheader("RUBiS ERROR: StoreBid");
    sp.printHTML(
      "<h2>Your request has not been processed due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
  }

  /**
   * Call the <code>doPost</code> method.
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    doPost(request, response);
  }

  /**
   * Store the bid to the database and display resulting message.
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    String userId=""; // item id
    String itemId=""; // user id
    Float minBid=0.0f; // minimum acceptable bid for this item
    Float bid=0.0f; // user bid
    Float maxBid=0.0f; // maximum bid the user wants
    Integer maxQty=0; // maximum quantity available for this item
    Integer qty=0; // quantity asked by the user
    ServletPrinter sp = null;
    PreparedStatement stmt = null;
    Connection conn = null;
    StoreClient<String, Object> client = null;
    StoreClientFactory factory = getConnection();
    

    sp = new ServletPrinter(response, "StoreBid");

    /* Get and check all parameters */

    String value = request.getParameter("userId");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a user identifier !<br></h3>", sp);
      return;
    }
    else
      userId = value;//new Integer(value);

    value = request.getParameter("itemId");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide an item identifier !<br></h3>", sp);
      return;
    }
    else
      itemId = value;//new Integer(value);

    value = request.getParameter("minBid");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a minimum bid !<br></h3>", sp);
      return;
    }
    else
    {
      //Float foo 
	minBid	= new Float(value);
       //= foo.floatValue();
    }

    value = request.getParameter("bid");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a bid !<br></h3>", sp);
      return;
    }
    else
    {
      //Float foo 
	bid = new Float(value);
      //bid = foo.floatValue();
    }

    value = request.getParameter("maxBid");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a maximum bid !<br></h3>", sp);
      return;
    }
    else
    {
      //Float foo
 	 maxBid = new Float(value);
       //= foo.floatValue();
    }

    value = request.getParameter("maxQty");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a maximum quantity !<br></h3>", sp);
      return;
    }
    else
    {
      //Integer foo 
	maxQty = new Integer(value);
       //= foo.intValue();
    }

    value = request.getParameter("qty");
    if ((value == null) || (value.equals("")))
    {
      printError("<h3>You must provide a quantity !<br></h3>", sp);
      return;
    }
    else
    {
     // Integer foo 
	qty= new Integer(value);
      // = foo.intValue();
    }

    /* Check for invalid values */

    if (qty > maxQty)
    {
      printError(
        "<h3>You cannot request "
          + qty
          + " items because only "
          + maxQty
          + " are proposed !<br></h3>", sp);
      return;
    }
    if (bid < minBid)
    {
      printError(
        "<h3>Your bid of $"
          + bid
          + " is not acceptable because it is below the $"
          + minBid
          + " minimum bid !<br></h3>", sp);
      return;
    }
    if (maxBid < minBid)
    {
      printError(
        "<h3>Your maximum bid of $"
          + maxBid
          + " is not acceptable because it is below the $"
          + minBid
          + " minimum bid !<br></h3>", sp);
      return;
    }
    if (maxBid < bid)
    {
      printError(
        "<h3>Your maximum bid of $"
          + maxBid
          + " is not acceptable because it is below your current bid of $"
          + bid
          + " !<br></h3>", sp);
      return;
    }
    try
    {	
	GregorianCalendar greg;
	greg = new GregorianCalendar(); 
	
	String now = TimeManagement.dateToString(greg);	
	//now = "blah";
    	//Get the bidderName for the item
    	StoreClient<String,Object> clientUser = factory.getStoreClient("users");
    	Versioned v = clientUser.get(userId);
    	HashMap<String, Object> bidder = (HashMap<String, Object>)v.getValue();
    	String bidderName = (String)bidder.get("nickname");
    	
    	
      // update the number of bids and the max bid for the item
      //PreparedStatement update = null;
    	StoreClient<String,String> client1 = factory.getStoreClient("ids");
    	Versioned v1 = client1.get("sequence_bids_id");
    	String bidId = (String)v1.getValue();
    	String newBidId = Integer.toString(Integer.parseInt(bidId)+1);
    	  	
    	client = factory.getStoreClient("bids");
    	HashMap<String, Object> newBid = new HashMap<String, Object>();
    	newBid.put("userId", userId);
    	newBid.put("itemId", itemId);
    	newBid.put("qty", qty);
    	newBid.put("maxBid", maxBid);
    	newBid.put("now", now);
    	newBid.put("bidderName",bidderName);
	newBid.put("bid",bid);
    	client.put(newBidId, newBid);
	
	client1.delete("sequence_bids_id"); 
        client1.put("sequence_bids_id",newBidId);
	
      try
      {
    	  client = factory.getStoreClient("items");
    	  Versioned v3 = client.get(itemId);
    	  HashMap<String, Object> item = (HashMap<String, Object>)v3.getValue();
        if (!item.isEmpty())
        {
          
          int nbOfBids = (Integer)item.get("no_of_bids");
          nbOfBids++;
          float oldMaxBid = (Float)item.get("maxBid");
          if (bid > oldMaxBid)
          {
            oldMaxBid = bid;
            
            item.put("no_of_bids", nbOfBids);
            item.put("maxBid",maxBid);
            
          }
          else
          {
        	  item.put("no_of_bids", nbOfBids);
          }
          
          newBid.put("bidId", newBidId);
          
          ArrayList listOfBids = new ArrayList();
	  listOfBids = (ArrayList)item.get("bids");
          listOfBids.add(newBid);
          item.put("bids", listOfBids);
          client.put(itemId, item);
          
          //Updating user store with the bid that he/she has made
          listOfBids = (ArrayList)bidder.get("bids");
          newBid.put("itemName",(String)item.get("name"));
	  newBid.put("initialPrice",(Float)item.get("initialPrice"));
	  newBid.put("quantity",(Integer)item.get("quantity"));
	  newBid.put("reservePrice",(Float)item.get("reservePrice"));
	  newBid.put("buyNow",(Float)item.get("buyNow"));
	  newBid.put("startDate",(String)item.get("startDate"));
	  newBid.put("endDate",(String)item.get("endDate"));
	  newBid.put("sellerId",(String)item.get("userId"));
	  newBid.put("sellerName",(String)item.get("sellerName"));
	  newBid.put("categoryId",(String)item.get("categoryId"));
	  
	  listOfBids.add(newBid);
	  
          bidder.put("bids", listOfBids);
          clientUser.put(userId, bidder);

        }
        else
        {
          
          printError("Couldn't find the item.", sp);
          
          return;
        }
      }
      catch (Exception ex)
      {
        
        printError("Failed to update nb of bids and max bid: " + ex, sp);
        /*if (update != null) 
          update.close();
        closeConnection(stmt, conn);*/
        return;
      }

      sp.printHTMLheader("RUBiS: Bidding result");
      sp.printHTML(
        "<center><h2>Your bid has been successfully processed.</h2></center>\n");
      
    }
    catch (Exception e)
    {
      sp.printHTML(
        "Error while storing the bid (got exception: " + e + ")<br>");
      /*try
      {
        conn.rollback();
        closeConnection(stmt, conn);
      }
      catch (Exception se)
      {
        printError("Transaction rollback failed: " + e, sp);
      }*/
      return;
    }
    sp.printHTMLfooter();
  }

  /**
  * Clean up the connection pool.
  */
  public void destroy()
  {
    super.destroy();
  }

}
