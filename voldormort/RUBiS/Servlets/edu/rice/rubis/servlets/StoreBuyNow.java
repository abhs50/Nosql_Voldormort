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

import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.UpdateAction;
import voldemort.versioning.Versioned;

/**
 * This servlet records a BuyNow in the database and display the result of the
 * transaction. It must be called this way :
 * 
 * <pre>
 * http://..../StoreBuyNow?itemId=aa&userId=bb&minBuyNow=cc&maxQty=dd&BuyNow=ee&maxBuyNow=ff&qty=gg 
 *   where: aa is the item id 
 *          bb is the user id
 *          cc is the minimum acceptable BuyNow for this item
 *          dd is the maximum quantity available for this item
 *          ee is the user BuyNow
 *          ff is the maximum BuyNow the user wants
 *          gg is the quantity asked by the user
 * </pre>
 * 
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a
 *         href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class StoreBuyNow extends RubisHttpServlet {

	public int getPoolSize() {
		return Config.StoreBuyNowPoolSize;
	}

	/**
	 * Close both statement and connection.
	 */
	private void closeConnection(PreparedStatement stmt, Connection conn) {
		try {
			if (stmt != null)
				stmt.close(); // close statement
			if (conn != null)
				releaseConnection(conn);
		} catch (Exception ignore) {
		}
	}

	/**
	 * Display an error message.
	 * 
	 * @param errorMsg
	 *            the error message value
	 */
	private void printError(String errorMsg, ServletPrinter sp) {
		sp.printHTMLheader("RUBiS ERROR: StoreBuyNow");
		sp
				.printHTML("<h2>Your request has not been processed due to the following error :</h2><br>");
		sp.printHTML(errorMsg);
		sp.printHTMLfooter();

	}

	/**
	 * Call the <code>doPost</code> method.
	 * 
	 * @param request
	 *            a <code>HttpServletRequest</code> value
	 * @param response
	 *            a <code>HttpServletResponse</code> value
	 * @exception IOException
	 *                if an error occurs
	 * @exception ServletException
	 *                if an error occurs
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		doPost(request, response);
	}

	/**
	 * Store the BuyNow to the database and display resulting message.
	 * 
	 * @param request
	 *            a <code>HttpServletRequest</code> value
	 * @param response
	 *            a <code>HttpServletResponse</code> value
	 * @exception IOException
	 *                if an error occurs
	 * @exception ServletException
	 *                if an error occurs
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		//Integer userId; // item id
		 String itemId; // user id
		// float minBuyNow; // minimum acceptable BuyNow for this item
		// float BuyNow; // user BuyNow
		// float maxBuyNow; // maximum BuyNow the user wants
		int maxQty; // maximum quantity available for this item
		int qty; // quantity asked by the user
		String userId="";
		ServletPrinter sp = null;
		PreparedStatement stmt = null;
		Connection conn = null;
		 StoreClientFactory factory = getConnection();
		StoreClient<String, Object> client = null;

		sp = new ServletPrinter(response, "StoreBuyNow");

		/* Get and check all parameters */

		String value = request.getParameter("userId");
		if ((value == null) || (value.equals(""))) {
			printError("<h3>You must provide a user identifier !<br></h3>", sp);
			return;
		} else
			userId = value;//new Integer(value);

		value = request.getParameter("itemId");
		if ((value == null) || (value.equals(""))) {
			printError("<h3>You must provide an item identifier !<br></h3>", sp);
			return;
		} else
			itemId = value;

		value = request.getParameter("maxQty");
		if ((value == null) || (value.equals(""))) {
			printError("<h3>You must provide a maximum quantity !<br></h3>", sp);
			return;
		} else {
			Integer foo = new Integer(value);
			maxQty = foo.intValue();
		}

		value = request.getParameter("qty");
		if ((value == null) || (value.equals(""))) {
			printError("<h3>You must provide a quantity !<br></h3>", sp);
			return;
		} else {
			Integer foo = new Integer(value);
			qty = foo.intValue();
		}

		/* Check for invalid values */
		if (qty > maxQty) {
			printError("<h3>You cannot request " + qty + " items because only "
					+ maxQty + " are proposed !<br></h3>", sp);
			return;
		}
		 String now = TimeManagement.currentDateToString();
		// Try to find the Item corresponding to the Item ID
		try {
			 int quantity;

			client = factory.getStoreClient("items");
			Versioned vc = client.get(itemId);
			HashMap<String, Object> item = (HashMap<String, Object>) vc.getValue();
			if (item.isEmpty()) {
				// conn.rollback();
				printError("This item does not exist in the database.", sp);
				// closeConnection(stmt, conn);
				return;
			}
			quantity = (Integer) item.get("quantity");
			 int quantity1 = quantity - qty;
			//stmt.close();
			
			
			if (quantity == 0) {
				// Method 1: Using inbuild applyUpdate in StoreClient to make it
				// automic
				//client.applyUpdate((new UpdateAction<String, Object>() {

					//@Override
					//public void update(StoreClient<String, Object> client) {
						// TODO Auto-generated method stub
						//Versioned v = client.get(itemId);
						//HashMap<String, Object> item = (HashMap<String, Object>) v
						//		.getValue();
						item.put("quantity", quantity1);
						item.put("endDate", now);
						client.put(itemId, item);
						
						//For updating the user store for the itemSold
						String sellerId = (String)item.get("userId");
					        StoreClient<String,Object> clientUser = factory.getStoreClient("users");
						Versioned v1 = clientUser.get(sellerId);
						HashMap<String, Object> user1 = (HashMap<String, Object>)v1.getValue();
						ArrayList listOfItemsSold = (ArrayList)user1.get("itemsSold");
						HashMap<String, Object> item1 = null;
						int j=0;
						for(j=1;j<listOfItemsSold.size();j++)
						{
							item1 = (HashMap<String, Object>)listOfItemsSold.get(j);
							if(((String)item1.get("itemId")).equals(itemId)){
								break;
							}
						}
						
						item1.put("quantity", quantity1);
						item1.put("endDate", now);
						//listOfItemsSold.remove(j);
						listOfItemsSold.add(j,item1);
						user1.put("itemsSold", listOfItemsSold);
						clientUser.put(sellerId,user1);
						
					
				

			} else {
				// Method 1: Using inbuilt applyUpdate in StoreClient to make it
				// automic
				//client.applyUpdate((new UpdateAction<String, Object>() {

					//@Override
					//public void update(StoreClient<String, Object> client) {
						// TODO Auto-generated method stub
						//Versioned v = client.get(itemId);
						//Map<String, Object> item = (Map<String, Object>) v
						//		.getValue();
						item.put("quantity", quantity1);
						client.put(itemId, item);
						//Updating the user store for the itemSold
						String sellerId = (String)item.get("userId");
						 StoreClient<String,Object> clientUser = factory.getStoreClient("users");
						Versioned v1 = clientUser.get(sellerId);
						HashMap<String, Object> user1 = (HashMap<String, Object>)v1.getValue();
						ArrayList listOfItemsSold = (ArrayList)user1.get("itemsSold");
						HashMap<String, Object> item1 = null;
						int j=0;
						for(j=1;j<listOfItemsSold.size();j++)
						{
							item1 = (HashMap<String, Object>)listOfItemsSold.get(j);
							if(((String)item1.get("itemId")).equals(itemId)){
								break;
							}
						}
						
						item1.put("quantity", quantity1);
						//listOfItemsSold.remove(j);
						listOfItemsSold.add(j,item1);
						user1.put("itemsSold", listOfItemsSold);
						clientUser.put(sellerId,user1);

					//}
				//}));

			}
		} catch (Exception e) {
			sp.printHTML("Failed to execute Query for the item: " + e + "<br>");
			
			return;
		}
		try {
			StoreClient<String, String> client1 = factory.getStoreClient("ids");
			Versioned v = client1.get("sequence_buy_now_id");
			String buyNowId = (String) v.getValue();
			Integer newBuyNowId = Integer.parseInt(buyNowId) + 1;
			String strnewBuyNowId = newBuyNowId.toString();

			client = factory.getStoreClient("buynow");
			HashMap<String, Object> newBuyNow = new HashMap<String, Object>();
			newBuyNow.put("userId", userId);
			newBuyNow.put("itemId", itemId);
			newBuyNow.put("qty", qty);
			newBuyNow.put("now", now);

			client.put(strnewBuyNowId, newBuyNow);

			client1.delete("sequence_buy_now_id"); 
         		client1.put("sequence_buy_now_id",strnewBuyNowId);

			sp.printHTMLheader("RUBiS: BuyNow result");
			if (qty == 1)
				sp
						.printHTML("<center><h2>Your have successfully bought this item.</h2></center>\n");
			else
				sp
						.printHTML("<center><h2>Your have successfully bought these items.</h2></center>\n");
		} catch (Exception e) {
			sp.printHTML("Error while storing the BuyNow (got exception: " + e
					+ ")<br>");
			
			return;
		}
		// closeConnection(stmt, conn);
		sp.printHTMLfooter();
	}

	/**
	 * Clean up the connection pool.
	 */
	public void destroy() {
		super.destroy();
	}

}
