/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.stfc.topcat.icatclient.v331;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import uk.ac.stfc.topcat.core.exception.AuthenticationException;
import uk.ac.stfc.topcat.core.exception.ICATMethodNotFoundException;
import uk.ac.stfc.topcat.core.gwt.module.TAdvancedSearchDetails;
import uk.ac.stfc.topcat.core.gwt.module.TDatafile;
import uk.ac.stfc.topcat.core.gwt.module.TDatafileParameter;
import uk.ac.stfc.topcat.core.gwt.module.TDataset;
import uk.ac.stfc.topcat.core.gwt.module.TFacilityCycle;
import uk.ac.stfc.topcat.core.gwt.module.TInvestigation;
import uk.ac.stfc.topcat.core.icat.ICATWebInterfaceBase;

/**
 *
 * @author sn65
 */
public class ICATInterfacev331 extends ICATWebInterfaceBase {

    private ICAT service;
    private String serverName;

    public ICATInterfacev331(String serverURL, String serverName) throws MalformedURLException {
        service = new ICATService(new URL(serverURL), new QName("client.icat3.uk", "ICATService")).getICATPort();
        this.serverName = serverName;
    }

   public String loginLifetime(String username, String password, int hours)throws AuthenticationException {
        String result=new String();
        try {
            result=service.loginLifetime(username, password, hours);
        } catch (SessionException_Exception ex) {
            throw new AuthenticationException("ICAT Server not available");
        } catch (javax.xml.ws.WebServiceException ex) {
            throw new AuthenticationException("ICAT Server not available");
        }
        return result;
    }

    public void logout(String sessionId) throws AuthenticationException{
        try {
            service.logout(sessionId);
        } catch (javax.xml.ws.WebServiceException ex) {
            throw new AuthenticationException("ICAT Server not available");
        }
    }

    public Boolean isSessionValid(String sessionId){
        Boolean result = new Boolean(false);
        try{
            //TODO: Hack here because the old icat didn't have a method to check
            //session validity
            service.getAllKeywords(sessionId, KeywordType.ALL);
            result=Boolean.TRUE;
        }catch(SessionException_Exception ex){
        } catch (javax.xml.ws.WebServiceException ex) {
        }
        return result;
    }
    
    public ArrayList<String> listInstruments(String sessionId) {
        ArrayList<String> instruments = new ArrayList<String>();
        try {
            instruments.addAll(service.listInstruments(sessionId));
        } catch (java.lang.NullPointerException ex) {
        } catch (SessionException_Exception ex) {
        }
        return instruments;
    }

    public ArrayList<String> listInvestigationTypes(String sessionId) {
        ArrayList<String> investigationTypes = new ArrayList<String>();
        try {
            investigationTypes.addAll(service.listInvestigationTypes(sessionId));
        } catch (SessionException_Exception ex) {
        } catch (java.lang.NullPointerException ex) {
        }
        return investigationTypes;
    }

    public ArrayList<TFacilityCycle> listFacilityCycles(String sessionId) throws ICATMethodNotFoundException {
        throw new ICATMethodNotFoundException("v331 doesn't support facility cycles method");
    }

    public ArrayList<TFacilityCycle> listFacilityCyclesForInstrument(String sessionId, String instrument) throws ICATMethodNotFoundException {
         throw new ICATMethodNotFoundException("v331 doesn't support facility cycles method");
    }

    public ArrayList<TInvestigation> getMyInvestigationsIncludesPagination(String sessionId, int start, int end) {
        ArrayList<TInvestigation> investigationList = new ArrayList<TInvestigation>();
        try {
            List<Investigation> resultInv = service.getMyInvestigationsIncludesPagination(sessionId, InvestigationInclude.NONE, 0, 200);
            for (Investigation inv : resultInv) {
                investigationList.add(copyInvestigationToTInvestigation(serverName,inv));
            }
        } catch (SessionException_Exception ex) {
        }
        return investigationList;
    }

    public ArrayList<TInvestigation> searchByAdvancedPagination(String sessionId, TAdvancedSearchDetails details, int start, int end) {
        ArrayList<TInvestigation> investigationList = new ArrayList<TInvestigation>();
        AdvancedSearchDetails inputParams = convertToAdvancedSearchDetails(details);

        try {
            List<Investigation> resultInv = service.searchByAdvancedPagination(sessionId, inputParams, 0, 200);
            for (Investigation inv : resultInv) {
                investigationList.add(copyInvestigationToTInvestigation(serverName,inv));
            }
        } catch (SessionException_Exception ex) {
        }
        return investigationList;
    }

    public ArrayList<TDataset> getDatasetsInInvestigation(String sessionId, Long investigationId) {
        ArrayList<TDataset> datasetList = new ArrayList<TDataset>();
        try {
            Investigation resultInv = service.getInvestigationIncludes(sessionId, Long.valueOf(investigationId), InvestigationInclude.DATASETS_ONLY);
            List<Dataset> dList = resultInv.getDatasetCollection();
            for (Dataset dataset : dList) {
                datasetList.add(new TDataset(serverName, dataset.getId().toString(), dataset.getName(), dataset.getDescription(), dataset.getDatasetType(), dataset.getDatasetStatus()));
            }
        } catch (InsufficientPrivilegesException_Exception ex) {
        } catch (NoSuchObjectFoundException_Exception ex) {
        } catch (SessionException_Exception ex) {
        }
        return datasetList;
    }

