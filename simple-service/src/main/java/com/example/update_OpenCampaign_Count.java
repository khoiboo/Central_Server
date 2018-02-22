package com.example;

import java.io.IOException;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Path("updateOpenCount")
@Singleton
public class update_OpenCampaign_Count {
	
	static String Statistics = "Statistics";
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@QueryParam("campaignID") String campaignID ) throws IOException {
		
		System.out.println("Campaign ID is " + campaignID);
		
		//Disable logging from MongoDB
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);
		
		/**** Connect to MongoDB ****/
		// Since 2.10.0, uses MongoClient
		MongoClient mongo = new MongoClient("localhost", 27017);
		
		/**** Get database ****/
		// if database doesn't exists, MongoDB will create it for you
		DB db = mongo.getDB(Statistics);
		DBCollection table = db.getCollection(campaignID);
		
		int currentOpenValue = 0;
		BasicDBObject query = new BasicDBObject();
		BasicDBObject field = new BasicDBObject();
		field.put("open", 1);
		DBCursor cursor = table.find(query,field);
		while (cursor.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    currentOpenValue = obj.getInt("open");	 				    			    
		}
		currentOpenValue++;
		
		// Update	 				
		BasicDBObject search_query = new BasicDBObject();
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.put("open", currentOpenValue);	 				
		BasicDBObject updateObj = new BasicDBObject();
		updateObj.put("$set", newDocument);
		
		//table.update(search_query, updateObj);     TEMPORARILY DISABLED
		mongo.close();
		
		return Response.status(200).entity("").build();
		
	}
	
	
	

}
