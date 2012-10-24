package edu.rice.rubis.servlets;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import voldemort.client.ClientConfig;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.client.protocol.admin.AdminClientConfig;
import voldemort.utils.ByteArray;
import voldemort.versioning.Versioned;


/** Builds the html page with the list of all categories and provides links to browse all
    items in a category or items in a category for a given region */
public class BrowseCategories extends RubisHttpServlet
{
  


  public int getPoolSize()
  {
    return Config.BrowseCategoriesPoolSize;
  }
  
  /**
   * Close the connection and statement.
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

  /** List all the categories in the database */
  private boolean categoryList(int regionId, int userId, PreparedStatement stmt, StoreClientFactory factory, ServletPrinter sp)
  {
    String categoryName;
    int categoryId;
    ResultSet rs = null;
   // StoreClientFactory factory = null;
    Iterator<ByteArray> iterator = null;
    AdminClient adminClient = null;
    StoreClient<String, String> client = null;
    
    // get the list of categories
    try
    {
      //stmt = conn.prepareStatement("SELECT name, id FROM categories");
      //rs = stmt.executeQuery();
    	client = factory.getStoreClient("categories");
    	int nodeId = 0;
        List<Integer> partitionList = new ArrayList<Integer>();
        partitionList.add(0);
        partitionList.add(1);
        adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
        iterator = adminClient.fetchKeys(nodeId, "categories", partitionList, null, false);
    	
    }
    catch (Exception e)
    {
      sp.printHTML("Failed to execute Query for categories list: " + e);
      //closeConnection(stmt, conn);
      return false;
    }
    try
    {
      if (false)
      {
        sp.printHTML(
          "<h2>Sorry, but there is no category available at this time. Database table is empty</h2><br>");
        //closeConnection(stmt, conn);
        return false;
      }
      else
        sp.printHTML("<h2>Currently available categories</h2><br>");
      /*
      do
      {
        categoryName = rs.getString("name");
        categoryId = rs.getInt("id");

        if (regionId != -1)
        {
          sp.printCategoryByRegion(categoryName, categoryId, regionId);
        }
        else
        {
          if (userId != -1)
            sp.printCategoryToSellItem(categoryName, categoryId, userId);
          else
            sp.printCategory(categoryName, categoryId);
        }
      }
      while (rs.next());*/

      String keycategoryId = null;
      //String categoryName = null;
      while (iterator.hasNext()) {
    	  keycategoryId = new String(iterator.next().get());
    	  int id = Integer.parseInt(keycategoryId);
    	  categoryName = client.getValue(keycategoryId);
	if (regionId != -1)
        {
          sp.printCategoryByRegion(categoryName, id, regionId);
        }
        else
        {
          if (userId != -1)
            sp.printCategoryToSellItem(categoryName, id, userId);
          else
            sp.printCategory(categoryName, id);
        }
         

 //System.out.println("Key-Value-Pair::" + categoryId + ":" + categoryName);
         // sp.printCategory(categoryName, id);
      }  
      
      
    }
    catch (Exception e)
    {
      sp.printHTML("Exception getting categories list: " + e + "<br>");
      //closeConnection(stmt, conn);
      return false;
    }
    return true;
  }

  /** Build the html page for the response */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    ServletPrinter sp = null;
    PreparedStatement stmt = null;
    Connection conn = null;
    int regionId = -1, userId = -1;
    String username = null, password = null;

    sp = new ServletPrinter(response, "BrowseCategories");
    sp.printHTMLheader("RUBiS available categories");

    username = request.getParameter("nickname");
    password = request.getParameter("password");
    String bootstrapUrl = "tcp://localhost:6666";
    StoreClientFactory factory = getConnection();
  
   // conn = getConnection();

    // Authenticate the user who want to sell items
    if ((username != null && username != "")
      || (password != null && password != ""))
    {
      Auth auth = new Auth(conn, sp);
      userId = auth.authenticate(username, password);
      if (userId == -1)
      {
        sp.printHTML(
          " You don't have an account on RUBiS!<br>You have to register first.<br>");
        sp.printHTMLfooter();
        closeConnection(stmt, conn);
        return;
      }
    }
   

    String value = request.getParameter("region");
    if ((value != null) && (!value.equals("")))
    {
      // get the region ID
      try
      {
    	/*  
        stmt = conn.prepareStatement("SELECT id FROM regions WHERE name=?");
        stmt.setString(1, value);
        ResultSet rs = stmt.executeQuery();
        if (!rs.first())
        {
          sp.printHTML(
            " Region " + value + " does not exist in the database!<br>");
          closeConnection(stmt, conn);
          return;
        }
        regionId = rs.getInt("id");
        stmt.close();
        */
    	  
    	  StoreClient<String, String> client = factory.getStoreClient("regions");
      	  int nodeId = 0;
          List<Integer> partitionList = new ArrayList<Integer>();
          partitionList.add(0);
          partitionList.add(1);
          AdminClient adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
          Iterator<ByteArray> iterator = adminClient.fetchKeys(nodeId, "regions", partitionList, null, false);
          String keyregionId = null;
          String regionName = null;
          while (iterator.hasNext()) {
        	  keyregionId = new String(iterator.next().get());
        	  regionName = client.getValue(keyregionId);
              //System.out.println("Key-Value-Pair::" + categoryId + ":" + categoryName);
        	  if(regionName.equals(value)){
        		  regionId = Integer.parseInt(keyregionId);
        		  break;
        	  }
        	 
             
          } 
        
        
      }
      catch (Exception e)
      {
        sp.printHTML("Failed to execute Query for region: " + e);
        //closeConnection(stmt, conn);
        return;
      }
    }
    /// Change userid
    boolean connAlive = categoryList(regionId, userId, stmt, factory, sp);
   // if (connAlive) {
    //    closeConnection(stmt, conn);
   // }
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