    public ArrayList<TDatafile> getDatafilesInDataset(String sessionId, Long datasetId) {
        ArrayList<TDatafile> datafileList = new ArrayList<TDatafile>();
        try {
            Dataset resultInv;
            resultInv = service.getDatasetIncludes(sessionId, Long.valueOf(datasetId), DatasetInclude.DATASET_AND_DATAFILES_ONLY);
            List<Datafile> dList = resultInv.getDatafileCollection();
            for (Datafile datafile : dList) {
                datafileList.add(copyDatafileToTDatafile(serverName,datafile));
            }
        } catch (SessionException_Exception ex) {
        } catch (InsufficientPrivilegesException_Exception ex) {
        } catch (NoSuchObjectFoundException_Exception ex) {
        }
        return datafileList;
    }

    public ArrayList<TDatafileParameter> getParametersInDatafile(String sessionId, Long datafileId) {
        ArrayList<TDatafileParameter> result = new ArrayList<TDatafileParameter>();
        try {
            Datafile df = service.getDatafile(sessionId, Long.valueOf(datafileId));
            List<DatafileParameter> dfList = df.getDatafileParameterCollection();
            for (DatafileParameter dfParam : dfList) {
                if (dfParam.isNumeric()) {
                    result.add(new TDatafileParameter(dfParam.getDatafileParameterPK().getName(), dfParam.getDatafileParameterPK().getUnits(), dfParam.getNumericValue().toString()));
                } else {
                    result.add(new TDatafileParameter(dfParam.getDatafileParameterPK().getName(), dfParam.getDatafileParameterPK().getUnits(), dfParam.getStringValue()));
                }
            }
        } catch (SessionException_Exception ex) {
        } catch (InsufficientPrivilegesException_Exception ex) {
        } catch (NoSuchObjectFoundException_Exception ex) {
        }
        return result;    }

    public String downloadDatafiles(String sessionId, ArrayList<Long> datafileIds) {
        String result = "";
        Pattern pattern = Pattern.compile("<input type=\"hidden\" name=\"requestID\" value=\"");
        Matcher matcher;
        try {
            String url = service.downloadDatafiles(sessionId, datafileIds);
            URL srb = new URL(url);
            URLConnection srbc = srb.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(srbc.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                matcher = pattern.matcher(inputLine);
                if (matcher.lookingAt()) {
                    String delimiter = "\"";
                    String[] temp = inputLine.split(delimiter);
                    result = srb.getProtocol() + "://" + srb.getHost() + "/dp/download/?requestID=" + temp[5];
                    break;
                }
            }
            in.close();
        } catch (InsufficientPrivilegesException_Exception ex) {
        } catch (NoSuchObjectFoundException_Exception ex) {
        } catch (SessionException_Exception ex) {
        } catch (IOException ex) {
        }
        return result;
    }

    public String downloadDataset(String sessionId, Long datasetId) {
        String result = "";
        Pattern pattern = Pattern.compile("<input type=\"hidden\" name=\"requestID\" value=\"");
        Matcher matcher;
        try {
            String url = service.downloadDataset(sessionId, datasetId);
            URL srb = new URL(url);
            URLConnection srbc = srb.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(srbc.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                matcher = pattern.matcher(inputLine);
                if (matcher.lookingAt()) {
                    String delimiter = "\"";
                    String[] temp = inputLine.split(delimiter);
                    result = srb.getProtocol() + "://" + srb.getHost() + "/dp/download/?requestID=" + temp[5];
                    break;
                }
            }
            in.close();
        } catch (InsufficientPrivilegesException_Exception ex) {
        } catch (NoSuchObjectFoundException_Exception ex) {
        } catch (SessionException_Exception ex) {
        } catch (IOException ex) {
        }
        return result;
    }

    public ArrayList<String> getKeywordsForUser(String sessionId) {
        ArrayList<String> resultKeywords = new ArrayList<String>();
        try {
            resultKeywords.addAll(service.getKeywordsForUser(sessionId));
        } catch (SessionException_Exception ex) {
        }
        return resultKeywords;
    }

    public ArrayList<String> getKeywordsInInvestigation(String sessionId, Long investigationId) {
        ArrayList<String> keywords = new ArrayList<String>();
        try {
            Investigation resultInvestigation = service.getInvestigationIncludes(sessionId, investigationId, InvestigationInclude.KEYWORDS_ONLY);
             List<Keyword> resultKeywords = resultInvestigation.getKeywordCollection();
             for(Keyword key:resultKeywords){
                 keywords.add(key.getKeywordPK().getName());
             }
        } catch (InsufficientPrivilegesException_Exception ex) {
        } catch (NoSuchObjectFoundException_Exception ex) {
        } catch (SessionException_Exception ex) {
        }
        return keywords;
    }

