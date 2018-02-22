package com.example;

import java.util.ArrayList;
import java.util.Random;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Path("/login")
public class Login {
	
	static String participant = "MergedParticipant";
	static String participantID = "participantList";
	
	@POST
	//@Path("{id}")
	@Consumes(MediaType.TEXT_PLAIN)	
	public Response respond(String input, @QueryParam("userEmail")  String email,
										  @QueryParam("password") String password,
										  @QueryParam("device") String deviceModel,
										  @QueryParam("APK version") String APK_Version) throws Exception {
		
		String reply = "0";
		
		System.out.println("participant's email is " + email);
		System.out.println("participant's password is " + password);
		System.out.println("participant's device is " + deviceModel);
		System.out.println("participant's APK version is " + APK_Version);
		
		
		//Disable logging from MongoDB
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);	
		
		/**** Connect to MongoDB ****/
		// Since 2.10.0, uses MongoClient
		MongoClient mongo = new MongoClient("localhost", 27017);
		
		//DB db = null;
		
		/**** Get database ****/
		// if database doesn't exists, MongoDB will create it for you
		DB db = mongo.getDB(participant);		
		
		
		/**** Get collection / table from 'testdb' ****/
		// if collection doesn't exists, MongoDB will create it for you		
		DBCollection table = db.getCollection(participantID);
		
		ArrayList<String> listOfEmail = new ArrayList();
		BasicDBObject query = new BasicDBObject();
		query.put("email", email);		
		BasicDBObject field = new BasicDBObject();
		field.put("email", 1);		
		DBCursor cursor = table.find(query,field);
		while (cursor.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    System.out.println(obj.getString("email"));
		    listOfEmail.add(obj.getString("email"));		    
		}
		
		ArrayList replyParameters = new ArrayList();
		
		if (listOfEmail.size() == 1) //Email found
		{
			System.out.println("Email found");
			
			String nickName = "";
			int participantSecretCode = -10;
			ArrayList<String> listOfPassWord = new ArrayList();
			query = new BasicDBObject();
			query.put("email", email);	
			query.put("password", password);			
			field = new BasicDBObject();
			field.put("email", 1);
			field.put("password", 1);
			field.put("userID", 1);
			field.put("participantSecretCode", 1);
			
			cursor = table.find(query,field);
			while (cursor.hasNext()) {
			    BasicDBObject obj = (BasicDBObject) cursor.next();			    
			    listOfPassWord.add(obj.getString("password"));	
			    nickName = obj.getString("userID");
			    participantSecretCode = obj.getInt("participantSecretCode");
			}
			
			if ( listOfPassWord.size() == 1 ) //Password found
			{
				System.out.println("Password found");
				
				if (participantSecretCode == -1) //User has NOT log in on mobile device
				{
					int min = 0, max = 100;

		            Random r = new Random();
		            int randomNumber = r.nextInt(max - min + 1) + min;
		            System.out.println("The secret code for participant will be " + randomNumber);
					
					BasicDBObject search_query = new BasicDBObject();
					search_query.put("email",email);
					
					BasicDBObject newDocument = new BasicDBObject();
					newDocument.put("APK version",APK_Version);
					newDocument.put("device",deviceModel);
					newDocument.put("participantSecretCode",randomNumber);
					newDocument.put("rewardCode", getSaltString());
					
					BasicDBObject updateObj = new BasicDBObject();
					updateObj.put("$set", newDocument);
					table.update(search_query, updateObj);				
					
					replyParameters.add(randomNumber);
					replyParameters.add(nickName);
					System.out.println("reply from server is " + replyParameters.toString());
					
					reply = replyParameters.toString();  //Log in successfully for the very first time, 0 <= reply <= 100				
				}
				else if (participantSecretCode >= 0) //User has ALREADY logged in on mobile device
				{
					replyParameters.add(participantSecretCode);
					replyParameters.add(nickName);
					
					reply = replyParameters.toString();  //Log in successfully, 0 <= reply <= 100
				}
				
								
			}
			else 
			{
				replyParameters.add(202); //Correct email but incorrect password
				reply = replyParameters.toString(); 
			}		
		}
		else
		{
			replyParameters.add(203); //Email not found
			reply = replyParameters.toString(); 
		}		
		mongo.close(); 
		
		return Response.status(200)
				.entity(reply)
				.header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
				.allow("OPTIONS")
				.build();
		
	}
	
	protected static String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 6) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }

}
