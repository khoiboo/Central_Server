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

@Path("about")
public class about {
	
	@GET	
    @Produces(MediaType.TEXT_PLAIN)
	public Response getIt( @QueryParam("language")  String language  ) throws IOException
	{
		String sentence1_en = "This experiment is about bla bla";
		String sentence2_en = "The experiemnt will end on 28 July 2017";
		String sentence3_en = "Gifts and cash will be delivered in October, with email notification";
		
		String sentence1_de = "Eine Plattform für alle, um eine Datenerfassungskampagne zu starten.";
		String sentence2_de = "Du kannst der Plattform beitreten um Kampagnen zu definieren oder Daten zu sammeln oder beides";
		String sentence3_de = "Diese Plattform erlaubt Echtzeitüberwachung und Visualisierung gesammelter Ergebnisse. ";
		
		ArrayList<String> reply = new ArrayList();
		if ( language.equals("en") )
		{
			reply.add(sentence1_en);
			reply.add(sentence2_en);
			reply.add(sentence3_en);
		}
		else if ( language.equals("de") )
		{
			reply.add(sentence1_de);
			reply.add(sentence2_de);
			reply.add(sentence3_de);
		}
			
			
		return Response.status(200).entity(reply.toString()).build();	
	}

}
