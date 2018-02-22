package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
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

@Path("Test_getSubmissionStatus2")
public class Test_getSubmissionStatus2 {
	
	static String DB_Name = "Test_SubmittedResult";	
	
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@QueryParam("userID")  String userID ) throws IOException {		
		
		List<ArrayList<String>> listTrackedSubmission = new ArrayList();
		
		System.out.println("Param is " + userID);
		
		//Disable logging from MongoDB
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);	
		
		/**** Connect to MongoDB ****/
		MongoClient mongo = new MongoClient("localhost", 27017);
		
		DB db = mongo.getDB(DB_Name);
		
		Set<String> collections = db.getCollectionNames();
		
		for (String collectionName: collections)
		{
			DBCollection table = db.getCollection(collectionName);
			
			BasicDBObject query = new BasicDBObject();
			query.put("userID", userID);
			BasicDBObject field = new BasicDBObject();
			field.put("status", 1);
			field.put("submissionTime", 1);
			DBCursor cursor = table.find(query,field);
			while (cursor.hasNext()) 
			{
				BasicDBObject obj = (BasicDBObject) cursor.next();
				
				
				String status = obj.getString("status");
				String timeStamp = obj.getString("submissionTime");
				
				if (status != null)
				{
					ArrayList<String> entry = new ArrayList();
					entry.add(collectionName);
					entry.add(timeStamp);
					entry.add(status);				
					listTrackedSubmission.add(entry);
				}				
			}
			
		}
		
	    Collections.sort(listTrackedSubmission, new MyComparator());
		
		Gson gson = new Gson();
		return Response.status(200).entity(gson.toJson(listTrackedSubmission)).build();		
	}
	
	
}

class MyComparator implements Comparator<ArrayList<String>> {
@Override
public int compare(ArrayList<String> o1, ArrayList<String> o2) {
	DateTime moment1 = new DateTime(o1.get(1));
	DateTime moment2 = new DateTime(o2.get(1));
    if ( moment1.getMillis() > moment2.getMillis() ) {
        return -1;
    } else if ( moment1.getMillis() < moment2.getMillis() ) {
        return 1;
    }
    return 0;
}}


