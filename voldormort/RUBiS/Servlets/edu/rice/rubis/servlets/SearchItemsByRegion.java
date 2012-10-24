package edu.rice.rubis.servlets;

import java.io.IOException;
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

import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.client.protocol.admin.AdminClientConfig;
import voldemort.utils.ByteArray;
import voldemort.versioning.Versioned;

/**
 * Build the html page with the list of all items for given category and region.
 * 
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a
 *         href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class SearchItemsByRegion extends RubisHttpServlet {

	public int getPoolSize() {
		return Config.SearchItemsByRegionPoolSize;
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
		sp.printHTMLheader("RUBiS ERROR: SearchItemsByRegion");
		sp
				.printHTML("<h2>Your request has not been processed due to the following error :</h2><br>");
		sp.printHTML(errorMsg);
		sp.printHTMLfooter();

	}

	/** List items in the given category for the given region */
	private void itemList(String categoryId, String regionId, int page,
			int nbOfItems, ServletPrinter sp) {
		String itemName="", endDate="";
		String itemId;
		int nbOfBids = 0;
		float maxBid;
		ResultSet rs = null;
		PreparedStatement stmt = null;
		Connection conn = null;
		StoreClientFactory factory = getConnection();
		StoreClient<String, Object> client = null;
		AdminClient adminClient = null;
		Iterator<ByteArray> iterator = null;
		boolean itemsPresent = false;
		ArrayList finalItemList = new ArrayList();
		
		// get the list of items
		try {
			int nodeId = 0;
			List<Integer> partitionList = new ArrayList<Integer>();
			partitionList.add(0);
			partitionList.add(1);
			adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
			iterator = adminClient.fetchKeys(nodeId, "users",partitionList, null, false);
			client = factory.getStoreClient("users");
			
			String userId = "";
			while (iterator.hasNext()) {
				userId = new String(iterator.next().get());

				Versioned v = client.get(userId);
				HashMap<String, Object> user = (HashMap<String, Object>) v
						.getValue();
				if (((String) user.get("regionId")).equalsIgnoreCase(regionId)) {
					ArrayList<HashMap<String, Object>> listOfItems = (ArrayList<HashMap<String, Object>>) user.get("itemsSold");
					for (int k = 1; k < listOfItems.size(); k++) {
						HashMap<String, Object> item = (HashMap<String, Object>) listOfItems
								.get(k);
						if (((String) item.get("categoryId"))
								.equalsIgnoreCase(categoryId)) {
							finalItemList.add(item);
							itemsPresent = true;
							String categoryName = "";							
							categoryName = (String) item.get("categoryName");
							sp.printCategory(categoryName, Integer
									.parseInt(categoryId));
						}
					}
				}

			}

		} catch (Exception e) {
			sp.printHTML("Failed to execute Query for items in region: " + e);
			//closeConnection(stmt, conn);
			return;
		}
		try {
			if (!itemsPresent) {
				if (page == 0) {
					sp
							.printHTML("<h3>Sorry, but there is no items in this category for this region.</h3><br>");
				} else {
					sp
							.printHTML("<h3>Sorry, but there is no more items in this category for this region.</h3><br>");
					sp.printItemHeader();
					sp.printItemFooter(
							"<a href=\"edu.rice.rubis.servlets.SearchItemsByRegion?category="
									+ categoryId + "&region=" + regionId
									+ "&page=" + (page - 1) + "&nbOfItems="
									+ nbOfItems + "\">Previous page</a>", "");
				}
				
				return;
			}

			sp.printItemHeader();
			for(int l=0; l<finalItemList.size();l++)
			{
				HashMap<String, Object> currItem = (HashMap<String, Object>)finalItemList.get(l);
				itemName = (String)currItem.get("name");
				itemId = (String)currItem.get("itemId");
				endDate = (String)currItem.get("endDate");
				maxBid = (Float)currItem.get("maxBid");
				nbOfBids = (Integer)currItem.get("no_of_bids");
				float initialPrice = (Float)currItem.get("initialPrice");
				if (maxBid < initialPrice)
					maxBid = initialPrice;
				sp.printItem(itemName, Integer.parseInt(itemId), maxBid, nbOfBids, endDate);
				
			}
				
			
			if (page == 0) {
				sp.printItemFooter("",
						"<a href=\"edu.rice.rubis.servlets.SearchItemsByRegion?category="
								+ categoryId + "&region=" + regionId + "&page="
								+ (page + 1) + "&nbOfItems=" + nbOfItems
								+ "\">Next page</a>");
			} else {
				sp.printItemFooter(
						"<a href=\"edu.rice.rubis.servlets.SearchItemsByRegion?category="
								+ categoryId + "&region=" + regionId + "&page="
								+ (page - 1) + "&nbOfItems=" + nbOfItems
								+ "\">Previous page</a>",
						"<a href=\"edu.rice.rubis.servlets.SearchItemsByRegion?category="
								+ categoryId + "&region=" + regionId + "&page="
								+ (page + 1) + "&nbOfItems=" + nbOfItems
								+ "\">Next page</a>");
			}
			closeConnection(stmt, conn);
		} catch (Exception e) {
			sp.printHTML("Exception getting item list: " + e + "<br>");
			closeConnection(stmt, conn);
		}
	}

	/*
	 * Read the parameters, lookup the remote category and region and build the
	 * web page with the list of items
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// Integer categoryId, regionId;
		Integer page;
		Integer nbOfItems;

		String categoryId, regionId;

		ServletPrinter sp = null;
		sp = new ServletPrinter(response, "SearchItemsByRegion");

		String value = request.getParameter("category");
		if ((value == null) || (value.equals(""))) {
			printError("You must provide a category!<br>", sp);
			return;
		} else
			categoryId = value;// new Integer(value);

		value = request.getParameter("region");
		if ((value == null) || (value.equals(""))) {
			printError("You must provide a region!<br>", sp);
			return;
		} else
			regionId = value;// new Integer(value);

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

		sp.printHTMLheader("RUBiS: Search items by region");
		itemList(categoryId, regionId, page.intValue(), nbOfItems.intValue(),
				sp);

		sp.printHTMLfooter();
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		doGet(request, response);
	}

	/**
	 * Clean up the connection pool.
	 */
	public void destroy() {
		super.destroy();
	}
}
