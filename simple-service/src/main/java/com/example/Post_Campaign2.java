package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.nio.file.Paths;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

import GeoC_QuestionHierarchy.Answer;
import GeoC_QuestionHierarchy.Answer_Deserializer;
import GeoC_QuestionHierarchy.Campaign;
import GeoC_QuestionHierarchy.Campaign_Deserializer;
import GeoC_QuestionHierarchy.IncentiveType;
import GeoC_QuestionHierarchy.IncentiveType_Deserializer;
import GeoC_QuestionHierarchy.Rank;
import GeoC_QuestionHierarchy.Rank_Deserializer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@Path("/send_result")
public class Post_Campaign2 {
	static String IV = "AAAAAAAAAAAAAAAA";
	static String encryptionKey = "0123456789abcdef";
	static long minInterval = 1;  //Waiting time, in seconds
	
	 static String FreeTextSingle         = "FreeTextSingle";
     static String FreeTextMulti 		  = "FreeTextMulti";
     static String MultipleChoiceSingle   = "MultipleChoiceSingle";
     static String MultipleChoiceMulti    = "MultipleChoiceMulti";
     static String ContRange 			  = "ContRange";
     static String AudioSensor 			  = "AudioSensor";
     static String TextDisplay 			  = "TextDisplay";
     static String FreeNumericSingle      = "FreeNumericSingle";
     static String UploadPhoto			  = "UploadPhoto";
     static String DateInput 			  = "DateInput";
     static String TimeInput			  = "TimeInput";
     static String WifiSensor 			  = "WifiSensor";
     
     static int point_FreeTextSingle        = 1;
     static int point_FreeTextMulti   		= 1;
     static int point_MultipleChoiceSingle  = 1;
     static int point_MultipleChoiceMulti   = 1;
     static int point_ContRange 			= 1;
     static int point_AudioSensor 		    = 4;
     static int point_FreeNumericSingle     = 1;
     static int point_TextDisplay			= 0;
     static int point_UploadPhoto			= 3;
     static int point_DateInput				= 1;
     static int point_TimeInput				= 1;
     static int point_WifiSensor			= 2;
     
     static int flatPaymentLimit = 500;  // 5 eur for all campaign with flat payment
     
     static String SubmittedResult = "SubmittedResult";
     static String Statistics = "Statistics";
     static String campaignStorage = "campaignStorage";
     static String participant = "MergedParticipant";
     
     static String participantID = "participantList";
     static String campaign_List = "campaign_List";
     
	
	@POST
	//@Path("{id}")
	@Consumes(MediaType.TEXT_PLAIN)	
	public Response respond( String input, @QueryParam("paddingSize") String paddingSize,
			                               @QueryParam("CampaignID")  String CampaignID,
			                               @QueryParam("userID")      String userID,
			                               @QueryParam("lat") String submissionLat,
			                               @QueryParam("lon") String submissionLon,
			                               @QueryParam("submissionMode") String submissionMode) throws Exception {
		
		//Disable logging from MongoDB
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);	
		
		String directory = "C:\\Users\\Khoi\\Server\\simple-service\\Campaign_Result\\";
	
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		System.out.println("Padding size is " + paddingSize);
		System.out.println("CampaignID is " + CampaignID);
		System.out.println("userID is " + userID);
		System.out.println("Submission Latitude is  " + submissionLat);
		System.out.println("Submission Longitude is  " + submissionLon);
		System.out.println("Submission Mode is  " + submissionMode);
		
