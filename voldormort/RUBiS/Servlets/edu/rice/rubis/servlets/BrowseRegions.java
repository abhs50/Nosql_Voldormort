package edu.rice.rubis.servlets;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.plaf.synth.Region;

import voldemort.client.ClientConfig;
import voldemort.client.DefaultStoreClient;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.client.protocol.admin.AdminClientConfig;
import voldemort.utils.ByteArray;

/** Builds the html page with the list of all region in the database */
public class BrowseRegions extends RubisHttpServlet {

	//private static DefaultStoreClient<Object, Object> client;

	
	 public int getPoolSize() { return Config.BrowseRegionsPoolSize; }
	 
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
	 * Get the list of regions from the database
	 */
	
	private void regionList(StoreClientFactory factory,ServletPrinter sp) {

	    String bootStrapUrl = "tcp://localhost:6666";
	    String storeName = "regions";

	    int maxThreads = 300;
	    StoreClient<String, String> client = factory.getStoreClient(storeName);

	    String value;
	    String key;
	    HashMap<String, String> listOfRegions = new HashMap<String, String>();
		

		// get the list of regions
		try {

			
		    int nodeId = 0;
		    List<Integer> partitionList = new ArrayList<Integer>();
		    partitionList.add(0);
		    partitionList.add(1);
		    AdminClient adminClient = new AdminClient(bootStrapUrl, new AdminClientConfig());
		    Iterator<ByteArray> iterator = adminClient.fetchKeys(nodeId, "regions", partitionList, null,false);

		    while (iterator.hasNext()) {
		        key = new String(iterator.next().get());
		        value = (String)client.getValue(key);
		        listOfRegions.put(key,value);
		    }
		        
		        /*
			 * conn = getConnection();
			 * 
			 * stmt = conn.prepareStatement("SELECT name, id FROM regions"); rs
			 * = stmt.executeQuery();
			 */
		} catch (Exception e) {
			sp.printHTML("Failed to executeQuery for the list of regions" + e);
			//closeConnection(stmt, conn);
			return;
		}
		try {
			if (listOfRegions.isEmpty()) {
				System.out.println("No regions found");
				sp.printHTML("<h2>Sorry, but there is no region available at this time. Database table is empty</h2><br>");
				//closeConnection(stmt, conn);
				return;
			} else {
				sp.printHTML("<h2>Currently available regions</h2><br>");
				
				String regionName = null;
				for (Map.Entry<String, String> entry : listOfRegions.entrySet()) {
					regionName = entry.getValue();    
					//System.out.println(regionName);
					sp.printRegion(regionName);
				}
				
			}
			//closeConnection(stmt, conn);

		} catch (Exception e) {
			System.out.println(e);
			sp.printHTML("Exception getting region list: " + e + "<br>");
			//closeConnection(stmt, conn);
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		ServletPrinter sp = null;
		sp = new ServletPrinter(response, "BrowseRegions");
		sp.printHTMLheader("RUBiS: Available regions");
		sp = new ServletPrinter(response, "BrowseCategories");
		String username = null, password = null;
   		username = request.getParameter("nickname");
		password = request.getParameter("password");
		String bootstrapUrl = "tcp://localhost:6666";
		StoreClientFactory factory = getConnection();
		sp.printHTMLheader("RUBiS: Available regions"+factory);
		regionList(factory, sp);
		sp.printHTMLfooter();
	}

	/**
	 * Clean up the connection pool.
	 */
	/*
	public void destroy() {
		super.destroy();
	}
	*/

}
