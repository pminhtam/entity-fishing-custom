package com.scienceminer.nerd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.scienceminer.nerd.disambiguation.NerdEngine;
import com.scienceminer.nerd.embeddings.SimilarityScorer;
import com.scienceminer.nerd.exceptions.CustomisationException;
import com.scienceminer.nerd.exceptions.QueryException;
import com.scienceminer.nerd.exceptions.ResourceNotFound;
import com.scienceminer.nerd.kb.Lexicon;
import com.scienceminer.nerd.kb.UpperKnowledgeBase;
import com.scienceminer.nerd.mention.ProcessText;
import com.scienceminer.nerd.utilities.NerdConfig;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
import java.io.File;  // Import the File class
import java.util.Scanner; // Import the Scanner class to read text files
import java.io.FileWriter; 
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.lang.Language;

//*****************************************************
import org.grobid.core.*;
import org.grobid.core.data.*;
import org.grobid.core.factory.*;
import org.grobid.core.utilities.*;
import org.grobid.core.engines.Engine;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.data.Entity;
import org.grobid.core.data.Sense;
import org.grobid.core.engines.NERParsers;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * RESTFul service for the NERD system.
 */
@Singleton
@Path(NerdPaths.ROOT)
public class NerdRestService implements NerdPaths {
    private static final Logger LOGGER = LoggerFactory.getLogger(NerdRestService.class);
    private static final String SHA1 = "authToken";
    private static final String NAME = "name";
    private static final String PROFILE = "profile";
    private static final String VALUE = "value";
    private static final String QUERY = "query";
    private static final String XML = "xml";
    private static final String NERD = "nerd";
    private static final String TEXT = "text";
    private static final String TERM = "term";
    private static final String ID = "id";
    private static final String FILE = "file";
    private static final String LANG = "lang";
    private static final String DOI = "doi";
    private static final String NBEST = "nbest";
    private static final String SENTENCE = "sentence";
    private static final String FORMAT = "format";
    private static final String CUSTOMISATION = "customisation";

    NerdRestProcessQuery nerdProcessQuery;
    NerdRestProcessFile nerdProcessFile;
    NerdRestKB nerdRestKB;

    public NerdRestService() {
        LOGGER.info("Init lexicon.");
        Lexicon.getInstance();
        LOGGER.info("Init lexicon finished.");

        LOGGER.info("Init KB resources.");
        UpperKnowledgeBase.getInstance();
        LOGGER.info("Init KB resources finished.");

        nerdProcessQuery = new NerdRestProcessQuery();
        nerdProcessFile = new NerdRestProcessFile();
        nerdRestKB = new NerdRestKB();

        //Pre-instantiate
        ProcessText.getInstance();
        SimilarityScorer.getInstance();
        NerdEngine.getInstance();
    }

    /**
     * @see com.scienceminer.nerd.service.NerdRestProcessGeneric#isAlive()
     */
    @GET
    @Path(NerdPaths.IS_ALIVE)
    @Produces(MediaType.TEXT_PLAIN)
    public Response isAlive() {
        return NerdRestProcessGeneric.isAlive();
    }

