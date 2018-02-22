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

@Path("getExpPoint")
public class getExpPoint {
	
	static String participant = "MergedParticipant";
	
	static String participantList = "participantList";
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt(@QueryParam("userID")  String userID ) throws IOException {
		
		Record_ViewAccount update_ViewAccount = new Record_ViewAccount(userID);
		update_ViewAccount.start();		
		
		String participantID = null;
		double expPoint = 0;
		double money = 0;
		List<String> listCampaignPrize = new ArrayList();
		String rewardCode = null;
		
		double highestPoint = 0;
		
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
		DBCollection table = db.getCollection(participantList);
		
		BasicDBObject query = new BasicDBObject();
		query.put("userID", userID);
		BasicDBObject field = new BasicDBObject();
		//field.put("expPoint", 1);
		DBCursor cursor = table.find(query,field);
		while (cursor.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    expPoint = obj.getDouble("expPoint");
		    participantID = obj.getString("userID");
		    money = obj.getDouble("money");
		    rewardCode = obj.getString("rewardCode");		    
		    
		    BasicDBList list = (BasicDBList) obj.get("prize");
		    
		    System.out.println("Size of campaignPrize is " + list.size());
			for (int i=0;i<list.size();i++)
			{
				listCampaignPrize.add(  (String) list.get(i) );
			}		
		}
		
		System.out.println("UserID is " + participantID);
		System.out.println("expPoint is " + expPoint);		
		System.out.println("nextRank is " + determineNextRank(expPoint) );
		System.out.println("money is " + money);
		
		double user_expPoint = 0;
		query = new BasicDBObject();
		field = new BasicDBObject();		
		field.put("expPoint", 1);
		cursor = table.find(query,field);		
		while (cursor.hasNext()) 
		{
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    user_expPoint = obj.getDouble("expPoint");
		    
		    if ( user_expPoint > highestPoint )
		    {
		    	highestPoint = user_expPoint;
		    }	    		
		}
		
		System.out.println("Highest point is " + highestPoint);
		
		Gson gson = new Gson();
		Participant participant = new Participant(participantID, expPoint, determineNextRank(expPoint),money, listCampaignPrize, rewardCode, highestPoint);
		
		mongo.close();
		
		return Response.status(200).entity(gson.toJson(participant)).build();		
	}
	
	public double determineNextRank(double currentPoint) throws IOException
	{
		double returnValue = 1;
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Rank.class, new Rank_Deserializer());
		Gson gson = gsonBuilder.create();
		
		java.nio.file.Path p1 =  java.nio.file.Paths.get("C:\\Users\\mngo\\Server\\simple-service\\src\\main\\java\\com\\example");
		String content = null;
	    BufferedReader br = new BufferedReader(new FileReader(p1.toString() + "\\" + "config.txt"));
	    try {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            sb.append(System.lineSeparator());
	            line = br.readLine();
	        }
	        content = sb.toString();
	    } finally {
	        br.close();
	    }
	    
	    Rank[] rankArray = gson.fromJson(content, Rank[].class);
	    System.out.println("from method, current point is " + currentPoint);
	    
	    if (currentPoint <= rankArray[0].getLevel()) 
	    {
	    	returnValue = rankArray[0].getLevel();
	    		    	
	    }
	    	
	    else if (currentPoint <= rankArray[rankArray.length-1].getLevel())
	    {   	
	    	
	    	for (int i=1;i < rankArray.length;i++)
			{
	    		System.out.println("---------------" + rankArray[i].getLevel() + " " + rankArray[i].getWeight() );
				if ( rankArray[i].getLevel() >= currentPoint )
					if ( rankArray[i-1].getLevel() <= currentPoint )
						returnValue = rankArray[i].getLevel();
			}    	
	    }
	    else returnValue = 0;
		
		return returnValue;		
	}
}

class Record_ViewAccount extends Thread {
	
	String userID;
	
	public Record_ViewAccount(String userID) 	{ 	this.userID = userID; 	}	

    @Override
    public void run() {
        
    	MongoClient mongo = new MongoClient("localhost", 27017);		
		
		DB db = mongo.getDB("incomingViewAccount");
		DBCollection table = db.getCollection("requestDetails");
		
		BasicDBObject document = new BasicDBObject();
		document.put("userID", userID);
		document.put("time", new Date());
		table.insert(document);
		
		mongo.close();          
    }
}
