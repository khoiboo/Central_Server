package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import GeoC_QuestionHierarchy.ContRange;
import GeoC_QuestionHierarchy.DateTimeConverter;
import GeoC_QuestionHierarchy.FreeTextMulti;
import GeoC_QuestionHierarchy.FreeTextSingle;
import GeoC_QuestionHierarchy.IncentiveType;
import GeoC_QuestionHierarchy.IncentiveType_Deserializer;
import GeoC_QuestionHierarchy.MultipleChoiceMulti;
import GeoC_QuestionHierarchy.MultipleChoiceSingle;
import GeoC_QuestionHierarchy.PoI;
import GeoC_QuestionHierarchy.PoI_Deserializer;
import GeoC_QuestionHierarchy.Workflow_Element;
import GeoC_QuestionHierarchy.Workflow_Element_Deserializer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Path("getCampaign")
@Singleton
public class getCampaign {
	
	static String campaignStorage = "campaignStorage";
	static String Statistics = "Statistics";
	static String SubmittedResult = "SubmittedResult";
	
	static String campaign_List = "campaign_List";
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@QueryParam("lat") String lat,
						  @QueryParam("lon")  String lon,
						  @QueryParam("language")  String language, 
						  @QueryParam("userID")  String userID,
						  @QueryParam("secretCode")  String userSecretCode,
						  @QueryParam("appName")  String appName) throws IOException {
		
		//Disable logging from MongoDB
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);	
		
		
		System.out.println("Server receives a GET request from client app at " + new Date());
		System.out.println("Parameter  lat is " + lat + " and parameter lon is " + lon);
		System.out.println("The language setting of the phone is " + language);
		System.out.println("The userID is " + userID);
		System.out.println("The secretCode from user is " + userSecretCode);
		
		
		Record_GET_Request insertNewRequest = new Record_GET_Request(lat,lon,userID);
		insertNewRequest.start();
		
		
		List Campaign_list = new ArrayList();
				
		boolean decision = true;
		
		int denominator = 4;
		
		
		GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Branch.class, new Branch_Deserializer());
        gsonBuilder.registerTypeAdapter(Workflow_Element.class, new Workflow_Element_Deserializer());
        gsonBuilder.registerTypeAdapter(Base_Question.class, new BaseQuestion_Deserializer());
        gsonBuilder.registerTypeAdapter(Campaign.class, new Campaign_Deserializer());
        gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeConverter());
        gsonBuilder.registerTypeAdapter(IncentiveType.class, new IncentiveType_Deserializer());
        //gsonBuilder.registerTypeAdapter(PoI.class, new PoI_Deserializer());
        final Gson gson = gsonBuilder.create();
        
        List reply = new ArrayList();
        List campaignList = new ArrayList();
        
        /**** Connect to MongoDB ****/
		// Since 2.10.0, uses MongoClient
		MongoClient mongo = new MongoClient("localhost", 27017);
		
		
		/**** Get database ****/
		// if database doesn't exists, MongoDB will create it for you
		DB db = mongo.getDB(campaignStorage);
		
		
		/**** Get collection / table from 'testdb' ****/
		// if collection doesn't exists, MongoDB will create it for you		
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
		
		//System.out.println("The size of array containing all campaign is " + arrayAllCampaign.size());
		ArrayList<Campaign> campaignWithIncentive = new ArrayList();
		for (int i=0;i< arrayAllCampaign.size();i++)
		{			
			Campaign cam_obj = gson.fromJson(arrayAllCampaign.get(i), Campaign.class);
			if (cam_obj.getCampaignSecretCode() != 100)
				campaignWithIncentive.add(cam_obj);
			else
			{
				
				
				decision = process(cam_obj,lat,lon, userID);
				
				if (decision == true)
				{
					
						giveCampaign(cam_obj, Campaign_list, mongo);
					
					//else if ( (cam_obj.getCampaignSecretCode()% denominator) ==  (Integer.parseInt(userSecretCode)% denominator) )
					/*
					else if ( ((randomNumber*(i+1))%denominator) == (cam_obj.getCampaignSecretCode()% denominator)  )
					{
						giveCampaign(cam_obj, Campaign_list, mongo);
					}*/
				}
				/*
				else if (showCampaignTitle(cam_obj, lat, lon, userID) == true)
				{					
					Campaign emptyCampaign = new Campaign(cam_obj.getID(),"",false,false,"","",false,new ArrayList(),"",new ArrayList(),new ArrayList(),false,new ArrayList(),"",false, 100, 50);
					Campaign_list.add(emptyCampaign);
					System.out.println("+++++++++++++++++++ showCampaignTile only -> " + cam_obj.getID());					
				}
				*/
			}
			
			
		}
		decision = false;
		Random generator = new Random( Integer.parseInt(userSecretCode) );
		int randomNumber= generator.nextInt();
		//System.out.println("The random number corresponding to userSecretCode " + userSecretCode + " is " + randomNumber);
		
		/*
		System.out.println("The number of campaign with incentive is " + campaignWithIncentive.size()); // number of topic * 3
		for (Campaign cam : campaignWithIncentive)
		{
			System.out.println("Campaign " + cam.getID());
		}
		*/
		for ( int i=0;i < campaignWithIncentive.size();i++ ) 
		{
			int round = i / denominator;
			//System.out.println("The round is " + round);
			Random internalGenerator = new Random( (round+1)*70 );
			int internalRandomNumber = internalGenerator.nextInt();
			
			if ( ((randomNumber + internalRandomNumber)% denominator) == (campaignWithIncentive.get(i).getCampaignSecretCode()%denominator) )
			{
				giveCampaign(campaignWithIncentive.get(i), Campaign_list, mongo);
			}	
			
		}
		mongo.close();
		
		//System.out.println("Participants will receive " + Campaign_list.size() + " campaigns");	
		//System.out.println("The content is " + gson.toJson(Campaign_list));
		return Response.status(200).entity(gson.toJson(Campaign_list)).build();		
	}
	
	public void giveCampaign(Campaign camObj, List campaign_list, MongoClient mongo )
	{
		campaign_list.add(camObj);
		//System.out.println("Will serve " + camObj.getID() + " to the participants");
		
		mongo = new MongoClient("localhost", 27017);		
		DB db = mongo.getDB(Statistics);		
		DBCollection table = db.getCollection(camObj.getID());
		
		int currentValue = 0;
		BasicDBObject query = new BasicDBObject();
		BasicDBObject field = new BasicDBObject();
		field.put("download", 1);
		DBCursor cursor = table.find(query,field);
		while (cursor.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    currentValue = obj.getInt("download");	 				    			    
		}
		//currentValue++;              TEMPORARILY DISABLED
		
		// Update	 				
		BasicDBObject search_query = new BasicDBObject();
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.put("download", currentValue);	 				
		BasicDBObject updateObj = new BasicDBObject();
		updateObj.put("$set", newDocument);
		
		table.update(search_query, updateObj);	
		mongo.close();
	}
	
	public boolean showCampaignTitle(Campaign campaign, String user_lat, String user_lon, String userID) {
		boolean returnvalue = false;
		
		if (campaign.getExpiry()== false) //No time validity, 
		{
			if (campaign.getgeoBoolean()== true)
			{
				if ( checkContain ( user_lat,  user_lon,  campaign.getPoI_list())== false) returnvalue = true;
			}
			
		}
		else 
		{
			String dateValue1 = campaign.getStartDate();
			String dateValue2 = campaign.getEndDate();
			
			// Format for input
			DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm");
			// Parsing the date
			DateTime startDate = dtf.parseDateTime(dateValue1);
			DateTime endDate = dtf.parseDateTime(dateValue2);
			
			DateTime now = new DateTime();
			
			if (campaign.getgeoBoolean()== true)
			{
				if ((( now.getMillis() > startDate.getMillis()) && ( now.getMillis() < endDate.getMillis())) && ( checkContain ( user_lat,  user_lon,  campaign.getPoI_list())== false))
					returnvalue = true;				
			}	
			
		}
		
		return returnvalue;
	}
	
	public boolean process(Campaign campaign, String user_lat, String user_lon, String userID){
		boolean returnvalue = false;
		
		//----------check Data validity
		if (campaign.getExpiry() == false) returnvalue = true;
		else
		{
			String dateValue1 = campaign.getStartDate();
			String dateValue2 = campaign.getEndDate();
			
			// Format for input
			DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm");
			// Parsing the date
			DateTime startDate = dtf.parseDateTime(dateValue1);
			DateTime endDate = dtf.parseDateTime(dateValue2);
			
			DateTime now = new DateTime();
			
			if (        (  now.getMillis() > startDate.getMillis()  ) && (  now.getMillis() < endDate.getMillis()  ) )
			{
				returnvalue = true;
				/*
				System.out.println("StartDate is " + startDate);
				System.out.println("now is " + now);
				System.out.println("EndDate is " + endDate);
				System.out.println("\n");
				*/
			}
			else returnvalue = false;		
		}
		
		//---------check Location requirement
		if (campaign.getgeoBoolean() == true)
		{			
			returnvalue = returnvalue & checkContain ( user_lat,  user_lon,  campaign.getPoI_list());
		}
		
		if (campaign.getOnetimeValue() == true)
		{
			/**** Connect to MongoDB ****/
			// Since 2.10.0, uses MongoClient
			MongoClient mongo = new MongoClient("localhost", 27017);
			
			/**** Get database ****/
			// if database doesn't exists, MongoDB will create it for you
			DB db = mongo.getDB(SubmittedResult);
			
			
			/**** Get collection / table from 'testdb' ****/
			// if collection doesn't exists, MongoDB will create it for you		
			DBCollection table = db.getCollection(campaign.getID());
			
			ArrayList<String> submittedUser = new ArrayList();
			BasicDBObject query = new BasicDBObject();
			BasicDBObject field = new BasicDBObject();
			field.put("userID", 1);
			DBCursor cursor = table.find(query,field);
			while (cursor.hasNext()) {
			    BasicDBObject obj = (BasicDBObject) cursor.next();
			    submittedUser.add(obj.getString("userID"));
			}
			
			//System.out.println("Those who have submitted are the following");
			for (int i=0;i< submittedUser.size();i++)
			{
				//System.out.println(submittedUser.get(i));
				if (submittedUser.get(i).equals(userID)) returnvalue = false;
					
			}
			//System.out.println("------------------------------------------");	
			
			mongo.close();			
		}
		
		return returnvalue;
	}
	
	public boolean checkContain (String userLat, String userLong, ArrayList<String> AreaList)
	{
		boolean returnValue = false;
		SpatialReference wgs84 = SpatialReference.create(4326);
		
		boolean[] booleanArray = new boolean[AreaList.size()];
		//System.out.println("---------- Number of shape is" + AreaList.size());
		
		
		for (int i = 0;i < AreaList.size();i++)
		{
			JsonObject shape = (new JsonParser()).parse(AreaList.get(i)).getAsJsonObject();
			JsonArray outerArray = shape.get("rings").getAsJsonArray();
			JsonArray innerArray = (JsonArray) outerArray.get(0);
			//System.out.println("+++++++++++++++++Number of vertices is " + innerArray.size());
			
			JsonArray firstPoint_as_JsonArray = innerArray.get(0).getAsJsonArray();			
			
			//-----------Polygon------------
			Polygon polygon = new Polygon();			
			
			polygon.startPath( firstPoint_as_JsonArray.get(0).getAsDouble(), firstPoint_as_JsonArray.get(1).getAsDouble());				
				
			for (int k=1; k < innerArray.size();k++)
			{
				JsonArray point = innerArray.get(k).getAsJsonArray();
				//System.out.println(point.get(0).getAsDouble() + " " + point.get(1).getAsDouble());
				polygon.lineTo(point.get(0).getAsDouble(), point.get(1).getAsDouble());
			}
			polygon.closePathWithLine();
			//-----------Polygon------------			
			
			Point userLocation = new Point(Double.parseDouble(userLong), Double.parseDouble(userLat));
			
			booleanArray[i] = GeometryEngine.contains( (Geometry) polygon, (Geometry) userLocation, wgs84);				
		}
		
		for (int i =0 ;i< booleanArray.length;i++)
		{
			if (booleanArray[i] == true) returnValue = true;
		}
			
		return returnValue;
		
		/*
		if (AreaList.size()==1)
		{
			returnValue = booleanArray[0];
			return returnValue;
		}
			
		else if (AreaList.size()==2) 
		{
			returnValue = booleanArray[0] | booleanArray[1];
			return returnValue;
		}
			
		else
		{
			returnValue = booleanArray[0] | booleanArray[1];
			for (int i =1;i< booleanArray.length;i++)
				returnValue = returnValue | booleanArray[i];
			System.out.println("Final result is " + returnValue);
			return returnValue;
		}
		*/
		
			
	}
	
}

