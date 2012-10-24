package edu.rice.rubis.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map; /*
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
 */

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.client.protocol.admin.AdminClientConfig;
import voldemort.utils.ByteArray;
import voldemort.versioning.Versioned;

/**
 * This servlets displays the list of bids regarding an item. It must be called
 * this way :
 * 
 * <pre>
 * http://..../ViewUserInfo?itemId=xx where xx is the id of the item
 * /
 * 
 * <pre>
 */

public class ViewBidHistory extends RubisHttpServlet {

	public int getPoolSize() {
		return Config.ViewBidHistoryPoolSize;
	}

	/**
	 * Close both statement and connection to the database.
	 */
	/*
	 * private void closeConnection(PreparedStatement stmt, Connection conn) {
	 * try { if (stmt != null) stmt.close(); // close statement if (conn !=
	 * null) releaseConnection(conn); } catch (Exception ignore) { } }
	 */
	/** List the bids corresponding to an item */
	private boolean listBids(HashMap<String, Object> item,
			StoreClientFactory factory, ServletPrinter sp) {
		float bid;
		String userId;
		String keyBidId;
		String bidderName, date;
		StoreClient<String, String> client = null;
		AdminClient adminClient = null;
		Iterator<ByteArray> iterator = null;
		ArrayList listOfBids = new ArrayList();
		Map<String, Object> bidObj = null;
		// ResultSet rs = null;

		// Get the list of the user's last bids
		try {

			listOfBids = (ArrayList) item.get("bids");

			if (listOfBids.isEmpty()) {
				sp
						.printHTML("<h3>There is no bid corresponding to this item.</h3><br>");

				return false;
			}
		} catch (Exception e) {
			sp.printHTML("Exception getting bids list: " + e + "<br>");
			return false;
		}

		sp.printBidHistoryHeader();
		try {

			// Get the bids
			for (int j = 1; j < listOfBids.size(); j++) {
				bidObj = (HashMap<String, Object>) listOfBids.get(j);
				date = (String) bidObj.get("now");
				bid = (Float) bidObj.get("bid");
				userId = (String)bidObj.get("userId");
				bidderName = (String) bidObj.get("bidderName");
				sp.printBidHistory(Integer.parseInt(userId), bidderName, bid,
						date);

			}

		} catch (Exception e) {
			sp.printHTML("Exception getting bid: " + e + "<br>");
			// closeConnection(stmt, conn);
			return false;
		}
		sp.printBidHistoryFooter();
		return true;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String value = request.getParameter("itemId");
		Integer itemId;
		String itemName;
		String keyItemId;
		ServletPrinter sp = null;
     	Iterator<ByteArray> iterator = null;
		AdminClient adminClient = null;
		StoreClient<String, String> client = null;
		StoreClientFactory factory = null;
		HashMap<String, Object> item = new HashMap<String, Object>();

		sp = new ServletPrinter(response, "ViewBidHistory");

		if ((value == null) || (value.equals(""))) {
			sp.printHTMLheader("RUBiS ERROR: View bids history");
			sp.printHTML("<h3>You must provide an item identifier !<br></h3>");
			sp.printHTMLfooter();
			return;
		} else
			itemId = new Integer(value);
		if (itemId.intValue() == -1)
			sp.printHTML("ItemId is -1: this item does not exist.<br>");

		sp.printHTMLheader("RUBiS: Bid history");

		// get the item
		try {
			factory = getConnection();
			client = factory.getStoreClient("items");
			Versioned v = client.get(itemId.toString());
			item = (HashMap<String, Object>) v.getValue();
			itemName = (String) item.get("name");
		} catch (Exception e) {
			sp.printHTML("Failed to execute Query for item in table items: "
					+ e);
			
			return;
		}
		
		try {
			if (itemName == null) {
				sp.printHTML("<h2>This item does not exist!</h2>");
				
				return;
			}
			
			sp.printHTML("<center><h3>Bid History for " + itemName
					+ "<br></h3></center>");
		} catch (Exception e) {
			sp.printHTML("This item does not exist (got exception: " + e
					+ ")<br>");
			sp.printHTMLfooter();
			
			return;
		}

		boolean connAlive = listBids(item, factory, sp);
		// connAlive means we must close it. Otherwise we must NOT do a
		// double free
		/*
		 * if (connAlive) { closeConnection(stmt, conn); }
		 */
		sp.printHTMLfooter();
	}

	/**
	 * Clean up the connection pool.
	 */
	public void destroy() {
		super.destroy();
	}

}