    /**
     * @see NerdRestProcessGeneric#getVersion()
     */
    @GET
    @Path(NerdPaths.VERSION)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersion() {
        Response response = null;
        try {
            response = Response.status(Response.Status.OK)
                    .entity(NerdRestProcessGeneric.getVersion())
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }

    /**
     * Sentence Segmentation
     **/
    @GET
    @Path(SEGMENTATION)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processSentenceSegmentationGet(@QueryParam(TEXT) String text) {
        return NerdRestProcessString.processSentenceSegmentation(text);
    }


    @POST
    @Path(SEGMENTATION)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response processSentenceSegmentationPost(@FormDataParam(TEXT) String text) {
        return NerdRestProcessString.processSentenceSegmentation(text);
    }

    /** Language Identification **/

    /**
     * @see com.scienceminer.nerd.service.NerdRestProcessString#processLanguageIdentification(String)
     */
    @GET
    @Path(LANGUAGE)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processLanguageIdentificationGet(@QueryParam(TEXT) String text) {
        return NerdRestProcessString.processLanguageIdentification(text);
    }

    /**
     * @see com.scienceminer.nerd.service.NerdRestProcessString#processLanguageIdentification(String)
     */
    @POST
    @Path(LANGUAGE)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response processLanguageIdentificationPost(@FormDataParam(TEXT) String text) {
        return NerdRestProcessString.processLanguageIdentification(text);
    }

    private static String processLine(JSONObject query_json_file,JSONObject query_json,NerdRestProcessQuery nerdProcessQuery,JSONArray textField,String resultLangStr){
	    
	String query_string = "";
	for (int i =0 ; i<textField.size();i++){
		query_string += "  ";
		query_string += (String) query_json_file.get(textField.get(i));
	}
	//String query_string = (String) query_json_file.get(textField);
	JSONObject new_query_json = new JSONObject(query_json);
	new_query_json.put("text", query_string);
	//System.out.println(new_query_json.get("language"));
	//System.out.println(new_query_json.toString());
	JSONObject jsonLang = new JSONObject();
	jsonLang.put("lang",resultLangStr);
	new_query_json.put("language",jsonLang);

	//System.out.println(new_query_json.get("language"));
	//System.out.println(new_query_json.toString());
	//System.out.println("chay thread ne");
	String json = nerdProcessQuery.processQuery(new_query_json.toString());
	query_json_file.put("result",json);
	return query_json_file.toString();
						    //        myWriter.write(json+"\n");
	}
    @POST
    @Path(DISAMBIGUATE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processQueryJson(@FormDataParam(QUERY) String query,
                                     @FormDataParam(FILE) InputStream inputStream) {
        String json = null;
        Response response = null;
	JSONParser parser = new JSONParser();
        try {
		
	
            if (inputStream != null) {
                json = nerdProcessFile.processQueryAndPdfFile(query, inputStream);
            } else {
      		//File myObj = new File("/hdd/tam/entity-fishing/data_crawl/part-r-00004-9bfc16ff-e010-45c8-9905-4d66c4a66507");
  		//FileWriter myWriter = new FileWriter("/hdd/tam/entity-fishing/data_crawl/result_00004_.txt");
		
		//Object obj = parser.parse(query);
		Object obj = parser.parse("{ \"text\": \"\", \"shortText\": \"computer The number of passengers coming to underground\", \"termVector\": [], \"language\": { \"lang\": \"de\" }, \"entities\": [], \"mentions\": [ \"ner\", \"wikipedia\" ], \"nbest\": false, \"sentence\": false }");
		final JSONObject query_json = (JSONObject) obj;
		//String inputFile = "part-r-00004-9bfc16ff-e010-45c8-9905-4d66c4a66507";
      		//File myObj = new File("/hdd/tam/entity-fishing/data_crawl/"+ inputFile );
		Object obj_query_info = parser.parse(query);
		JSONObject obj_query_info_json = (JSONObject) obj_query_info;
		String inputPath =(String) obj_query_info_json.get("input");
		String outputPath = (String) obj_query_info_json.get("output");
		final JSONArray textField =(JSONArray) obj_query_info_json.get("text_field");
		String companyField = (String) obj_query_info_json.get("company_field");
		//final String textField =(String) obj_query_info_json.get("text_field");
		//System.out.println(textField.get(0));
        //System.out.println(query.toString());

		//System.out.println(textField.size());
        //LOGGER.info("Text size" + Integer.toString(textField.size()));
		
		
		final String textFieldLang =(String) textField.get(0);

      	File myObj = new File(inputPath);
  		FileWriter myWriter = new FileWriter(outputPath);
        Scanner myReader = new Scanner(myObj, "UTF-8");

        //System.out.println("InputPath ".concat(inputPath));
        //LOGGER.info("InputPath".concat(inputPath));
        //System.out.println("File size in bytes " + myObj.length());

        int numThreads = 15;
		
        Thread[] threads = new Thread[numThreads];
        final ArrayList<JSONObject> listData = new ArrayList<>(numThreads);
        ArrayList<String> listResult = new ArrayList<>(numThreads);
		Object obj2;

        //System.out.println("Open inputfile");
        //LOGGER.info("Open inputfile");
		
		LanguageUtilities languageIdentifier = LanguageUtilities.getInstance();
		final Language resultLang = null;
		//String resultLangStr = "";

        //System.out.println(String.valueOf(myReader.hasNextLine()));
        //LOGGER.info(String.valueOf(myReader.hasNextLine()));
	// Read list of company 
	ArrayList<String> listCompanyName = new ArrayList<>();
	File list_company = new File("list_company_name.txt");
	Scanner myReaderListCompanyName = new Scanner(list_company, "UTF-8");
	while (myReaderListCompanyName.hasNextLine()){
		String data = myReaderListCompanyName.nextLine();
		String companyName = data.split("\n")[0];
		//System.out.println(companyName);
		listCompanyName.add(companyName);
	}


        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            //System.out.println(data);
            //LOGGER.info(data);
		    //System.out.println(data);
		    obj2 = parser.parse(data);
		    JSONObject query_json_file = (JSONObject) obj2;
		    String companyQuery =(String) query_json_file.get(companyField);
		   if (!listCompanyName.contains(companyQuery)){
			   //System.out.println(companyQuery);
		   	continue;
		   }
		   System.out.println(companyQuery);
            listData.add(query_json_file);
		    //final listData;
            if (listData.size()>numThreads){
               for (int i = 0; i < threads.length; i++){
                  final int index = i+1;
			      System.out.println("chay vong lap thread");
			    //final obj = parser.parse(listData.get(index));
                            //final JSONObject query_json_file = (JSONObject) obj;
                            //final Object obj2 = parser.parse(query);
                            //final query_json = (JSONObject) obj2;
                            //String result = processLine(query_json_file,query_json,nerdProcessQuery);
			
                            threads[i] = new Thread(new Runnable() {
                               public void run() {
                                    //Object obj = parser.parse(listData.get(index));
                                    //JSONObject query_json_file = (JSONObject) obj;
                                    //Object obj2 = parser.parse(query);
                                    //JSONObject query_json = (JSONObject) obj2;
				    String resultLangStr = "en";
				    synchronized (languageIdentifier) {
					//resultLang = languageIdentifier.runLanguageId((String)listData.get(index).get(textField));
					resultLangStr = languageIdentifier.runLanguageId((String)listData.get(index).get(textFieldLang)).getLang();
					//if(resultLang!=null){
					//	resultLangStr = resultLang.getLang();
					//}
					System.out.println(resultLangStr);
				    }
                                    String result = processLine(listData.get(index),query_json,nerdProcessQuery,textField,resultLangStr);
                                    synchronized (listResult){
                                        listResult.add(result);
                                    }
                                }
                            });
			    threads[i].start();

                        }
                        for (int i = 0; i < threads.length; i++){
                            threads[i].join();
                        }
                        for (String e:listResult
                             ) {
                            myWriter.write(e+"\n");
                        }
                        listData.clear();
                        listResult.clear();
                    }
		    System.out.println("Het 1 vong lap");
                }
		

		//ExecutorService executorService = Executors.newFixedThreadPool(16);

		/*
	        whele (myReader.hasNextLine()) {
			String data = myReader.nextLine();

			obj = parser.parse(data);
			JSONObject query_json_file = (JSONObject) obj;
			String query_string = (String) query_json_file.get("abstract");
			
			query_json.put("text", query_string);
			json = nerdProcessQuery.processQuery(query_json.toString());
			myWriter.write(json+"\n");
			System.out.println(json);
			System.out.println(query_json_file["abstract"]);
		}
		*/
		myReader.close();
		myWriter.close();
		//obj = parser.parse(query);
		//query_json = (JSONObject) obj;
		System.out.println(query_json);

                //json = nerdProcessQuery.processQuery(query);
		//System.out.println(query);
		//System.out.println(json);
            }

            if (json == null) {
                response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(json)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

        } catch (QueryException qe) {
            return handleQueryException(qe, query);
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (GrobidException ge) {
            response = Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("The PDF cannot be processed by grobid. " + ge.getMessage())
                    .build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }
    @POST
    @Path("grobid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processGrobid(String query){
        String responseString = null;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Response response = null;
        try {
            String text = query;
            NerdConfig conf = mapper.readValue(new File("data/config/mention.yaml"), NerdConfig.class);
            String pGrobidHome = conf.getGrobidHome();
            String pGrobidProperties = "dependency_install/grobid-0.6.0/grobid-home/config/grobid.properties";
            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);

            System.out.println(">>>>>>>> GROBID_HOME=" + GrobidProperties.get_GROBID_HOME_PATH());

            NERParsers nerParser = new NERParsers();
            JSONArray ja_respone = new JSONArray();
            List<Entity> results = nerParser.extractNE(text);

            for(Entity entity: results) {
                System.out.println(entity.toJson());
                ja_respone.add((String)entity.toJson().toString());
//                ja_respone.add("aaaa");
            }
            response = Response
                    .status(Response.Status.OK)
                    .entity(ja_respone.toString())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();
        }
        catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;
    }
    /**
     * Same as processQueryJson when the user send only the query and can avoid using multipart/form-data
     */
    @POST
    @Path(DISAMBIGUATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processQueryJsonNoMultipart(String query) {
        String output = null;
        Response response = null;

        try {
            output = nerdProcessQuery.processQuery(query);

            if (output == null) {
                response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(output)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

        } catch (QueryException qe) {
            return handleQueryException(qe, query);
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }

    private Response handleResourceNotFound(ResourceNotFound re, String identifier) {
        Response response;

        String json = null;
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{ \"message\": \"The requested resource for identifier " + identifier + " could not be found but may be available in the future.\" }");
        json = jsonBuilder.toString();

        LOGGER.error(json);
        response = Response
                .status(Response.Status.NOT_FOUND)
                .entity(json)
                .build();
        return response;
    }

    private Response handleQueryException(QueryException qe, String query) {
        Response response;

        String message = "The sent query is invalid.";

        String json = null;
        StringBuilder jsonBuilder = new StringBuilder();

        switch (qe.getReason()) {

            case QueryException.LANGUAGE_ISSUE:
                message = "The language specified is not supported or not valid. ";
                jsonBuilder.append("{ \"message\": \"" + message + "\" }");
                json = jsonBuilder.toString();
                LOGGER.error(message, qe);
                response = Response
                        .status(Response.Status.NOT_ACCEPTABLE)
                        .entity(json)
                        .build();

                break;

            case QueryException.FILE_ISSUE:
                message = "There are issues with the posted PDF file. " + qe.getMessage();
                jsonBuilder.append("{ \"message\": \"" + message + "\" }");
                json = jsonBuilder.toString();
                LOGGER.error(message);
                response = Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(json)
                        .build();

                break;

            case QueryException.WRONG_IDENTIFIER:
                message = "Wrong identifier. " + qe.getMessage();
                jsonBuilder.append("{ \"message\": \"" + message + "\" }");
                json = jsonBuilder.toString();
                response = Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(json)
                        .build();
                break;

            case QueryException.INVALID_TERM:
                message = "Wrong term identifier. " + qe.getMessage();
                jsonBuilder.append("{ \"message\": \"" + message + "\" }");
                json = jsonBuilder.toString();
                response = Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(json)
                        .build();
                break;

            default:
                LOGGER.error(message + " Query sent: " + query, qe);
                response = Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity("The sent query is invalid. " + qe.getMessage())
                        .build();

                break;
        }

        return response;
    }

    /*@POST
    @Path(DISAMBIGUATE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    public Response processQueryXml(@FormDataParam(QUERY) String query,
                                    @FormDataParam(FILE) InputStream inputStream) {
        return Response.status(new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return 501;
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.SERVER_ERROR;
            }

            @Override
            public String getReasonPhrase() {
                return "Not implemented";
            }
        }).build();
    }*/


    /**
     * Admin API
     **/

    /*@Path(ADMIN)
    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response getAdmin_htmlGet(@QueryParam(SHA1) String sha1) {
        return NerdRestProcessAdmin.getAdminParams(sha1);
    }

    @Path(ADMIN_PROPERTIES)
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getAllProperties(@QueryParam(SHA1) String sha1) {
        return NerdRestProcessAdmin.getAllPropertiesValues(sha1);
    }

    @Path(ADMIN + "/property/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getProperty(@QueryParam(SHA1) String sha1, @PathParam(NAME) String propertyName) {
        return NerdRestProcessAdmin.getProperty(sha1, propertyName);
    }


    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path(ADMIN + "/property/{name}")
    @PUT
    public Response updateProperty(@QueryParam(SHA1) String sha1,
                                   @PathParam(NAME) String propertyName,
                                   @FormDataParam(VALUE) String newValue) {
        return NerdRestProcessAdmin.changePropertyValue(sha1, propertyName, newValue);
    }*/

    /**
     * KB operations
     **/
    @Path(KB + "/" + CONCEPT + "/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getConceptInformation(@PathParam(ID) String identifier,
                                          @DefaultValue(Language.EN) @QueryParam(LANG) String lang) {

        String output = null;
        Response response = null;

        try {
            output = nerdRestKB.getConceptInfo(identifier, lang);

            if (isBlank(output)) {
                response = Response.status(Response.Status.NOT_FOUND).build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(output)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

        } catch (ResourceNotFound re) {
            return handleResourceNotFound(re, identifier);
        } catch (QueryException qe) {
            return handleQueryException(qe, identifier);
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;
    }

    @GET
    @Path(KB + "/" + TERM + "/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTermLookup(@PathParam(TERM) String term,
                                  @DefaultValue("en") @QueryParam(LANG) String lang) {

        String output = null;
        Response response = null;

        try {
            output = nerdRestKB.getTermLookup(term, lang);

            if (isBlank(output)) {
                response = Response.status(Response.Status.NOT_FOUND).build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(output)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

        } catch (QueryException qe) {
            return handleQueryException(qe, term);
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;

    }

    @GET
    @Path(KB + "/" + DOI + "/{doi}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWikiedataIdByDOI(@PathParam(DOI) String doi) {

        String output = null;
        Response response = null;

        try {
            output = nerdRestKB.getWikidataIDByDOI(doi);

            if (isBlank(output)) {
                response = Response.status(Response.Status.NOT_FOUND).build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(output)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            LOGGER.error("An unexpected exception occurs. ", e);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return response;

    }

    /**
     * Customisation API
     **/
    @GET
    @Path(CUSTOMISATIONS)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomisations() {
        Response response = null;
        try {
            String output = NerdRestCustomisation.getCustomisations();

            response = Response
                    .status(Response.Status.OK)
                    .entity(output)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();

        } catch (CustomisationException ce) {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(responseJson(false, ce.getMessage()))
                    .build();

        } catch (Exception exp) {
            LOGGER.error("General error when accessing the list of existing customisations. ", exp);
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;
    }

    @GET
    @Path(CUSTOMISATION + "/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomisation(@PathParam(NAME) String name) {
        Response response = null;
        try {
            String output = NerdRestCustomisation.getCustomisation(name);
            if (output == null) {
                response = Response
                        .status(Response.Status.NOT_FOUND)
                        .build();
            } else {
                response = Response
                        .status(Response.Status.OK)
                        .entity(output)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }
        } catch (CustomisationException ce) {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(responseJson(false, ce.getMessage()))
                    .build();
        } catch (Exception exp) {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return response;

    }

    @PUT
    @Path(CUSTOMISATION + "/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateCustomisation(@PathParam(NAME) String name, @FormDataParam(VALUE) String newContent) {
        boolean ok = false;
        Response response = null;
        try {
            ok = NerdRestCustomisation.updateCustomisation(name, newContent);

            response = Response
                    .status(Response.Status.OK)
                    .entity(responseJson(ok, null))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();

        } catch (CustomisationException ce) {
            response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(responseJson(ok, ce.getMessage()))
                    .build();

        } catch (Exception e) {
            response = Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return response;


    }

    @POST
    @Path(CUSTOMISATIONS)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addCustomisation(@FormDataParam(NAME) String name, @FormDataParam(VALUE) String content) {
        boolean ok = false;
        Response response = null;
        try {
            ok = NerdRestCustomisation.createCustomisation(name, content);
            response = Response
                    .status(Response.Status.OK)
                    .entity(responseJson(ok, null))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();

        } catch (CustomisationException ce) {
            response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(responseJson(ok, ce.getMessage()))
                    .build();

        } catch (Exception e) {
            response = Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return response;
    }


    @Path(CUSTOMISATION + "/{name}")
    @DELETE
    public Response processDeleteNerdCustomisation(@PathParam(NAME) String name) {
        boolean ok = false;
        Response response = null;
        try {
            ok = NerdRestCustomisation.deleteCustomisation(name);
            response = Response
                    .status(Response.Status.OK)
                    .entity(responseJson(ok, null))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();

        } catch (CustomisationException ce) {
            response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(responseJson(ok, ce.getMessage()))
                    .build();

        } catch (Exception e) {
            response = Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return response;
    }


    private static String responseJson(boolean ok, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"ok\": \"" + ok + "\"");
        if (message != null) {
            sb.append(", \"status\": \"" + message + "\"");
        }
        sb.append("}");

        return sb.toString();
    }

}
