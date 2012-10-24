package edu.rice.rubis.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import voldemort.client.StoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.client.protocol.admin.AdminClientConfig;
import voldemort.utils.ByteArray;


/** 
 * Add a new user in the database 
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class RegisterUser extends RubisHttpServlet
{
  private UserTransaction utx = null;
  

  public int getPoolSize()
  {
    return Config.RegisterUserPoolSize;
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

	factory.close();
	 
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
    sp.printHTMLheader("RUBiS ERROR: Register user");
    sp.printHTML(
      "<h2>Your registration has not been processed due to the following error :</h2><br>");
    sp.printHTML(errorMsg);
    sp.printHTMLfooter();


  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    PreparedStatement stmt = null;
    Connection conn = null;
    
    String firstname = "",
    lastname = "",
    nickname = "",
    email = "",
    password = "";
    int regionId =0;
    int userId = -1;
    String creationDate, region;
    AdminClient adminClient = null;
    String regionName = "regions";
    String userStore = "users";
    String uniqueuserIdstore = "ids";
    StoreClient<String, Object> client = null;
    Iterator<ByteArray> iterator = null;
    StoreClient<String, String> uniqueclient = null;
    StoreClientFactory factory = null;
    int nodeId = 0;
    List<Integer> partitionList = new ArrayList<Integer>();
    String bootstrapUrl = "tcp://localhost:6666";
    String regionid ="";

    ServletPrinter sp = null;
    sp = new ServletPrinter(response, "RegisterUser");

    String value = request.getParameter("firstname");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a first name!<br>", sp);
      return;
    }
    else
      firstname = value;

    value = request.getParameter("lastname");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a last name!<br>", sp);
      return;
    }
    else
      lastname = value;

    value = request.getParameter("nickname");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a nick name!<br>", sp);
      return;
    }
    else
      nickname = value;

    value = request.getParameter("email");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide an email address!<br>", sp);
      return;
    }
    else
      email = value;

    value = request.getParameter("password");
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a password!<br>", sp);
      return;
    }
    else
      password = value;

    value = request.getParameter("region");
    String region_value = "" ;
    if ((value == null) || (value.equals("")))
    {
      printError("You must provide a valid region!<br>", sp);
      return;
    }

    else
    {
    	region = value;
        factory = getConnection();
	client = factory.getStoreClient(regionName); 
	uniqueclient = factory.getStoreClient(uniqueuserIdstore);
	Map<String, Object> userdetails = null;
	partitionList.add(0);
	partitionList.add(1);
	adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
	iterator = adminClient.fetchKeys(nodeId, regionName, partitionList, null, false);
	
	String bidderName="", date="";
	Map<String, Object> bidDescFinal = null;
		
      try
      {
        //conn = getConnection();
		
		 while (iterator.hasNext()) {
      	  		regionid = new String(iterator.next().get());
      	  		region_value = (String)client.getValue(regionid);
			  
			
			if((region_value.equalsIgnoreCase(region)))
		    	 break;
			
	   	 } 
		
		if (region_value.equals(null))
		{
		  printError(
		    " Region " + value + " does not exist in the database!<br>", sp);
		  //closeConnection(stmt, conn);
		  return;
		}
       
        //stmt.close();
      }
      catch (Exception e)
      {
        printError("Failed to execute Query for region: " + e, sp);
        factory.close();
	 
        ///closeConnection(stmt, conn);
        return;
      }
    }
    // Try to create a new user
    try
    {
	  client = factory.getStoreClient(userStore);
	  /// logic 
	  iterator = adminClient.fetchKeys(nodeId, "users", partitionList, null, false);
	  boolean flag = false;
	
        while (iterator.hasNext()) {
        String keyUserId = new String(iterator.next().get());
        Versioned vc = client.get(keyUserId);
        Map<String, Object> userdesc = (Map<String, Object>)vc.getValue();
		if(((String) userdesc.get("nickname")).equalsIgnoreCase(nickname))
		{
		   
		   flag = true;
		   break;
		}
	}
		  
      if (flag)
      {
        printError("The nickname you have choosen is already taken by someone else. Please choose a new nickname.<br>", sp);
        factory.close();
	 
        //closeConnection(stmt, conn);
        return;
      }
      //stmt.close();
    }
    catch (Exception e)
    {
      printError("Failed to execute Query to check the nickname: " + e, sp);
      //closeConnection(stmt, conn);
      factory.close();
	 
      return;
    }
    try
    {
	  java.util.Date date= new java.util.Date();
	  //System.out.println(new Timestamp(date.getTime()));
	  /*ArrayList<HashMap<String, Object>> itemSold = new ArrayList<HashMap<String, Object>>();
	  HashMap<String, Object> newItem = new HashMap<String, Object>();
		String name = "kenFollet", description = "fiction", startDate = "aa", endDate = "aa",  categoryId = "books", itemId = "1";
		String now = "aa";
		Integer quantity = 2;

	  Float reservePrice = 0.0f, initialPrice = 0.0f, buyNow = 0.0f, maxBid = 0.0f;
	  */
	  
	  uniqueclient = factory.getStoreClient(uniqueuserIdstore);
	  String user_id_value = "";
	  Versioned vc =  uniqueclient.get("sequence_user_id");
	  user_id_value = (String)vc.getValue();
 	  int val = Integer.parseInt(user_id_value);
          val = val +1;
          user_id_value = Integer.toString(val);
	 

	 /* 
	 Map<String,Object> newUser = new HashMap<String,Object>();
	 ArrayList<HashMap<String, Object>> itemsBid = new ArrayList();
	  

  	        
	  	newItem.put("itemId", "1");
		newItem.put("name", name);
		newItem.put("description", description);
		newItem.put("initialPrice", initialPrice);
		newItem.put("quantity", quantity);
		newItem.put("reservePrice", reservePrice);
		newItem.put("buyNow", buyNow);
		newItem.put("startDate", startDate);
		newItem.put("endDate", endDate);
		newItem.put("userId", userId);
		newItem.put("categoryId", categoryId);
		newItem.put("sellerName", "default");
		newItem.put("maxBid", maxBid);
		newItem.put("no_of_bids", quantity);

	        itemSold.add(newItem);
	        Timestamp abc = new Timestamp(date.getTime());

		
		HashMap<String, Object> newBid = new HashMap<String, Object>();
	    	newBid.put("bidId", "1");
		newBid.put("userId", user_id_value);
		newBid.put("bidderName", "default");
		newBid.put("itemId", itemId);
		newBid.put("qty", quantity);
		newBid.put("bid", 1.2f);
		newBid.put("maxBid", maxBid);
		newBid.put("now", now);
		newBid.put("itemName", " default");
		newBid.put("initialPrice", reservePrice);
		newBid.put("quantity", quantity);
		newBid.put("reservePrice", reservePrice);
		newBid.put("buyNow", reservePrice);
		newBid.put("startDate", "default");
		newBid.put("endDate", "default ");
		newBid.put("sellerId", "default");
		newBid.put("sellerName", "default");
		newBid.put("categoryId", "default");
	        itemsBid.add(newBid);
	 
	     
	        newUser.put("firstname", firstname);
		newUser.put("lastname", lastname);
		newUser.put("nickname", nickname);
		newUser.put("password", password);
		newUser.put("email", email);
		newUser.put("now", now);
		newUser.put("regionId", regionId);
		newUser.put("creation_date", now);
		newUser.put("regionName", region_value);
		newUser.put("itemsSold", itemSold);
		newUser.put("bids", itemsBid);
		
	 // client.put(userId,newUser);	
		
	  
	  
	  */
	        ArrayList<HashMap<String, Object>> itemSold = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> newItem = new HashMap<String, Object>();
		String name = "default", description = "fiction", startDate = "default", endDate = "default", categoryId = "books", itemId = "1";
		String now = "default";
		Integer quantity = 2;

		Float reservePrice = 0.0f, initialPrice = 0.0f, buyNow = 0.0f, maxBid = 0.0f;

		newItem.put("itemId", "1");
		newItem.put("name", name);
		newItem.put("description", description);
		newItem.put("initialPrice", initialPrice);
		newItem.put("quantity", quantity);
		newItem.put("reservePrice", reservePrice);
		newItem.put("buyNow", buyNow);
		newItem.put("startDate", startDate);
		newItem.put("endDate", endDate);
		newItem.put("userId", "1");
		newItem.put("categoryId", categoryId);
		newItem.put("categoryName", "default");
		newItem.put("sellerName", "default");
		newItem.put("maxBid", maxBid);
		newItem.put("no_of_bids", quantity);
		itemSold.add(newItem);
            

		ArrayList<HashMap<String, Object>> itemsBid = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> newBid = new HashMap<String, Object>();
		newBid.put("bidId", "1");
		newBid.put("userId", "1");
		newBid.put("bidderName", "default");
		newBid.put("itemId", itemId);
		newBid.put("qty", quantity);
		newBid.put("bid", 1.2f);
		newBid.put("maxBid", maxBid);
		newBid.put("now", now);
		newBid.put("itemName", " default");
		newBid.put("initialPrice", reservePrice);
		newBid.put("quantity", quantity);
		newBid.put("reservePrice", reservePrice);
		newBid.put("buyNow", reservePrice);
		newBid.put("startDate", "default");
		newBid.put("endDate", "default ");
		newBid.put("sellerId", "default");
		newBid.put("sellerName", "default");
		newBid.put("categoryId", "default");
		itemsBid.add(newBid);
              
		
		String regionId1 = "1";
		String regionName1 = "ellendale";

		HashMap<String, Object> newUser = new HashMap<String, Object>();
		newUser.put("firstname", firstname);
		newUser.put("lastname", lastname);
		newUser.put("nickname", nickname);
		newUser.put("password", password);
		newUser.put("email", email);
		newUser.put("now", now);
		newUser.put("regionId", regionid);
		newUser.put("creation_date", now);
		newUser.put("regionName", region);
		newUser.put("rating",0);
		newUser.put("itemsSold", itemSold);
		newUser.put("bids", itemsBid);
		
		
		client.put(user_id_value, newUser);
		
		
		uniqueclient.delete("sequence_user_id"); 
		uniqueclient.put("sequence_user_id",user_id_value);
	 	factory.close();
	 
	      
    }
    catch (Exception e)
    {
      printError(
        "RUBiS internal error: User registration failed (got exception: "
          + e
          + ")<br>", sp);
      //closeConnection(stmt, conn);
      factory.close();
	 
      return;
    }
 


    sp.printHTMLheader("RUBiS: Welcome to " + nickname);
    sp.printHTML(
      "<h2>Your registration has been processed successfully</h2><br>");
    sp.printHTML("<h3>Welcome " + nickname + "</h3>");
    sp.printHTML("RUBiS has stored the following information about you:<br>");
    sp.printHTML("First Name : " + firstname + "<br>");
    sp.printHTML("Last Name  : " + lastname + "<br>");
    sp.printHTML("Nick Name  : " + nickname + "<br>");
    sp.printHTML("Email      : " + email + "<br>");
    sp.printHTML("Password   : " + password + "<br>");
    sp.printHTML("Region     : " + region + "<br>");
    sp.printHTML(
      "<br>The following information has been automatically generated by RUBiS:<br>");
    //sp.printHTML("User id       :" + user_id_value + "<br>");
    //sp.printHTML("Creation date :" + creationDate + "<br>");

    sp.printHTMLfooter();
    closeConnection(stmt, conn);
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
    factory.close();
	 
  }
}