class Record_GET_Request extends Thread {
	
	String lat,lon,userID;
	
	public Record_GET_Request(String latValue, String lonValue, String userID)
	{
		this.lat = latValue;
		this.lon = lonValue;
		this.userID = userID;
	}	

    @Override
    public void run() {
    	
    	if ( !userID.equals("ccc") )  // The test user
    	{
    		MongoClient mongo = new MongoClient("localhost", 27017);		
    		
    		DB db = mongo.getDB("incomingGetRequest");
    		DBCollection table = db.getCollection("requestDetails");
    		
    		BasicDBObject document = new BasicDBObject();
    		document.put("time", new DateTime().toString());
    		document.put("latitude", lat);
    		document.put("longitude", lon);
    		document.put("userID", userID);
    		table.insert(document);
    		
    		mongo.close();
    		
    	}
        
    	          
    }
}

/*
{
						//send this campaign to the participant 
						Campaign_list.add(cam_obj);
						System.out.println("Now add " + cam_obj.getID() + " to the list");	 				  
										  
						// Connect to MongoDB						
						mongo = new MongoClient("localhost", 27017);					
							
						// Get database ,if database doesn't exists, MongoDB will create it for you
						db = mongo.getDB("Statistics");						
						
						// Get collection / table from your selected database ,if collection doesn't exists, MongoDB will create it for you	 				
						table = db.getCollection(cam_obj.getID());
						System.out.println("Will now update the statistics of " + cam_obj.getID());
						
						//Find the value
						int currentValue = 0;
						query = new BasicDBObject();
						field = new BasicDBObject();
						field.put("download", 1);
						cursor = table.find(query,field);
						while (cursor.hasNext()) {
						    BasicDBObject obj = (BasicDBObject) cursor.next();
						    currentValue = obj.getInt("download");	 				    			    
						}
						currentValue++;	 	 				
		 				
		 				// Update	 				
		 				BasicDBObject search_query = new BasicDBObject();
		 				BasicDBObject newDocument = new BasicDBObject();
		 				newDocument.put("download", currentValue);	 				
		
		 				BasicDBObject updateObj = new BasicDBObject();
		 				updateObj.put("$set", newDocument);
		
		 				table.update(search_query, updateObj);
		 				
		 				mongo.close();	 				
} 
*/