    public ArrayList<TInvestigation> searchByKeywords(String sessionId, ArrayList<String> keywords) {
        // call the search using keyword method
        List<Investigation> resultInvestigations = null;
        ArrayList<TInvestigation> returnTInvestigations = new ArrayList<TInvestigation>();
        try {
            resultInvestigations = service.searchByKeywords(sessionId, keywords);
        } catch (SessionException_Exception ex) {
        } catch (Exception ex) {
        }

        if (resultInvestigations != null) { // There are some result investigations
            for (Investigation inv : resultInvestigations) {
                returnTInvestigations.add(copyInvestigationToTInvestigation(serverName,inv));
            }
        }
        return returnTInvestigations;
    }

    public ArrayList<TDatafile> searchByRunNumber(String sessionId,ArrayList<String> instruments,float startRunNumber,float endRunNumber){
        List<Datafile> resultDatafiles = null;
        ArrayList<TDatafile> returnTDatafiles = new ArrayList<TDatafile>();
        try {
            resultDatafiles = service.searchByRunNumber(sessionId, instruments, startRunNumber, endRunNumber);
        } catch (SessionException_Exception ex) {
        }
        if (resultDatafiles != null) { // There are some result investigations
            for (Datafile datafile : resultDatafiles) {
                returnTDatafiles.add(copyDatafileToTDatafile(serverName,datafile));
            }
        }
        return returnTDatafiles;
    }

    public ArrayList<String> getKeywordsForUserWithStartMax(String sessionId, String partialKey, int numberOfKeywords) {
        ArrayList<String> resultKeywords = new ArrayList<String>();
        try {
            resultKeywords.addAll( service.getKeywordsForUserStartWithMax(sessionId,partialKey,numberOfKeywords));
        } catch (SessionException_Exception ex) {
        }
        return resultKeywords;
    }

    private AdvancedSearchDetails convertToAdvancedSearchDetails(TAdvancedSearchDetails searchDetails){
        AdvancedSearchDetails resultDetails = new AdvancedSearchDetails();
        try {
            resultDetails.setInvestigationName(searchDetails.getPropostaltitle());
            resultDetails.setInvestigationAbstract(searchDetails.getProposalAbstract());
            resultDetails.getInvestigators().addAll(searchDetails.getInvestigatorNameList());
            GregorianCalendar gc = new GregorianCalendar();
            if (searchDetails.getStartDate() != null) {
                gc.setTime(searchDetails.getStartDate());
                resultDetails.setDateRangeStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc));
            }
            if (searchDetails.getEndDate() != null) {
                gc.setTime(searchDetails.getEndDate());
                resultDetails.setDateRangeEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc));
            }
            resultDetails.setDatafileName(searchDetails.getDatafileName());
            resultDetails.getInstruments().addAll(searchDetails.getInstrumentList());
            if (searchDetails.getGrantId() != null) {
                resultDetails.setGrantId(Long.parseLong(searchDetails.getGrantId()));
            }
            if (searchDetails.getInvestigationTypeList().size() != 0) {
                resultDetails.setInvestigationType(searchDetails.getInvestigationTypeList().get(0));
            }
            if (searchDetails.getRbNumberStart() != null) {
                resultDetails.setRunStart(Double.valueOf(searchDetails.getRbNumberStart()));
            }
            if (searchDetails.getRbNumberEnd() != null) {
                resultDetails.setRunEnd(Double.valueOf(searchDetails.getRbNumberEnd()));
            }

            resultDetails.setSampleName(searchDetails.getSample());
            resultDetails.getKeywords().addAll(searchDetails.getKeywords());
        } catch (DatatypeConfigurationException ex) {
        }
        return resultDetails;
    }

    private TInvestigation copyInvestigationToTInvestigation(String serverName,Investigation inv){
        String id = inv.getId().toString();
        Date invStartDate = null;
        Date invEndDate = null;
        if (inv.getInvStartDate() != null) {
            invStartDate = inv.getInvStartDate().toGregorianCalendar().getTime();
        }
        if (inv.getInvEndDate() != null) {
            invEndDate = inv.getInvEndDate().toGregorianCalendar().getTime();
        }
        return new TInvestigation(id, inv.getVisitId(), serverName, inv.getTitle(), invStartDate, invEndDate);
    }

     private TDatafile copyDatafileToTDatafile(String serverName, Datafile datafile) {
        String format = "";
        String formatVersion = "";
        String formatType = "";
        Date createDate = null;
        if (datafile.getDatafileFormat() != null) {

            if (datafile.getDatafileFormat().getDatafileFormatPK() != null) {
                format = datafile.getDatafileFormat().getDatafileFormatPK().getName();
                formatVersion = datafile.getDatafileFormat().getDatafileFormatPK().getVersion();
            }
            formatType = datafile.getDatafileFormat().getFormatType();
        }
        if (datafile.getDatafileCreateTime() != null) {
            createDate = datafile.getDatafileCreateTime().toGregorianCalendar().getTime();
        }
        return new TDatafile(serverName, datafile.getId().toString(), datafile.getName(), datafile.getFileSize(), format, formatVersion, formatType, createDate);
    }

}