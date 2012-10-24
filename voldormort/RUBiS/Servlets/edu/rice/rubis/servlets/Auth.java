package edu.rice.rubis.servlets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import voldemort.client.StoreClientFactory;

import voldemort.client.ClientConfig;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.client.protocol.admin.AdminClientConfig;
import voldemort.utils.ByteArray;
import voldemort.versioning.Versioned;

public class Auth
{

  //private Context servletContext;
  private Connection conn = null;
  private ServletPrinter sp;

  public Auth(Connection connect, ServletPrinter printer)
  {
    conn = connect;
    sp = printer;
  }

  public int authenticate(String name, String password)
  {
    String userId = "-1";
    //ResultSet rs = null;
    //PreparedStatement stmt = null;

    // Lookup the user
    try
    {
    	String bootstrapUrl = "tcp://localhost:6666";
        StoreClientFactory factory = new SocketStoreClientFactory(new ClientConfig().setBootstrapUrls(bootstrapUrl));
    	StoreClient<String, String> client = factory.getStoreClient("users");
    	boolean userPresent = false;
		int nodeId = 0;
		List<Integer> partitionList = new ArrayList<Integer>();
		partitionList.add(0);
		partitionList.add(1);
		AdminClient adminClient = new AdminClient(bootstrapUrl, new AdminClientConfig());
		Iterator<ByteArray> iterator = adminClient.fetchKeys(nodeId,
				"users", partitionList, null, false);
		while (iterator.hasNext()) {
			userId = new String(iterator.next().get());
			Versioned vc = client.get(userId);
			Map<String, Object> userList = (Map<String, Object>)vc.getValue();
			if(userList.get("nickname").equals(name) && userList.get("password").equals(password))
			{
				userPresent = true;
				break;
				
			}
		}
      /*stmt =
        conn.prepareStatement(
          "SELECT users.id FROM users WHERE nickname=? AND password=?");
      stmt.setString(1, name);
      stmt.setString(2, password);
      rs = stmt.executeQuery();*/
      if (!userPresent)
      {
        sp.printHTML(
          " User " + name + " does not exist in the database!<br><br>");
        return Integer.parseInt(userId);
      }
      //userId = rs.getInt("id");
    }
    catch (Exception e)
    {
      sp.printHTML("Failed to executeQuery " + e);
      return Integer.parseInt(userId);
    }
    /*finally
    {
      try
      {
        if (stmt != null)
          stmt.close(); // close statement
      }
      catch (Exception ignore)
      {
      }
      return userId;
    }*/
  
  return Integer.parseInt(userId);
  }
}
