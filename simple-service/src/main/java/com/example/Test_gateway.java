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
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import GeoC_QuestionHierarchy.BaseQuestion_Deserializer;
import GeoC_QuestionHierarchy.Base_Question;
import GeoC_QuestionHierarchy.Branch;
import GeoC_QuestionHierarchy.Branch_Deserializer;
import GeoC_QuestionHierarchy.Campaign;
import GeoC_QuestionHierarchy.Campaign_Deserializer;
import GeoC_QuestionHierarchy.DateTimeConverter;
import GeoC_QuestionHierarchy.Submission;
import GeoC_QuestionHierarchy.Workflow_Element;
import GeoC_QuestionHierarchy.Workflow_Element_Deserializer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Path("Test_gateway")
public class Test_gateway {
	
	String campaignStorage = "Test_campaignStorage";
	String SubmittedResult = "Test_SubmittedResult";
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@QueryParam("type")  String type,
						  @QueryParam("campaign")  String campaignID,
						  @QueryParam("userID")  String userID) throws IOException
	{
		
		//Disable logging from MongoDB
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);	
				
		String returnValue = "";
		List Campaign_list = new ArrayList();
		

		GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Branch.class, new Branch_Deserializer());
        gsonBuilder.registerTypeAdapter(Workflow_Element.class, new Workflow_Element_Deserializer());
        gsonBuilder.registerTypeAdapter(Base_Question.class, new BaseQuestion_Deserializer());
        gsonBuilder.registerTypeAdapter(Campaign.class, new Campaign_Deserializer());
        gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeConverter());        
        final Gson gson = gsonBuilder.create();
		
		/**** Connect to MongoDB ****/
		MongoClient mongo = new MongoClient("localhost", 27017);		
		
		if (type.equals("campaign"))
		{
			if (campaignID == null)
			{
				System.out.println("Reply with all campaigns");
				DB db = mongo.getDB(campaignStorage);
				DBCollection table = db.getCollection("campaign_List");
				
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
					Campaign_list.add(cam_obj);
					
					returnValue = gson.toJson(Campaign_list);
				}			
			}
			else 
			{
				System.out.println("\n\n\n\nThe requested campaign is " + campaignID);
				DB db = mongo.getDB(campaignStorage);
				DBCollection table = db.getCollection("campaign_List");
				
				ArrayList<String> arrayAllCampaign = new ArrayList();
				BasicDBObject query = new BasicDBObject();
				query.put("campaign_ID", campaignID);
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
					Campaign_list.add(cam_obj);
					
					returnValue = gson.toJson(Campaign_list);
				}	
				
			}
			
		}			
		else if (type.equals("result"))
		{
			if (userID == null) //Get all results from the specified campaign
			{
				System.out.println("\n\n\n\nDisplaying the result of campaign " + campaignID);
				DB db = mongo.getDB(SubmittedResult);
				DBCollection table = db.getCollection(campaignID);	
				
				ArrayList<Submission> arraySubmission = new ArrayList();
				BasicDBObject query = new BasicDBObject();
				//query.put("campaign_ID", campaignID);
				BasicDBObject field = new BasicDBObject();
				
				DBCursor cursor = table.find(query,field);
				while (cursor.hasNext()) {
				    BasicDBObject obj = (BasicDBObject) cursor.next();
				    
				    Submission temp = new Submission("xxx",obj.getString("submissionContent"),obj.getString("submissionLat"),obj.getString("submissionLon"),obj.getString("submissionTime"));
				    arraySubmission.add(temp);				    
				}
				
				returnValue = gson.toJson(arraySubmission);
				
			}
			else //Get all results from a particular user in the specified campaign
			{
				System.out.println("\n\n\n\nDisplaying the submission of user " + userID + " in campaign " + campaignID);
				DB db = mongo.getDB(SubmittedResult);
				DBCollection table = db.getCollection(campaignID);
				
				ArrayList<Submission> arraySubmission = new ArrayList();
				BasicDBObject query = new BasicDBObject();
				query.put("userID", userID);
				BasicDBObject field = new BasicDBObject();
				
				DBCursor cursor = table.find(query,field);
				while (cursor.hasNext()) {
				    BasicDBObject obj = (BasicDBObject) cursor.next();
				    
				    Submission temp = new Submission(obj.getString("userID"),obj.getString("submissionContent"),obj.getString("submissionLat"),obj.getString("submissionLon"),obj.getString("submissionTime"));
				    arraySubmission.add(temp);				    
				}
				
				returnValue = gson.toJson(arraySubmission);				
			}		
			
		}
		
		mongo.close();
			
			
		return Response.status(200).entity(returnValue).build();
		
		
	}

}
