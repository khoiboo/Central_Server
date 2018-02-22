package com.example;

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

@Path("Test_getPublicResult")
public class Test_getPublicResult {
	
	static String campaignStorage = "Test_campaignStorage";
	
	static String campaign_List = "campaign_List";
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@QueryParam("userID")  String userID) throws IOException {
		
		
		
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
		        
		ArrayList<Campaign> reply = new ArrayList();
		
		MongoClient mongo = new MongoClient("localhost", 27017);	
		
		DB db = mongo.getDB(campaignStorage);		
			
		DBCollection table = db.getCollection(campaign_List);
		
		ArrayList<String> arrayAllCampaign = new ArrayList();
		BasicDBObject query = new BasicDBObject();
		BasicDBObject field = new BasicDBObject();
		field.put("campaign_Config", 1);
		DBCursor cursor = table.find(query,field);
		while (cursor.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    arrayAllCampaign.add(obj.getString("campaign_Config"));
		}
		
		System.out.println("The size of array containing all campaign is " + arrayAllCampaign.size());
		for (int i=0;i< arrayAllCampaign.size();i++)
		{
			Campaign cam_obj = gson.fromJson(arrayAllCampaign.get(i), Campaign.class);
			
			if (cam_obj.getShowResultBoolean() == true)
				reply.add(cam_obj);				
		}
		System.out.println("The number of campaigns with public results is " +  reply.size());	
		
		return Response.status(200).entity(gson.toJson(reply)).build();			
	}

}


