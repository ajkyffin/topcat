package org.icatproject.topcat;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.net.URLEncoder;

import org.icatproject.topcat.httpclient.*;
import org.icatproject.topcat.exceptions.*;
import org.icatproject.topcat.domain.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.json.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.icatproject.topcat.repository.CacheRepository;

public class IcatClient {

	private Logger logger = LoggerFactory.getLogger(IcatClient.class);

    private HttpClient httpClient;
    private String sessionId;
   
    public IcatClient(String url, String sessionId){
        this.httpClient = new HttpClient(url + "/icat");
        this.sessionId = sessionId;
    }

    public String getUserName() throws TopcatException {
    	try {
    		return parseJsonObject(httpClient.get("session/" + sessionId, new HashMap<String, String>()).toString()).getString("userName");
    	} catch (Exception e){
            throw new BadRequestException(e.getMessage());
    	}
    }

	public Boolean isAdmin() throws TopcatException {
		try {
			String[] adminUserNames = getAdminUserNames();
			String userName = getUserName();
			int i;

			for (i = 0; i < adminUserNames.length; i++) {
				if(userName.equals(adminUserNames[i])){
					return true;
				}
			}
		} catch(Exception e){}
		return false;
	}

	public String getFullName() throws TopcatException {
		try {
			String query = "select user.fullName from User user where user.name = :user";
			String url = "entityManager?sessionId=" + URLEncoder.encode(sessionId, "UTF8") + "&query=" + URLEncoder.encode(query, "UTF8");
    		String response = httpClient.get(url, new HashMap<String, String>()).toString();
    		try {
    			return parseJsonArray(response).getString(0);
    		} catch(Exception e){
    			return "";
    		}
    	} catch (Exception e){
            throw new BadRequestException(e.getMessage());
    	}
	}

	public List<JsonObject> getEntities(String entityType, List<Long> entityIds) throws TopcatException {
		List<JsonObject> out = new ArrayList<JsonObject>();
		try {
			entityIds = new ArrayList<Long>(entityIds);

			String queryPrefix;
			String querySuffix;

			if (entityType.equals("datafile")) {
				queryPrefix = "SELECT datafile from Datafile datafile where datafile.id in (";
				querySuffix = ") include datafile.dataset.investigation";
			} else if (entityType.equals("dataset")) {
				queryPrefix = "SELECT dataset from Dataset dataset where dataset.id in (";
				querySuffix = ") include dataset.investigation";
			} else {
				queryPrefix = "SELECT investigation from Investigation investigation where investigation.id in (";
				querySuffix = ")";
			}

			StringBuffer currentCandidateEntityIds = new StringBuffer();
			String currentPassedUrl = null;
			String currentCandidateUrl = null;

			List<String> passedUrls = new ArrayList<String>();

			while(entityIds.size() > 0){
				if (currentCandidateEntityIds.length() != 0) {
					currentCandidateEntityIds.append(",");
				}
				currentCandidateEntityIds.append(entityIds.get(0));
				currentCandidateUrl = "entityManager?sessionId="  + URLEncoder.encode(sessionId, "UTF8") + "&query=" + URLEncoder.encode(queryPrefix + currentCandidateEntityIds.toString() + querySuffix , "UTF8");
				if(httpClient.urlLength(currentCandidateUrl) > 2048){
					currentCandidateEntityIds = new StringBuffer();
					if(currentPassedUrl == null){
						break;
					}
					passedUrls.add(currentPassedUrl);
					currentPassedUrl = null;
				} else {
					currentPassedUrl = currentCandidateUrl;
					currentCandidateUrl = null;
					entityIds.remove(0);
				}
			}

			if(currentPassedUrl != null){
				passedUrls.add(currentPassedUrl);
			}

			for(String passedUrl : passedUrls){
				for(JsonValue entityValue : parseJsonArray(httpClient.get(passedUrl, new HashMap<String, String>()).toString())){
					JsonObject entity = (JsonObject) entityValue;
					out.add(entity.getJsonObject(entityType.substring(0, 1).toUpperCase() + entityType.substring(1)));
				}
			}

		} catch (Exception e) {
			throw new BadRequestException(e.getMessage());
		}

		return out;
	}

