package com.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Path("updateAddFavorite")
@Singleton
public class update_AddFavorite_Count {
	
	static String Statistics = "Statistics";
	
	
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@QueryParam("campaignID") String campaignID, @QueryParam("participantID") String participantID  ) throws IOException {
		
		System.out.println("Campaign ID is " + campaignID);
		System.out.println("Participant ID is " + participantID);
		
		//Disable logging from MongoDB
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);
		
		Set uniqueUser = new HashSet();
		
		/**** Connect to MongoDB ****/
		// Since 2.10.0, uses MongoClient
		MongoClient mongo = new MongoClient("localhost", 27017);
		
		/**** Get database ****/
		// if database doesn't exists, MongoDB will create it for you
		DB db = mongo.getDB(Statistics);
		DBCollection table = db.getCollection(campaignID);
		
		BasicDBObject query1 = new BasicDBObject();
	    BasicDBObject field1 = new BasicDBObject();    
	    
	    DBCursor cursor = table.find(query1,field1);
	    while (cursor.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    
		    BasicDBList list = (BasicDBList) obj.get("addtoFavorite");
		    System.out.println("Current number of users who add this campaign to thier fav list is  " + list.size());
		    
		    for (int e=0;e<list.size();e++)
			{
		    	uniqueUser.add(  (String) list.get(e) );
			}		    
		}
	    
	    System.out.println("Size of user Set is " + uniqueUser.size());
	    
	    uniqueUser.add(participantID); // Add the new participantID to the Set, if possible
	    
	    ArrayList<String> newArray = new ArrayList(uniqueUser);
	    
	    // Update	 				
	 	BasicDBObject search_query = new BasicDBObject();
	 	BasicDBObject newDocument = new BasicDBObject();
	 	newDocument.put("addtoFavorite", newArray);	 				
	 	BasicDBObject updateObj = new BasicDBObject();
	 	updateObj.put("$set", newDocument);
	 	
	 	table.update(search_query, updateObj);
	    
	    
	    
		
		mongo.close();
		return Response.status(200).entity("").build();
		
	}

}
