package com.notesapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@Path("/notes")
public class NotesRestService {
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createNote(InputStream incomingNote) throws IOException {
		
		String sDataFileName = "notesData.dat";
		//initial ID is 1
		int newID = 1;
		
		File dataFile = new File(sDataFileName);
		//Make sure the file exists and is Writeable/Readable
		if(!dataFile.exists())
		{
			try
			{
				dataFile.createNewFile();
			}
			catch(IOException e)
			{
				return Response.status(Response.Status.EXPECTATION_FAILED).entity("Failed to create data file.").build();
			}
		}
		if(!dataFile.canRead())
		{
			dataFile.setReadable(true);
		}
		if(!dataFile.canWrite())
		{
			dataFile.setWritable(true);
		}
		
		if( dataFile.length() > 0 )
		{	
			try(FileReader fileReader = new FileReader(sDataFileName))
			{
				LineNumberReader lineReader = new LineNumberReader(fileReader);
				lineReader.skip(Long.MAX_VALUE);
				newID = lineReader.getLineNumber()+1;
			}
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(incomingNote));
		JSONParser jsonParser = new JSONParser();
		String inputStr = reader.readLine(); //assumption here is that incoming note is a single line
		if(inputStr.charAt(0) == '\'')
		{
			//remove the surrounding single quotes
			inputStr = inputStr.substring(1,inputStr.length()-1);
		}
			
		if(inputStr != null)
		{
			try
			{
				JSONObject jsonObj = (JSONObject)jsonParser.parse(inputStr);
				
				jsonObj.put("id", newID);
				
				try(FileWriter file = new FileWriter(sDataFileName, true))
				{
					file.append(jsonObj.toJSONString()+"\n");
				}
		
		 
				return Response.status(Response.Status.OK).entity(jsonObj.toJSONString()).build();
			}
			catch(ParseException e)
			{
				return Response.status(Response.Status.EXPECTATION_FAILED).entity("Failed to parse string: " + inputStr + " \n" + e.toString()).build();
			}
			
			
		}
		
		return Response.status(Response.Status.EXPECTATION_FAILED).entity("Failed to read input note.").build();
	}
	
	private JSONArray GetJSONFileContents() throws IOException, ParseException
	{
		String sDataFileName = "notesData.dat";
		JSONArray results = new JSONArray();
		JSONArray fileContents = new JSONArray();
		
		File dataFile = new File(sDataFileName);
		if(!dataFile.exists())
		{
			//no file, we'll consider this a successful result with no results
			return fileContents;
		}
		if(!dataFile.canRead())
		{
			dataFile.setReadable(true);
		}
		if(!dataFile.canWrite())
		{
			dataFile.setWritable(true);
		}
			
		
		try(FileInputStream fileInStream = new FileInputStream(dataFile))
		{
			JSONParser parser = new JSONParser();
			try
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(fileInStream));
				String inputStr = null;
				while( (inputStr = reader.readLine()) != null) 
				{
					
					JSONObject json = (JSONObject)parser.parse(inputStr);
					fileContents.add(json);
				}
				
			}
			catch(ParseException e)
			{
				throw e;
			}
			finally
			{
				fileInStream.close();
			}
		}
		
		return fileContents;
	}
	
	private String GetResultsString(JSONArray arrResults)
	{
		String sResultsString = "";
		if(arrResults.size() > 1)
			sResultsString = "[\n" + ((JSONObject)arrResults.get(0)).toJSONString();
		else if(arrResults.size() == 1)
			sResultsString = ((JSONObject)arrResults.get(0)).toJSONString();
		
		for(int i = 1; i < arrResults.size(); ++i)
		{
			sResultsString += (",\n");
			
			sResultsString += ( ((JSONObject)arrResults.get(i)).toJSONString() );
		}
		
		if(arrResults.size() > 1)
			sResultsString += ("\n]");
		
		return sResultsString;
	}
 
	@GET
	@Path("/{noteID}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNote(@PathParam("noteID") String sNoteID ) throws IOException {
		JSONArray arrFileContents = new JSONArray();
		JSONArray arrResults = new JSONArray();
		
		try
		{
			arrFileContents = GetJSONFileContents();
		}
		catch(IOException e)
		{
			return Response.status(Response.Status.EXPECTATION_FAILED).entity("File operation failed.").build();
		}
		catch(ParseException e)
		{
			return Response.status(Response.Status.EXPECTATION_FAILED).entity("Parse failed.").build();
		}
		
		if(!sNoteID.isEmpty()) //find the note matching the ID specified
		{
			for(int i = 0; i < arrFileContents.size(); ++i)
			{
				JSONObject curObj = (JSONObject)arrFileContents.get(i);
				
				if(sNoteID.equals(curObj.get("id").toString()))
				{
					arrResults.add(curObj);
					break;
				}
			}
			
			if(arrResults.isEmpty())
			{
				return Response.status(Response.Status.OK).entity("Could not find ID matching " + sNoteID).build();
			}
		}
		else //no id string, return all contents of the file
		{
			arrResults = arrFileContents;
		}
		
		String sResultsString = GetResultsString(arrResults);
		
		return Response.status(Response.Status.OK).entity(sResultsString).build();
	}
	
	//This will handle the situation in which query is provided, or no ID is provided
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNote2(@DefaultValue("") @QueryParam("query") String sQueryVal) throws IOException {
		
		JSONArray arrFileContents = new JSONArray();
		JSONArray arrResults = new JSONArray();
		
		try
		{
			arrFileContents = GetJSONFileContents();
		}
		catch(IOException e)
		{
			return Response.status(Response.Status.EXPECTATION_FAILED).entity("File operation failed.").build();
		}
		catch(ParseException e)
		{
			return Response.status(Response.Status.EXPECTATION_FAILED).entity("Parse failed.").build();
		}
		
		//check for query value first
		if(!sQueryVal.isEmpty())
		{
			//find and return all results that include an instance of sQueryVal in the body
			for(int i = 0; i < arrFileContents.size(); ++i)
			{
				JSONObject curObj = (JSONObject)arrFileContents.get(i);
				
				String sBody = (String)curObj.get("body");
				if(sBody.contains(sQueryVal))
				{
					arrResults.add(curObj);
				}
			}
		}
		else //no query string, return all contents of the file
		{
			arrResults = arrFileContents;
		}
		
		String sResultsString = GetResultsString(arrResults);
		
		return Response.status(Response.Status.OK).entity(sResultsString).build();
	}
 
}