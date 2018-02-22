package com.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

import GeoC_QuestionHierarchy.Answer;
import GeoC_QuestionHierarchy.Answer_Deserializer;
import GeoC_QuestionHierarchy.BaseQuestion_Deserializer;
import GeoC_QuestionHierarchy.Base_Question;
import GeoC_QuestionHierarchy.Branch;
import GeoC_QuestionHierarchy.Branch_Deserializer;
import GeoC_QuestionHierarchy.Campaign;
import GeoC_QuestionHierarchy.Campaign_Deserializer;
import GeoC_QuestionHierarchy.DateTimeConverter;
import GeoC_QuestionHierarchy.Participant;
import GeoC_QuestionHierarchy.Workflow_Element;
import GeoC_QuestionHierarchy.Workflow_Element_Deserializer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Path("Test_getDetailedResult")
public class Test_getDetailedResult {
	
	static String SubmittedResult = "Test_SubmittedResult";
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@QueryParam("campaignID")  String campaignID, @QueryParam("quesID")  String quesID  ) throws IOException {
		
		System.out.println("Received request for question " + quesID + " of campaign \"" + campaignID + "\"");
		
		//Disable logging from MongoDB
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);		
		
		GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Answer.class, new Answer_Deserializer());
        gsonBuilder.registerTypeAdapter(Campaign.class, new Campaign_Deserializer());
        gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeConverter());
        //gsonBuilder.registerTypeAdapter(PoI.class, new PoI_Deserializer());
        final Gson gson = gsonBuilder.create();
        
        ArrayList<Answer> reply = new ArrayList();
		
		MongoClient mongo = new MongoClient("localhost", 27017);		
				
		DB db = mongo.getDB(SubmittedResult);		
		
		/**** Get collection / table from 'testdb' ****/		
		DBCollection table = db.getCollection(campaignID);
		
		BasicDBObject query = new BasicDBObject();		
		BasicDBObject field = new BasicDBObject();
		field.put("submissionContent", 1);
		DBCursor cursor = table.find(query,field);
		while (cursor.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    
		    String submissionContent = obj.getString("submissionContent");
		    
		    Answer[] answerArray = gson.fromJson(submissionContent, Answer[].class);
		    for (int i=0;i < answerArray.length;i++)
		    {
		    	if (answerArray[i].getID().equals(quesID))
		    		reply.add(answerArray[i]);
		    }
		    		
		}
		
		
		mongo.close();
		
		return Response.status(200).entity(gson.toJson(reply)).build();			
	}

}