		int paddingSize_int = Integer.parseInt(paddingSize);
		
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Answer.class, new Answer_Deserializer());
		gsonBuilder.registerTypeAdapter(Campaign.class, new Campaign_Deserializer());
		gsonBuilder.registerTypeAdapter(IncentiveType.class, new IncentiveType_Deserializer());
        final Gson gson = gsonBuilder.create();
		
		Campaign campaign_obj = null;
		ArrayList<IncentiveType> incentiveList = new ArrayList();
		
		/**** Connect to MongoDB ****/
		// Since 2.10.0, uses MongoClient
		MongoClient mongo = new MongoClient("localhost", 27017);
		
		/**** Get database ****/
		// if database doesn't exists, MongoDB will create it for you
		DB db = mongo.getDB(campaignStorage);
		
		
		/**** Get collection / table from 'testdb' ****/
		// if collection doesn't exists, MongoDB will create it for you		
		DBCollection table = db.getCollection(campaign_List);
		
		ArrayList<String> arrayAllCampaign = new ArrayList();
		BasicDBObject queryCampaignID = new BasicDBObject();
		queryCampaignID.put("campaign_ID", CampaignID);
		BasicDBObject fieldCampaignConfig = new BasicDBObject();
		fieldCampaignConfig.put("campaign_Config", 1);
		DBCursor cursorLookForCampaign = table.find(queryCampaignID,fieldCampaignConfig);
		while (cursorLookForCampaign.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursorLookForCampaign.next();
		    arrayAllCampaign.add(obj.getString("campaign_Config"));
		}
		
		if (arrayAllCampaign.size() == 1)
		{
			campaign_obj = gson.fromJson(arrayAllCampaign.get(0), Campaign.class);
			
			System.out.println("Campaign " + campaign_obj.getID() + " found");
			
			incentiveList = campaign_obj.getIncentiveType(); 		
		}		
		
		mongo = new MongoClient("localhost", 27017);
				
		db = mongo.getDB(SubmittedResult);
				
		table = db.getCollection(CampaignID);
		
		ArrayList<String> submittedTime = new ArrayList();
		BasicDBObject query_Time = new BasicDBObject();
		query_Time.put("userID", userID);
		BasicDBObject field_Time = new BasicDBObject();
		field_Time.put("submissionTime", 1);
		DBCursor cursor_Time = table.find(query_Time,field_Time);
		while (cursor_Time.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor_Time.next();
		    submittedTime.add(obj.getString("submissionTime"));
		}
		
		byte[] cipher = hexStringToByteArray(input);
		
		String result_with_padding = decrypt(cipher, encryptionKey);
		
		//System.out.println("After decryption, server receives " + "\n" + result_with_padding);
		String result = result_with_padding.substring(0, result_with_padding.length() - paddingSize_int);
		System.out.println("After padding removal, the text is " + "\n" + result  );
		
		
		
		Answer[] answer_array = gson.fromJson(result, Answer[].class);
		System.out.println("Size of answer_array is " + answer_array.length);
		
		
		
		
		if (submittedTime.size() == 0) //Participant submit for the first time, always accept
		{
			System.out.println("************* This is the first submission ");
			insertSubmission(gson, mongo, userID, CampaignID,result, submissionLat, submissionLon);
			/*
			BasicDBObject document = new BasicDBObject();
			document.put("userID", userID);		
			document.put("submissionContent", result);
			document.put("submissionLat", submissionLat);
			document.put("submissionLon", submissionLon);
			document.put("submissionTime", new DateTime().toString());
			table.insert(document);	
			
			//Update new value for 'submit'
			db = mongo.getDB("Statistics");		
			{				
					// Get collection / table from 'testdb'					 				
					table = db.getCollection(CampaignID);
					
					//Find the value
					int currentValue = 0;
					BasicDBObject query = new BasicDBObject();
					BasicDBObject field = new BasicDBObject();
					field.put("submit", 1);
					DBCursor cursor = table.find(query,field);
					while (cursor.hasNext()) {
					    BasicDBObject obj = (BasicDBObject) cursor.next();
					    currentValue = obj.getInt("submit");				    			    
					}
					
					currentValue++;				
					
					// Update ***				
					BasicDBObject search_query = new BasicDBObject();
					BasicDBObject newDocument = new BasicDBObject();
					newDocument.put("submit", currentValue);	 				

					BasicDBObject updateObj = new BasicDBObject();
					updateObj.put("$set", newDocument);

					table.update(search_query, updateObj);				
			}
			*/
			
			updateExperiencePoint(userID,mongo,answer_array);		
			
			processPayment(incentiveList,userID,mongo);
			
			mongo.close();		
			
			return Response.status(200)
						.entity("First time submission OK")
						.header("Access-Control-Allow-Origin", "*")
						.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
						.allow("OPTIONS")
						.build();

		}
		
		else // Participant has submitted before for this campaign
		{
			for (int i = 0; i < submittedTime.size();i++)
			{
				System.out.println("Previous submission is " + submittedTime.get(i));
			}
			
			System.out.println("Last submission is " + submittedTime.get(submittedTime.size()-1));
			
			DateTime lastSubmission = new DateTime(submittedTime.get(submittedTime.size()-1));
			long lastSubmissionMilisec = lastSubmission.getMillis();
			
			long now = new DateTime().getMillis();
			
			long elapsedTime = (now - lastSubmissionMilisec)/1000; //in seconds
			
			System.out.println("Time elapsed is " + elapsedTime   + " seconds"     );
			
			if (submissionMode.equals("false")) //Always accept an offline submission
			{
				System.out.println("Register an offline submission");
				
				insertSubmission(gson, mongo, userID, CampaignID,result, submissionLat, submissionLon);	
				
				return Response.status(200)
						.entity("Submission OK")
						.header("Access-Control-Allow-Origin", "*")
						.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
						.allow("OPTIONS")
						.build();
			}
			else if (submissionMode.equals("true")) //For online submission, it is important to check the eligibility of the submission
			{
				System.out.println("Examine an online submission");
				
				if (elapsedTime < minInterval)
				{					
					if ( campaign_obj.getContinuousSubmissionValue() == true )  //It's OK to submit very frequently
					{
						System.out.println("Submission is OK, this campaign allows continuous submission");
						
						insertSubmission(gson, mongo, userID, CampaignID,result, submissionLat, submissionLon);
						
						processPayment(incentiveList,userID,mongo);
						
						//updateExperiencePoint(userID,mongo,answer_array);
						
						mongo.close();		
						
						return Response.status(200)
									.entity("Submission OK")
									.header("Access-Control-Allow-Origin", "*")
									.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
									.allow("OPTIONS")
									.build();	
					}
					else // it is not OK to submit before minInterval passed
					{
						System.out.println("The elapsed time is less than the min interval");
						return Response.status(200)
								.entity("Submission rejected due to high submission frequency - minimum interval is " + minInterval + " seconds")
								.header("Access-Control-Allow-Origin", "*")
								.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
								.allow("OPTIONS")
								.build();
					}
					
					
				}
				else //Submission is OK, elapsedTime > minInterval
				{
					System.out.println("Submission is OK");
					
					/*
					BasicDBObject document = new BasicDBObject();
					document.put("userID", userID);		
					document.put("submissionContent", result);
					document.put("submissionLat", submissionLat);
					document.put("submissionLon", submissionLon);
					document.put("submissionTime", new DateTime().toString());
					table.insert(document);	
					
					//Update new value for 'submit'
					db = mongo.getDB("Statistics");		
					{				
							// Get collection / table from 'testdb'					 				
							table = db.getCollection(CampaignID);
							
							//Find the value
							int currentValue = 0;
							BasicDBObject query = new BasicDBObject();
							BasicDBObject field = new BasicDBObject();
							field.put("submit", 1);
							DBCursor cursor = table.find(query,field);
							while (cursor.hasNext()) {
							    BasicDBObject obj = (BasicDBObject) cursor.next();
							    currentValue = obj.getInt("submit");				    			    
							}							
							currentValue++;				
							
							// Update ***				
							BasicDBObject search_query = new BasicDBObject();						
							BasicDBObject newDocument = new BasicDBObject();
							newDocument.put("submit", currentValue);	 				

							BasicDBObject updateObj = new BasicDBObject();
							updateObj.put("$set", newDocument);

							table.update(search_query, updateObj);				
					}
					*/
					
					insertSubmission(gson, mongo, userID, CampaignID,result, submissionLat, submissionLon);
					
					processPayment(incentiveList,userID,mongo);
					
					//updateExperiencePoint(userID,mongo,answer_array);
					
					mongo.close();		
					
					return Response.status(200)
								.entity("Submission OK")
								.header("Access-Control-Allow-Origin", "*")
								.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
								.allow("OPTIONS")
								.build();				
				}				
			}
		}
		return null;		
	}
	
	public static double determineCoefficient(double currentPoint) throws IOException
	{
		System.out.println("Starting method determineCoefficient");
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
	    System.out.println("Rank array is " + content);
	    
	    Rank[] rankArray = gson.fromJson(content, Rank[].class);
	    System.out.println("from method, current point is " + currentPoint);
	    
	    if (currentPoint <= rankArray[0].getLevel()) 
	    {
	    	returnValue = rankArray[0].getWeight();
	    		    	
	    }
	    	
	    else if (currentPoint <= rankArray[rankArray.length-1].getLevel())
	    {   	
	    	
	    	for (int i=1;i < rankArray.length;i++)
			{
	    		System.out.println("---------------" + rankArray[i].getLevel() + " " + rankArray[i].getWeight() );
				if ( rankArray[i].getLevel() >= currentPoint )
					if ( rankArray[i-1].getLevel() <= currentPoint )
						returnValue = rankArray[i-1].getWeight();
			}    	
	    }
	    else returnValue = rankArray[rankArray.length-1].getWeight();
		
		return returnValue;		
	}
	
	public void processPayment(ArrayList<IncentiveType> incentiveList, String userID, MongoClient mongo) throws IOException
	{
		System.out.println("In PROCESS PAYMENT");
		System.out.println("In PROCESS PAYMENT, length of incentive list is " + incentiveList.size());				
		
		for (int i=0;i< incentiveList.size();i++)
		{			
			IncentiveType incentive = incentiveList.get(i);
			if (incentive.getTypeNumber().equals("2"))
			{
				System.out.println("This incentive is FLAT PAYMENT");
				int amount = Integer.parseInt(incentive.getParameter().get(0));
				System.out.println("The amount of FLAT PAYMENT is " + amount);
				
				addPayment(userID, mongo, amount);				
			}
			else if (incentive.getTypeNumber().equals("4"))
			{
				System.out.println("This incentive is NO INCENTIVE");
			}
		}
		mongo.close();		
	}
	
	public void addPayment (String userID, MongoClient mongo, int amount) throws IOException
	{
		System.out.println("Function addPayment starts");
		mongo = new MongoClient("localhost", 27017);
		DB db = mongo.getDB(participant);
		DBCollection table = db.getCollection(participantID);	
		
		double currentMoney = 0;
		double currentFlatPaymentMoney = 0;
		BasicDBObject query = new BasicDBObject();
		query.put("userID", userID);
		System.out.println("In addPayment method, userID is " + userID);
		BasicDBObject field = new BasicDBObject();
		//field.put("money", 1);
		//field.put("moneyFromFlatPayment", 1);
		DBCursor cursor = table.find(query,field);
		
		System.out.println("Size of result is " + cursor.count());
		while (cursor.hasNext()) 
		{
			System.out.println("Found the user !!!");
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    currentMoney = obj.getDouble("money");	
		    currentFlatPaymentMoney = obj.getDouble("moneyFromFlatPayment");
		}
		System.out.println("Current money is " + currentMoney);
		System.out.println("Current flatPaymentMoney is " + currentFlatPaymentMoney);
		
		if ( (flatPaymentLimit - currentFlatPaymentMoney) >= amount )
		{
			System.out.println("Will increment " + amount + " points for user " + userID);
			currentMoney += amount;
			currentFlatPaymentMoney += amount;
			
			System.out.println("Updated money is " + currentMoney);
			System.out.println("Updated flatPaymentMoney is " + currentFlatPaymentMoney);
			
			BasicDBObject search_query = new BasicDBObject();
			search_query.put("userID", userID);
			
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.put("money", currentMoney);	 
			BasicDBObject updateObj = new BasicDBObject();
			updateObj.put("$set", newDocument);
			table.update(search_query, updateObj);
			
			BasicDBObject newFlatPaymentMoney = new BasicDBObject();
			newFlatPaymentMoney.put("moneyFromFlatPayment", currentFlatPaymentMoney);			
			BasicDBObject updateFlatPaymentMoney = new BasicDBObject();
			updateFlatPaymentMoney.put("$set", newFlatPaymentMoney);			
			table.update(search_query, updateFlatPaymentMoney);
		}
		else
		{
			double reducedAmount = flatPaymentLimit - currentFlatPaymentMoney;
			
			System.out.println("Will increment " + reducedAmount + " points for user " + userID);
			currentMoney += reducedAmount;
			currentFlatPaymentMoney += reducedAmount;
			
			System.out.println("Updated money is " + currentMoney);
			System.out.println("Updated flatPaymentMoney is " + currentFlatPaymentMoney);
			
			BasicDBObject search_query = new BasicDBObject();
			search_query.put("userID", userID);
			
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.put("money", currentMoney);	 
			BasicDBObject updateObj = new BasicDBObject();
			updateObj.put("$set", newDocument);
			table.update(search_query, updateObj);
			
			BasicDBObject newFlatPaymentMoney = new BasicDBObject();
			newFlatPaymentMoney.put("moneyFromFlatPayment", currentFlatPaymentMoney);			
			BasicDBObject updateFlatPaymentMoney = new BasicDBObject();
			updateFlatPaymentMoney.put("$set", newFlatPaymentMoney);			
			table.update(search_query, updateFlatPaymentMoney);
		}
		
		/*
		System.out.println("Will increment " + amount + " points for user " + userID);
		currentMoney += amount;
		
		System.out.println("Updated money is " + currentMoney);
		
		BasicDBObject search_query = new BasicDBObject();
		search_query.put("userID", userID);
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.put("money", currentMoney);	 				

		BasicDBObject updateObj = new BasicDBObject();
		updateObj.put("$set", newDocument);

		table.update(search_query, updateObj);
		*/
		mongo.close();
	}
	
	public static void updateExperiencePoint(String userID, MongoClient mongo, Answer[] ansArray) throws IOException
	{		
		DB db = mongo.getDB(participant);
		DBCollection table = db.getCollection(participantID);	
		
		double currentPoint = 0;
		BasicDBObject query = new BasicDBObject("userID", userID);
		BasicDBObject field = new BasicDBObject();
		field.put("expPoint", 1);
		DBCursor cursor = table.find(query,field);
		while (cursor.hasNext()) {
		    BasicDBObject obj = (BasicDBObject) cursor.next();
		    currentPoint = obj.getDouble("expPoint");				    			    
		}
		System.out.println("Current point of  " + userID + " is " + currentPoint);
		
		double coefficient = determineCoefficient(currentPoint);
		System.out.println("0000000000000000000000 Coefficient is " + coefficient);
		
		double point = 0;
		for(int i=0;i<ansArray.length;i++)
		{
			String quesType = ansArray[i].getQuestionType();
			switch (quesType)
			{
				case "FreeTextSingle":       		
					point += (point_FreeTextSingle * coefficient); 		  
					break;
					
				case "FreeTextMulti" :  			
					point += point_FreeTextMulti * coefficient;  		  
					break;
					
				case "MultipleChoiceSingle":		
					point += point_MultipleChoiceSingle * coefficient;    
					break;
					
				case "MultipleChoiceMulti":			
					point += point_MultipleChoiceMulti * coefficient;	  
					break;
					
				case "ContRange":					
					point += point_ContRange * coefficient;			  	  
					break;
					
				case "AudioSensor":	
					if ( ansArray[i].getList().size() > 0 )
					{
						point += point_AudioSensor * coefficient;
					}								  
					break;
				
				case "FreeNumericSingle":			
					point += point_FreeNumericSingle * coefficient;	  	  
					break;
				
				case "UploadPhoto":	
					if ( ansArray[i].getList().size() > 0 )
					{
						point += point_UploadPhoto * coefficient;		
					}						  
					break;
				
				case "DateInput":					
					point += point_DateInput * coefficient;				  
					break;
				
				case "TimeInput":					
					point += point_TimeInput * coefficient;				  
					break;
				
				case "WifiSensor":		
					if ( ansArray[i].getList().size() > 0 )
					{
						point += point_WifiSensor * coefficient;
					}								  
					break;			
			}		
		}
		
		System.out.println("Will increment " + point + " points for user " + userID);
		
		currentPoint += point;
		
		System.out.println("Updated point is " + currentPoint);
		
		BasicDBObject search_query = new BasicDBObject();
		search_query.put("userID", userID);
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.put("expPoint", currentPoint);	 				

		BasicDBObject updateObj = new BasicDBObject();
		updateObj.put("$set", newDocument);

		table.update(search_query, updateObj);	
		
		mongo.close();
	}
	
	public static String decrypt(byte[] cipherText, String encryptionKey) throws Exception{
	    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	    SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
	    cipher.init(Cipher.DECRYPT_MODE, key,new IvParameterSpec(IV.getBytes("UTF-8")));
	    return new String(cipher.doFinal(cipherText),"UTF-8");
	  }
	
	public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

	public static void insertSubmission(Gson gson, MongoClient mongo, String userID, String ID_of_campaign,String submissionContent, String lat, String lon) throws IOException
	{
		DB db = mongo.getDB(SubmittedResult);
		DBCollection table = db.getCollection(ID_of_campaign);	
		
		BasicDBObject document = new BasicDBObject();
		document.put("userID", userID);		
		document.put("submissionContent", submissionContent);
		document.put("submissionLat", lat);
		document.put("submissionLon", lon);
		document.put("submissionTime", new DateTime().toString());
		document.put("submissionQuality","good");
		table.insert(document);
		
		db = mongo.getDB(Statistics);		
		{										 				
				table = db.getCollection(ID_of_campaign);
				
				//Find the value
				int currentValue = 0;
				BasicDBObject query = new BasicDBObject();
				BasicDBObject field = new BasicDBObject();
				field.put("submit", 1);
				DBCursor cursor = table.find(query,field);
				while (cursor.hasNext()) {
				    BasicDBObject obj = (BasicDBObject) cursor.next();
				    currentValue = obj.getInt("submit");				    			    
				}				
				currentValue++;				
				
				// Update ***				
				BasicDBObject search_query = new BasicDBObject();						
				BasicDBObject newDocument = new BasicDBObject();
				newDocument.put("submit", currentValue);	 				

				BasicDBObject updateObj = new BasicDBObject();
				updateObj.put("$set", newDocument);

				table.update(search_query, updateObj);				
		}
		
		Answer[] answer_array = gson.fromJson(submissionContent, Answer[].class);
		
		updateExperiencePoint(userID,mongo,answer_array);
		
		mongo.close();		
	}
}
