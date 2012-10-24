package edu.rice.rubis.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.versioning.Versioned;

/** This servlets display the page allowing a user to buy an item
 * It must be called this way :
 * <pre>
 * http://..../BuyNow?itemId=xx&nickname=yy&password=zz
 *    where xx is the id of the item
 *          yy is the nick name of the user
 *          zz is the user password
 * </pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class BuyNow extends RubisHttpServlet
{
  

  public int getPoolSize()
  {
    return Config.BuyNowPoolSize;
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
    sp.printHTMLheader("RUBiS ERROR: Buy now");
    sp.printHTML(
      "<h2>Your request has not been processed due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();
  }

  /**
   * Authenticate the user and end the display a buy now form
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    ServletPrinter sp = null;
    
    String itemStr = request.getParameter("itemId");
    String name = request.getParameter("nickname");
    String pass = request.getParameter("password");
    sp = new ServletPrinter(response, "BuyNow");

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
    /*
    PreparedStatement stmt = null;
    
    */
    // Authenticate the user who want to bid
    //conn = getConnection();
    Connection conn = null;
    Auth auth = new Auth(conn, sp);
    int userId = auth.authenticate(name, pass);
    if (userId == -1)
    {
      sp.printHTML("name: " + name + "<br>");
      sp.printHTML("pwd: " + pass + "<br>");
      printError(" You don't have an account on RUBiS!<br>You have to register first.<br>", sp);
      //closeConnection(stmt, conn);
      return;
    }
    Integer itemId = new Integer(itemStr);
    // Try to find the Item corresponding to the Item ID
    try
    {
      /*stmt = conn.prepareStatement("SELECT * FROM items WHERE id=?");
      stmt.setInt(1, itemId.intValue());
      ResultSet irs = stmt.executeQuery();
      if (!irs.first())
      {
        printError("This item does not exist in the database.", sp);
        closeConnection(stmt, conn);
        return;
      }
		*/
      StoreClientFactory factory = getConnection();
      StoreClient<String, Object> client = factory.getStoreClient("items");
      Versioned v = client.get(itemStr);
      Map<String, Object> item = (Map<String, Object>)v.getValue();
    	
      String itemName = (String)item.get("name");
      String description = (String)item.get("description");
      String startDate = (String)item.get("startDate");
      String endDate = (String)item.get("endDate");
      float buyNow = (Float)item.get("buyNow");
      int quantity = (Integer)item.get("quantity");
      String sellerId = (String)item.get("userId");
      //stmt.close();
      String sellerName = null;
      
      try
      {
    	  client = factory.getStoreClient("users");
    	  v = client.get(sellerId);
          Map<String, Object> seller = (Map<String, Object>)v.getValue();
          
    	  
    	/*stmt = conn.prepareStatement("SELECT nickname FROM users WHERE id=?");
        stmt.setInt(1, sellerId);
        ResultSet srs = stmt.executeQuery();*/
        if (seller.isEmpty())
        {
          printError("This user does not exist in the database.", sp);
          //closeConnection(stmt, conn);
          return;
        }
        sellerName = (String)seller.get("nickname");
      }
      catch (Exception s)
      {
        printError("Failed to execute Query for seller: " + s, sp);
        //closeConnection(stmt, conn);
        return;
      }
      // Display the form for buying the item
      sp.printItemDescriptionToBuyNow(
        itemId.intValue(),
        itemName,
        description,
        buyNow,
        quantity,
        Integer.parseInt(sellerId),
        sellerName,
        startDate,
        endDate,
        userId);

    }
    catch (Exception e)
    {
      printError("Failed to execute Query for item: " + e, sp);
      //closeConnection(stmt, conn);
      return;
    }
    sp.printHTMLfooter();
    //closeConnection(stmt, conn);

    // 	try
    // 	{
    // 	    stmt = conn.prepareStatement("UPDATE items SET end_date=? WHERE id=?");
    // 	    stmt.setInt(1, endDate);
    // 	    stmt.setInt(2, itemId.intValue());
    // 	    stmt.executeUpdate();
    // 	}
    // 	catch (SQLException e)
    // 	{
    // 	    printError("Failed to update the auction end date: " +e);
    // 	    return;
    // 	}
    // 	sp.printHTMLheader("Buy now");
    // 	sp.printHTML("<h2>You bought: "+item.getName()+" for $"+item.getBuyNow()+
    // 		     ".</h2><br>");

  }

  /**
   * Call the doGet method
   *
   * @param request a <code>HttpServletRequest</code> value
   * @param response a <code>HttpServletResponse</code> value
   * @exception IOException if an error occurs
   * @exception ServletException if an error occurs
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
