package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import GeoC_QuestionHierarchy.Participant;
import GeoC_QuestionHierarchy.Rank;
import GeoC_QuestionHierarchy.Rank_Deserializer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Path("Test_getSubmissionStatus")
public class Test_getSubmissionStatus {
	
	static String participant = "Test_participant";	
	static String participantID = "participantID";
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@QueryParam("userID")  String userID ) throws IOException {		
		
		List<List> listTrackedSubmission = new ArrayList();
		
		System.out.println("Param is " + userID);
		
		//Disable logging from MongoDB
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);	
		
		/**** Connect to MongoDB ****/
		MongoClient mongo = new MongoClient("localhost", 27017);
		
		/**** Get database ****/		
		DB db = mongo.getDB(participant);		
		
		/**** Get collection / table from 'testdb' ****/		
		DBCollection table = db.getCollection("participantID");
		
		BasicDBObject query = new BasicDBObject();
		query.put("userID", userID);
		BasicDBObject field = new BasicDBObject();
		field.put("trackedSubmission", 1);
		DBCursor cursor = table.find(query,field);
		while (cursor.hasNext()) 
		{
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    
		    BasicDBList list = (BasicDBList) obj.get("trackedSubmission");
		    
		    System.out.println("Size of campaignPrize is " + list.size());
			for (int i=0;i<list.size();i++)
			{
				listTrackedSubmission.add(  (List) list.get(i) );
			}		
		}			
		mongo.close();
		
		Gson gson = new Gson();
		return Response.status(200).entity(gson.toJson(listTrackedSubmission)).build();		
	}
	
	
}