	public Long getSize(CacheRepository cacheRepository, List<Long> investigationIds, List<Long> datasetIds, List<Long> datafileIds) throws TopcatException {
		try {
			Long out = (long) 0;
			String query, url;
			Response response;
			InternalException internalException;

			datafileIds = new ArrayList<Long>(datafileIds);

			for(Long investigationId : investigationIds){
				String key = "getSize:investigation:" + investigationId;
				Long size = null;

				internalException = (InternalException) cacheRepository.get(key + ":internalException");

				if(internalException != null){
					throw internalException;
				}
				
				size = (Long) cacheRepository.get(key);


				if(size == null){
					query = "select sum(datafile.fileSize) from  Datafile datafile, datafile.dataset as dataset, dataset.investigation as investigation where investigation.id = " + investigationId;
					url = "entityManager?sessionId=" + URLEncoder.encode(sessionId, "UTF8") + "&query=" + URLEncoder.encode(query, "UTF8");
					response = httpClient.get(url, new HashMap<String, String>());
					if(response.getCode() >= 400){
						internalException = new InternalException(response.toString());
						cacheRepository.put(key + ":internalException", internalException);
						throw internalException;
					}
					try {
						size = ((JsonNumber) parseJsonArray(response.toString()).get(0)).longValue();
					} catch(Exception e) {
						size = (long) 0;
					}
					cacheRepository.put(key, size);
					
				}

				out += size;
			}

			for(Long datasetId : datasetIds){
				String key = "getSize:dataset:" + datasetId;
				Long size = null;

				internalException = (InternalException) cacheRepository.get(key + ":internalException");

				if(internalException != null){
					throw internalException;
				}
				
				size = (Long) cacheRepository.get(key);

				if(size == null){
					query = "select sum(datafile.fileSize) from  Datafile datafile, datafile.dataset as dataset where dataset.id = " + datasetId;
					url = "entityManager?sessionId=" + URLEncoder.encode(sessionId, "UTF8") + "&query=" + URLEncoder.encode(query, "UTF8");
					response = httpClient.get(url, new HashMap<String, String>());
					if(response.getCode() >= 400){
						internalException = new InternalException(response.toString());
						cacheRepository.put(key + ":internalException", internalException);
						throw internalException;
					}
					try {
						size = ((JsonNumber) parseJsonArray(response.toString()).get(0)).longValue();
					} catch(Exception e) {
						size = (long) 0;
					}
					cacheRepository.put(key, size);
				}

				out += size;
			}

			String queryPrefix = "select sum(datafile.fileSize) from Datafile datafile where datafile.id in (";
			String querySuffix = ")";

			StringBuffer currentCandidateEntityIds = new StringBuffer();
			String currentPassedUrl = null;
			String currentCandidateUrl = null;

			List<String> passedUrls = new ArrayList<String>();

			while(datafileIds.size() > 0){
				if (currentCandidateEntityIds.length() != 0) {
					currentCandidateEntityIds.append(",");
				}
				currentCandidateEntityIds.append(datafileIds.get(0));
				currentCandidateUrl = "entityManager?sessionId="  + URLEncoder.encode(sessionId, "UTF8") + "&query=" + URLEncoder.encode(queryPrefix + currentCandidateEntityIds.toString() + querySuffix , "UTF8");
				if(httpClient.urlLength(currentCandidateUrl) > 2048){
					currentCandidateEntityIds = new StringBuffer();
					if(currentPassedUrl == null){
						break;
					}
					passedUrls.add(currentPassedUrl);
					currentPassedUrl = null;
				} else {
					currentPassedUrl = currentCandidateUrl;
					currentCandidateUrl = null;
					datafileIds.remove(0);
				}
			}

			if(currentPassedUrl != null){
				passedUrls.add(currentPassedUrl);
			}

			for(String passedUrl : passedUrls){
				response = httpClient.get(passedUrl, new HashMap<String, String>());
				if(response.getCode() >= 400){
					throw new InternalException(response.toString());
				}
				try{
					out += ((JsonNumber) parseJsonArray(response.toString()).get(0)).longValue();
				} catch(Exception e) {}
			}

			return out;
		} catch(TopcatException e){
			throw e;
		} catch (Exception e) {
			throw new BadRequestException(e.getMessage());
		}
	}

	protected String[] getAdminUserNames() throws Exception {
		return Properties.getInstance().getProperty("adminUserNames", "").split("[ ]*,[ ]*");
	}

	//todo: merge into Util methods in 2.3.0
	private JsonObject parseJsonObject(String json) throws Exception {
        InputStream jsonInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonReader jsonReader = Json.createReader(jsonInputStream);
        JsonObject out = jsonReader.readObject();
        jsonReader.close();
        return out;
    }

    private JsonArray parseJsonArray(String json) throws Exception {
        InputStream jsonInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonReader jsonReader = Json.createReader(jsonInputStream);
        JsonArray out = jsonReader.readArray();
        jsonReader.close();
        return out;
    }
}

