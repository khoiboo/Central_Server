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

@Path("/register")
public class Register {
	
	static String participant = "MergedParticipant";
	static String participantID = "participantList";
	
	@POST
	//@Path("{id}")
	@Consumes(MediaType.TEXT_PLAIN)	
	public Response respond(String input, @QueryParam("firstName") String firstName,
										  @QueryParam("lastName") String lastName,										  
										  @QueryParam("userEmail")  String email,
										  @QueryParam("password") String password,
										  @QueryParam("pictureLink") String pictureLink,
										  @QueryParam("nickName")  String userID,
										  @QueryParam("gender")  String gender,
										  @QueryParam("yearOfBirth")  String YoB,
										  @QueryParam("device") String deviceModel,
										  @QueryParam("APK version") String APK_Version) throws Exception {
		
		String reply = "good";
		
		System.out.println("Server receives: " + input);
		System.out.println("participant ID is " + userID);
		System.out.println("participant's email is " + email);
		System.out.println("participant gender is " + gender);
		System.out.println("participant year of birth is " + YoB);
		
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
		
		ArrayList<String> listOfName = new ArrayList();
		BasicDBObject query = new BasicDBObject();
		BasicDBObject field = new BasicDBObject();
		field.put("email", 1);
		DBCursor cursor = table.find(query,field);
		while (cursor.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    listOfName.add(obj.getString("email"));
		}
		
		for (String obj: listOfName)
		{
			if (obj.equals(email))
			{
				reply = "bad";
			}
		}	
		
		if(!reply.equals("bad"))
		{
			int min = 0;
            int max = 100;

            Random r = new Random();
            int randomNumber = r.nextInt(max - min + 1) + min;
            System.out.println("The secret code is " + randomNumber);
            
            reply = String.valueOf(randomNumber);
			/**** Insert ****/
			// create a document to store key and value
			BasicDBObject document = new BasicDBObject();
			document.put("firstName", firstName);
			document.put("lastName", lastName);
			document.put("userID", userID);	
			document.put("email", email);
			document.put("password", password);
			document.put("yearOfBirth", YoB);
			document.put("gender", gender);
			document.put("linkProfilePic", pictureLink);
			document.put("timeOfRegister", new DateTime().toString());
			document.put("device", deviceModel);
			document.put("APK version", APK_Version);
			document.put("participantSecretCode", randomNumber);
			document.put("rewardCode", getSaltString());
			document.put("expPoint", 0);
			document.put("money", 0);
			document.put("moneyFromFlatPayment", 0);
			document.put("prize", new String[]{});
			
			table.insert(document);
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
