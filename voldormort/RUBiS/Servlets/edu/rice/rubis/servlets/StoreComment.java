package edu.rice.rubis.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.UpdateAction;
import voldemort.versioning.Versioned;

/**
 * This servlets records a comment in the database and display the result of the
 * transaction. It must be called this way :
 * 
 * <pre>
 * http://..../StoreComment?itemId=aa&userId=bb&minComment=cc&maxQty=dd&comment=ee&maxComment=ff&qty=gg 
 *   where: aa is the item id 
 *          bb is the user id
 *          cc is the minimum acceptable comment for this item
 *          dd is the maximum quantity available for this item
 *          ee is the user comment
 *          ff is the maximum comment the user wants
 *          gg is the quantity asked by the user
 * /
 * 
 * <pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class StoreComment extends RubisHttpServlet {

	public int getPoolSize() {
		return Config.StoreCommentPoolSize;
	}

	/**
	 * Close both statement and connection to the database.
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
		sp.printHTMLheader("RUBiS ERROR: StoreComment");
		sp
				.printHTML("<h2>Your request has not been processed due to the following error :</h2><br>");
		sp.printHTML(errorMsg);
		sp.printHTMLfooter();

	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String toId; // to user id
		String fromId; // from user id
		String itemId; // item id
		String comment; // user comment
		Integer rating; // user rating
		ServletPrinter sp = null;
		PreparedStatement stmt = null;
		Connection conn = null;
		StoreClientFactory factory = getConnection();
		StoreClient<String, Object> client = null;
		StoreClient<String, String> client1 = null;

		sp = new ServletPrinter(response, "StoreComment");

		/* Get and check all parameters */

		String value = request.getParameter("to");
		if ((value == null) || (value.equals(""))) {
			printError(
					"<h3>You must provide a 'to user' identifier !<br></h3>",
					sp);
			return;
		} else
			// toId = new Integer(value);
			toId = value;

		value = request.getParameter("from");
		if ((value == null) || (value.equals(""))) {
			printError(
					"<h3>You must provide a 'from user' identifier !<br></h3>",
					sp);
			return;
		} else
			fromId = value; // new Integer(value);

		value = request.getParameter("itemId");
		if ((value == null) || (value.equals(""))) {
			printError("<h3>You must provide an item identifier !<br></h3>", sp);
			return;
		} else
			itemId = value;// new Integer(value);

		value = request.getParameter("rating");
		if ((value == null) || (value.equals(""))) {
			printError("<h3>You must provide a rating !<br></h3>", sp);
			return;
		} else
			rating = new Integer(value);

		comment = request.getParameter("comment");
		if ((comment == null) || (comment.equals(""))) {
			printError("<h3>You must provide a comment !<br></h3>", sp);
			return;
		}

		try {
			
			conn = null;
			
			try {
				String now = TimeManagement.currentDateToString();

				client1 = factory.getStoreClient("ids");
				Versioned vc = client1.get("sequence_comments_id");
				String commentId = (String) vc.getValue();
				int newCommentId = Integer.parseInt(commentId) + 1;
				commentId = Integer.toString(newCommentId);

				client = factory.getStoreClient("comments");
				HashMap<String, Object> newComment = new HashMap<String, Object>();
				newComment.put("fromId", fromId);
				newComment.put("toId", toId);
				newComment.put("rating", rating);
				newComment.put("now", now);
				newComment.put("comment", comment);
				newComment.put("itemId", itemId);
	

				client.put(commentId, newComment);

				client1.delete("sequence_comments_id"); 
         			client1.put("sequence_comments_id",commentId);
			} catch (Exception e) {
				
				printError("Error while storing the comment (got exception: "
						+ e + ")<br>", sp);
				
				return;
			}
			// Try to find the user corresponding to the 'to' ID
			try {

				client = factory.getStoreClient("users");
				Versioned vc = client.get(toId);
				 HashMap<String, Object> toUser = (HashMap<String, Object>) vc
						.getValue();
				 String userId = toId;
				
				if (!toUser.isEmpty()) {
					int userRating = (Integer) toUser.get("rating");
					userRating = userRating + rating.intValue();
					toUser.put("rating", userRating);
					//client.applyUpdate(new UpdateAction<String, Object>() {

						//@Override
						//public void update(StoreClient<String, Object> client1) {
							// TODO Auto-generated 
							client.put(userId, toUser);
						//}
					//});

					
				}
			} catch (Exception e) {
				conn.rollback();
				printError(
						"Error while updating user's rating (got exception: "
								+ e + ")<br>", sp);
				
				return;
			}
			sp.printHTMLheader("RUBiS: Comment posting");
			sp
					.printHTML("<center><h2>Your comment has been successfully posted.</h2></center>");

			sp.printHTMLfooter();
			
		} catch (Exception e) {
			sp.printHTML("Exception getting comment list: " + e + "<br>");
			
		}
	}

	/**
	 * Clean up the connection pool.
	 */
	public void destroy() {
		super.destroy();
	}
}
