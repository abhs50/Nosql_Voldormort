package edu.rice.rubis.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * This servlets displays general information about a user. It must be called
 * this way :
 * 
 * <pre>
 * 
 *  http://..../ViewUserInfo?userId=xx where xx is the id of the user
 * 
 * </pre>
 */

public class ViewUserInfo extends RubisHttpServlet {

	public int getPoolSize() {
		return Config.ViewUserInfoPoolSize;
	}

	/**
	 * Close both statement and connection to the database.
	 */
	private void closeConnection(PreparedStatement stmt, Connection conn) {
		try {
			if (conn != null)
				if (conn.getAutoCommit() == false)
					conn.rollback();
		} catch (Exception ignore) {
		}
		try {
			if (stmt != null)
				stmt.close(); // close statement
		} catch (SQLException e) {
		}
		if (conn != null)
			releaseConnection(conn);
	}

	private boolean commentList(String userId, StoreClientFactory factory, StoreClient<String, Object> client, ServletPrinter sp)
  {
    ResultSet rs = null;
    String date, comment,authorId;
    //int authorId;
    AdminClient adminClient = null;
    Iterator<ByteArray> iterator = null;
    
    try
    {
      //conn.setAutoCommit(false); // faster if made inside a Tx
    	HashMap<String, Object> commentList = new HashMap<String, Object>();
      // Try to find the comment corresponding to the user
      try
      {
        client = factory.getStoreClient("comments");
        int nodeId = 0;
        String commentId;
        List<Integer> partitionList = new ArrayList<Integer>();
        partitionList.add(0);
        partitionList.add(1);
        adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
        iterator = adminClient.fetchKeys(nodeId, "comments", partitionList, null, false);
        while (iterator.hasNext()) {
      	  commentId = new String(iterator.next().get());
      	  Versioned vc = client.get(commentId);
      	  HashMap<String, Object> commentObj = (HashMap<String, Object>)vc.getValue();
      	  if(((String)commentObj.get("toId")).equals(userId)){
      		commentList.put(commentId, commentObj);
      	  }
        }
    	  
    	  /*stmt = conn
            .prepareStatement("SELECT * FROM comments WHERE to_user_id=?");
        stmt.setInt(1, userId.intValue());
        rs = stmt.executeQuery();*/
      }
      catch (Exception e)
      {
        sp.printHTML("Failed to execute Query for list of comments: " + e);
        //conn.rollback();
        //closeConnection(stmt, conn);
        return false;
      }
      if (commentList.isEmpty())
      {
        sp.printHTML("<h3>There is no comment yet for this user.</h3><br>");
        //conn.commit();
        //closeConnection(stmt, conn);
        return false;
      }
      sp.printHTML("<br><hr><br><h3>Comments for this user</h3><br>");

      sp.printCommentHeader();
      // Display each comment and the name of its author
      Iterator entries = commentList.entrySet().iterator();
           
      do
      {
        comment = (String)commentList.get("comment");
        date = (String)commentList.get("now");
        authorId = (String)commentList.get("fromId");

        String authorName = "none";
        //ResultSet authorRS = null;
        //PreparedStatement authorStmt = null;
        try
        {
          client = factory.getStoreClient("users");
          Versioned v1 = client.get(authorId);
          HashMap<String, Object> authUser = (HashMap<String, Object>)v1.getValue();
          /*authorStmt = conn
              .prepareStatement("SELECT nickname FROM users WHERE id=?");
          authorStmt.setInt(1, authorId);
          authorRS = authorStmt.executeQuery();
          if (authorRS.first())
            authorName = authorRS.getString("nickname");
          authorStmt.close();*/
          if(!authUser.isEmpty())
          {
        	  authorName = (String)authUser.get("nickname");
          }
        }
        catch (Exception e)
        {
          sp.printHTML("Failed to execute Query for the comment author: " + e);
          //conn.rollback();
          //authorStmt.close();
          //closeConnection(stmt, conn);
          return false;
        }
        sp.printComment(authorName, Integer.parseInt(authorId), date, comment);
      }
      while (entries.hasNext());
      //while (rs.next());
      sp.printCommentFooter();
      //conn.commit();
    }
    catch (Exception e)
    {
      sp.printHTML("Exception getting comment list: " + e + "<br>");
      /*try
      {
        conn.rollback();
        closeConnection(stmt, conn);
        return false;
      }
      catch (Exception se)
      {
        sp.printHTML("Transaction rollback failed: " + e + "<br>");
        closeConnection(stmt, conn);
        return false;
      }*/
    }
    return true;
  }

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String value = request.getParameter("userId");
		String userId;
		ResultSet rs = null;
		ServletPrinter sp = null;
		PreparedStatement stmt = null;
		Connection conn = null;
		StoreClientFactory factory = getConnection();
		StoreClient<String, Object> client = null;

		sp = new ServletPrinter(response, "ViewUserInfo");

		if ((value == null) || (value.equals(""))) {
			sp.printHTMLheader("RUBiS ERROR: View user information");
			sp.printHTML("<h3>You must provide a user identifier !<br></h3>");
			sp.printHTMLfooter();
			return;
		} else
			userId = value;// new Integer(value);

		sp.printHTMLheader("RUBiS: View user information");

		// Try to find the user corresponding to the userId
		HashMap<String, Object> user = new HashMap<String, Object>();
		try {
			client = factory.getStoreClient("users");
			Versioned vc = client.get(userId);
			user = (HashMap<String, Object>) vc.getValue();

			// conn = getConnection();
			/*
			 * conn = null; stmt =
			 * conn.prepareStatement("SELECT * FROM users WHERE id=?");
			 * stmt.setInt(1, userId.intValue()); rs = stmt.executeQuery();
			 */
		} catch (Exception e) {
			sp.printHTML("Failed to execute Query for user: " + e);
			//closeConnection(stmt, conn);
			//sp.printHTMLfooter();
			return;
		}
		try {
			if (user.isEmpty()) {
				sp.printHTML("<h2>This user does not exist!</h2>");
				// closeConnection(stmt, conn);
				sp.printHTMLfooter();
				return;
			}
			String firstname = (String) user.get("firstname");// rs.getString("firstname");
			String lastname = (String) user.get("lastname");// rs.getString("lastname");
			String nickname = (String) user.get("nickname");// rs.getString("nickname");
			String email = (String) user.get("email");// rs.getString("email");
			String date = (String) user.get("creation_date");// rs.getString("creation_date");
			int rating = (Integer) user.get("rating");// rs.getInt("rating");
			// stmt.close();

			String result = new String();

			result = result + "<h2>Information about " + nickname + "<br></h2>";
			result = result + "Real life name : " + firstname + " " + lastname
					+ "<br>";
			result = result + "Email address  : " + email + "<br>";
			result = result + "User since     : " + date + "<br>";
			result = result + "Current rating : <b>" + rating + "</b><br>";
			sp.printHTML(result);

		} catch (Exception s) {
			sp.printHTML("Failed to get general information about the user: "
					+ s);
			// closeConnection(stmt, conn);
			sp.printHTMLfooter();
			return;
		}
		boolean connAlive = commentList(userId, factory, client, sp);
		sp.printHTMLfooter();
		// connAlive means we must close it. Otherwise we must NOT do a
		// double free
		if (connAlive) {
			closeConnection(stmt, conn);
		}
	}

	/**
	 * Clean up the connection pool.
	 */
	public void destroy() {
		super.destroy();
	}

}
