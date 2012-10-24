package edu.rice.rubis.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.versioning.Versioned;

/**
 * This servlets displays general information about the user loged in and about
 * his current bids or items to sell.
 * 
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a
 *         href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class AboutMe extends RubisHttpServlet {

	public int getPoolSize() {
		return Config.AboutMePoolSize;
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
		// sp.printHTMLheader("RUBiS ERROR: About me");
		sp
				.printHTML("<h3>Your request has not been processed due to the following error :</h3><br>");
		sp.printHTML(errorMsg);
		sp.printHTMLfooter();

	}

	/** List items the user is currently selling and sold in yhe past 30 days */
	private boolean listItem(HashMap<String, Object> user, ServletPrinter sp) {
		/*ResultSet currentSellings = null;
		ResultSet pastSellings = null;
		PreparedStatement currentStmt = null;
		PreparedStatement pastStmt = null;*/

		String itemName, endDate, startDate;
		float currentPrice, initialPrice, buyNow, reservePrice;
		int quantity;
		String itemId;
		ArrayList listOfItems = new ArrayList();
		// current sellings
		try {
			
			listOfItems = (ArrayList)user.get("itemsSold");

		} catch (Exception e) {
			printError(
					"Exception getting current sellings list: " + e + "<br>",
					sp);
			return false;
		}

		try {
			if (listOfItems.isEmpty()) {
				sp.printHTML("<br>");
				sp
						.printHTMLHighlighted("<h3>You are currently selling no item.</h3>");
				
			} else {
				// display current sellings
				sp.printHTML("<br>");
				sp.printSellHeader("Items you are currently selling.");
				for(int j=1;j<listOfItems.size();j++)
				{
					HashMap<String, Object> item = (HashMap<String, Object>)listOfItems.get(j);
					// Get the name of the items
					
					itemId = (String)item.get("itemId");
					itemName = (String)item.get("name");
					endDate = (String)item.get("endDate");
					startDate = (String)item.get("startDate");
					initialPrice = (Float)item
							.get("initialPrice");
					reservePrice = (Float)item
							.get("reservePrice");
					buyNow = (Float)item.get("buyNow");
					quantity = (Integer)item.get("quantity");

					currentPrice = (Float)item.get("maxBid");
					if (currentPrice < initialPrice)
						currentPrice = initialPrice;

				// display information about the item
				sp.printSell(Integer.parseInt(itemId), itemName, initialPrice, reservePrice,
						quantity, buyNow, startDate, endDate, currentPrice);
					
					
				}
				
					
				} 
				sp.printItemFooter();
			}
		 catch (Exception e) {
			printError(
					"Exception getting current items in sell: " + e + "<br>",
					sp);
			
			
		}
		 return false;
	}

			/** List items the user bought in the last 30 days */
	private boolean listBoughtItems(Integer userId, PreparedStatement stmt,
			Connection conn, ServletPrinter sp) {
		ResultSet buy = null;
		String itemName, sellerName;
		int quantity, sellerId, itemId;
		float buyNow;

		// Get the list of items the user bought
		try {
			stmt = conn
					.prepareStatement("SELECT * FROM buy_now WHERE buy_now.buyer_id=? AND TO_DAYS(NOW()) - TO_DAYS(buy_now.date)<=30");
			stmt.setInt(1, userId.intValue());
			buy = stmt.executeQuery();
			if (!buy.first()) {
				sp.printHTML("<br>");
				sp
						.printHTMLHighlighted("<h3>You didn't buy any item in the last 30 days.</h3>");
				sp.printHTML("<br>");
				stmt.close();
				return true;
			}
		} catch (Exception e) {
			printError("Exception getting bought items list: " + e + "<br>", sp);
			closeConnection(stmt, conn);
			return false;
		}

		sp.printUserBoughtItemHeader();

		try {
			do {
				itemId = buy.getInt("item_id");
				quantity = buy.getInt("qty");
				// Get the name of the items
				try {
					PreparedStatement itemStmt = null;
					try {
						ResultSet itemRS = null;
						itemStmt = conn
								.prepareStatement("SELECT * FROM items WHERE id=?");
						itemStmt.setInt(1, itemId);
						itemRS = itemStmt.executeQuery();
						if (!itemRS.first()) {
							sp.printHTML("Couldn't find bought item.<br>");
							itemStmt.close();
							return true;
						}
						itemName = itemRS.getString("name");
						sellerId = itemRS.getInt("seller");
						buyNow = itemRS.getFloat("buy_now");
						itemStmt.close();
					} catch (SQLException e) {
						sp
								.printHTML("Failed to execute Query for item (buy now): "
										+ e);
						if (itemStmt != null)
							itemStmt.close();
						closeConnection(stmt, conn);
						return false;
					}
					PreparedStatement sellerStmt = null;
					try {
						sellerStmt = conn
								.prepareStatement("SELECT nickname FROM users WHERE id=?");
						sellerStmt.setInt(1, sellerId);
						ResultSet sellerResult = sellerStmt.executeQuery();
						// Get the seller's name
						if (sellerResult.first())
							sellerName = sellerResult.getString("nickname");
						else {
							sp.printHTML("Unknown seller");
							sellerStmt.close();
							closeConnection(stmt, conn);
							return false;
						}
						sellerStmt.close();

					} catch (SQLException e) {
						sp
								.printHTML("Failed to execute Query for seller (buy now): "
										+ e);
						if (sellerStmt != null)
							sellerStmt.close();
						closeConnection(stmt, conn);
						return false;
					}
				} catch (Exception e) {
					printError("Exception getting buyNow: " + e + "<br>", sp);
					closeConnection(stmt, conn);
					return false;
				}
				// display information about the item
				sp.printUserBoughtItem(itemId, itemName, buyNow, quantity,
						sellerId, sellerName);
			} while (buy.next());
			stmt.close();
		} catch (Exception e) {
			printError("Exception getting bought items: " + e + "<br>", sp);
			closeConnection(stmt, conn);
			return false;
		}
		sp.printItemFooter();
		return true;

	}

	/** List items the user won in the last 30 days */
	/*private boolean listWonItems(HashMap<String, Object> user, ServletPrinter sp) {
		int sellerId, itemId;
		float currentPrice, initialPrice;
		String itemName, sellerName;
		ResultSet won = null;

		// Get the list of the user's won items
		try {
			stmt = conn
					.prepareStatement("SELECT item_id FROM bids, items WHERE bids.user_id=? AND bids.item_id=items.id AND TO_DAYS(NOW()) - TO_DAYS(items.end_date) < 30 GROUP BY item_id");
			stmt.setInt(1, userId.intValue());
			won = stmt.executeQuery();
			if (!won.first()) {
				sp.printHTML("<br>");
				sp
						.printHTMLHighlighted("<h3>You didn't win any item in the last 30 days.</h3>");
				sp.printHTML("<br>");
				stmt.close();
				return true;
			}
		} catch (Exception e) {
			sp.printHTML("Exception getting won items list: " + e + "<br>");
			closeConnection(stmt, conn);
			return false;
		}

		sp.printUserWonItemHeader();
		try {
			do {
				itemId = won.getInt("item_id");
				// Get the name of the items
				try {
					PreparedStatement itemStmt = null;
					try {
						ResultSet itemRS = null;
						itemStmt = conn
								.prepareStatement("SELECT * FROM items WHERE id=?");
						itemStmt.setInt(1, itemId);
						itemRS = itemStmt.executeQuery();
						if (!itemRS.first()) {
							sp.printHTML("Couldn't find won item.<br>");
							itemStmt.close();
							return true;
						}
						itemName = itemRS.getString("name");
						sellerId = itemRS.getInt("seller");
						initialPrice = itemRS.getFloat("initial_price");

						currentPrice = itemRS.getFloat("max_bid");
						if (currentPrice < initialPrice)
							currentPrice = initialPrice;

						itemStmt.close();
					} catch (SQLException e) {
						sp
								.printHTML("Failed to execute Query for item (won items): "
										+ e);
						if (itemStmt != null)
							itemStmt.close();
						closeConnection(stmt, conn);
						return false;
					}
					PreparedStatement sellerStmt = null;
					try {
						sellerStmt = conn
								.prepareStatement("SELECT nickname FROM users WHERE id=?");
						sellerStmt.setInt(1, sellerId);
						ResultSet sellerResult = sellerStmt.executeQuery();
						// Get the seller's name
						if (sellerResult.first())
							sellerName = sellerResult.getString("nickname");
						else {
							sp.printHTML("Unknown seller");
							sellerStmt.close();
							closeConnection(stmt, conn);
							return false;
						}
						sellerStmt.close();

					} catch (SQLException e) {
						sp
								.printHTML("Failed to execute Query for seller (won items): "
										+ e);
						if (sellerStmt != null)
							sellerStmt.close();
						closeConnection(stmt, conn);
						sellerStmt = null;
						return false;
					}
					// PreparedStatement currentPriceStmt = null;
					// try
					// {
					// currentPriceStmt =
					// conn.prepareStatement("SELECT MAX(bid) AS bid FROM bids WHERE item_id=?");
					// currentPriceStmt.setInt(1, itemId);
					// ResultSet currentPriceResult =
					// currentPriceStmt.executeQuery();
					// // Get the current price (max bid)
					// if (currentPriceResult.first())
					// currentPrice = currentPriceResult.getFloat("bid");
					// else
					// currentPrice = initialPrice;
					// }
					// catch (SQLException e)
					// {
					// sp.printHTML("Failed to executeQuery for current price: "
					// +e);
					// closeConnection();
					// if (currentPriceStmt!=null) currentPriceStmt.close();
					// return;
					// }
				} catch (Exception e) {
					printError("Exception getting item: " + e + "<br>", sp);
					closeConnection(stmt, conn);
					return false;
				}
				// display information about the item
				sp.printUserWonItem(itemId, itemName, currentPrice, sellerId,
						sellerName);
			} while (won.next());
			stmt.close();
		} catch (Exception e) {
			sp.printHTML("Exception getting won items: " + e + "<br>");
			closeConnection(stmt, conn);
			return false;
		}
		sp.printItemFooter();
		return true;
	}

	*//** List comments about the user *//*
	private boolean listComment(Integer userId, PreparedStatement stmt,
			Connection conn, ServletPrinter sp) {
		ResultSet rs = null;
		String date, comment;
		int authorId;

		try {
			conn.setAutoCommit(false); // faster if made inside a Tx

			// Try to find the comment corresponding to the user
			try {
				stmt = conn
						.prepareStatement("SELECT * FROM comments WHERE to_user_id=?");
				stmt.setInt(1, userId.intValue());
				rs = stmt.executeQuery();
			} catch (Exception e) {
				sp.printHTML("Failed to execute Query for list of comments: "
						+ e);
				conn.rollback();
				closeConnection(stmt, conn);
				return false;
			}
			if (!rs.first()) {
				sp.printHTML("<br>");
				sp
						.printHTMLHighlighted("<h3>There is no comment yet for this user.</h3>");
				sp.printHTML("<br>");
				conn.commit();
				stmt.close();
				return true;
			} else
				sp.printHTML("<br><hr><br><h3>Comments for this user</h3><br>");

			sp.printCommentHeader();
			// Display each comment and the name of its author
			do {
				comment = rs.getString("comment");
				date = rs.getString("date");
				authorId = rs.getInt("from_user_id");

				String authorName = "none";
				ResultSet authorRS = null;
				PreparedStatement authorStmt = null;
				try {
					authorStmt = conn
							.prepareStatement("SELECT nickname FROM users WHERE id=?");
					authorStmt.setInt(1, authorId);
					authorRS = authorStmt.executeQuery();
					if (authorRS.first())
						authorName = authorRS.getString("nickname");
					authorStmt.close();
				} catch (Exception e) {
					sp
							.printHTML("Failed to execute Query for the comment author: "
									+ e);
					conn.rollback();
					if (authorStmt != null)
						authorStmt.close();
					closeConnection(stmt, conn);
					return false;
				}
				sp.printComment(authorName, authorId, date, comment);
			} while (rs.next());
			sp.printCommentFooter();
			conn.commit();
			stmt.close();
		} catch (Exception e) {
			sp.printHTML("Exception getting comment list: " + e + "<br>");
			try {
				conn.rollback();
				closeConnection(stmt, conn);
				return false;
			} catch (Exception se) {
				sp.printHTML("Transaction rollback failed: " + e + "<br>");
				closeConnection(stmt, conn);
				return false;
			}
		}
		return true;
	}

*/	/** List items the user put a bid on in the last 30 days */
	private boolean listBids(HashMap<String, Object> user, ServletPrinter sp) // sp)
	{

		float currentPrice = 0, initialPrice = 0, maxBid = 0;
		String itemName = "", sellerName = "", startDate = "", endDate = "";
		String sellerId = "";
		int quantity = 0, itemId = 0;
		ArrayList<HashMap<String, Object>> listOfBids = new ArrayList<HashMap<String, Object>>();

		// Get the list of the user's last bids
		try {
			listOfBids = (ArrayList<HashMap<String, Object>>) user.get("bids");
			if (listOfBids.isEmpty()) {
				sp.printHTMLHighlighted("<h3>You didn't put any bid.</h3>");
				sp.printHTML("<br>");
				return true;
			}
		} catch (Exception e) {
			sp.printHTML("Exception getting bids list: " + e + "<br>");
			return false;
		}
		String username = (String) user.get("nickname");
		String password = (String) user.get("password");
		sp.printUserBidsHeader();
		try {
			for (int i = 1; i < listOfBids.size(); i++) {
				HashMap<String, Object> bid = listOfBids.get(i);

				itemName = (String) bid.get("itemName");
				initialPrice = (Float) bid.get("initialPrice");
				quantity = (Integer) bid.get("quantity");
				startDate = (String) bid.get("startDate");
				endDate = (String) bid.get("endDate");
				sellerId = (String) bid.get("sellerId");

				currentPrice = (Float) bid.get("maxBid");
				sellerName = (String) bid.get("sellerName");

				// display information about user's bids
				sp.printItemUserHasBidOn(itemId, itemName, initialPrice,
						quantity, startDate, endDate, Integer
								.parseInt(sellerId), sellerName, currentPrice,
						maxBid, username, password);
			}
		}

		catch (Exception e) {
			printError("Exception getting item: " + e + "<br>", sp);
			return false;
		}

		sp.printItemFooter();
		return true;
	}

	/**
	 * Call <code>doPost</code> method.
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
	 * Check username and password and build the web page that display the
	 * information about the loged in user.
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
		String password = null, username = null;
		String userId = null;
		ResultSet rs = null;
		PreparedStatement stmt = null;
		Connection conn = null;
		ServletPrinter sp = null;
		StoreClientFactory factory = getConnection();
		StoreClient<String, Object> client = factory.getStoreClient("users");
		HashMap<String, Object> user = new HashMap<String, Object>();

		sp = new ServletPrinter(response, "About me");

		username = request.getParameter("nickname");
		password = request.getParameter("password");
		// conn = getConnection();
		conn = null;

		// Authenticate the user
		if ((username != null && username != "")
				|| (password != null && password != "")) {
			Auth auth = new Auth(conn, sp);
			int id = auth.authenticate(username, password);
			if (id == -1) {
				printError(
						"You don't have an account on RUBiS!<br>You have to register first.<br>",
						sp);
				//closeConnection(conn);
				return;
			}
			//userId = new Integer(id);
			userId = Integer.toString(id);
		} else {
			printError(" You must provide valid username and password.", sp);
			return;
		}

		// Try to find the user corresponding to the userId
		try {
			Versioned v = client.get(userId);
			user = (HashMap<String, Object>) v.getValue();

			/*
			 * stmt = conn.prepareStatement("SELECT * FROM users WHERE id=?");
			 * stmt.setInt(1, userId.intValue()); rs = stmt.executeQuery();
			 */
		} catch (Exception e) {
			sp.printHTML("Failed to execute Query for user: " + e);
			closeConnection(stmt, conn);
			sp.printHTMLfooter();
			return;
		}
		try {
			if (user == null) // if (!user.isEmpty())
			{
				sp.printHTML("<h2>This user does not exist!</h2>");
				// closeConnection(stmt, conn);
				sp.printHTMLfooter();
				return;
			}
			String firstname = (String) user.get("firstname");// rs.getString("firstname");
			String lastname = (String) user.get("lastname");
			String nickname = (String) user.get("nickname");
			String email = (String) user.get("email");
			String date = (String) user.get("creation_date");
			int rating = (Integer) user.get("rating");
			// stmt.close();

			String result = new String();

			result = result + "<h2>Information about " + nickname + "<br></h2>";
			result = result + "Real life name : " + firstname + " " + lastname
					+ "<br>";
			result = result + "Email address  : " + email + "<br>";
			result = result + "User since     : " + date + "<br>";
			result = result + "Current rating : <b>" + rating + "</b><br>";
			sp.printHTMLheader("RUBiS: About " + nickname);
			sp.printHTML(result);

		} catch (Exception s) {
			sp.printHTML("Failed to get general information about the user: "
					+ s);
			// closeConnection(stmt, conn);
			sp.printHTMLfooter();
			return;
		}

		boolean connAlive;

		connAlive = listBids(user, sp);
		if (connAlive) {
			connAlive = listItem(user, sp);
		}
		/*if (connAlive) {
			connAlive = listWonItems(user,sp);//listWonItems(userId, stmt, conn, sp);
		}
		if (connAlive) {
			connAlive = listBoughtItems(userId, stmt, conn, sp);
		}
		if (connAlive) {
			connAlive = listComment(userId, stmt, conn, sp);
		}

		sp.printHTMLfooter();
		if (connAlive) {
			closeConnection(stmt, conn);
		}
		*/
	}

	/**
	 * Clean up the connection pool.
	 */
	public void destroy() {
		super.destroy();
	}

}
