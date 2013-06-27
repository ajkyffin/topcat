/**
 *
 * Copyright (c) 2009-2013
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * Neither the name of the STFC nor the names of its contributors may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package uk.ac.stfc.topcat.ejb.manager;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import uk.ac.stfc.topcat.core.gwt.module.exception.TopcatException;
import uk.ac.stfc.topcat.core.icat.ICATWebInterfaceBase;
import uk.ac.stfc.topcat.ejb.entity.TopcatUserDownload;
import uk.ac.stfc.topcat.ejb.entity.TopcatUserSession;

/**
 * This is used to get data prior to file upload.
 */

public class DownloadManager {

    private final static Logger logger = Logger.getLogger(DownloadManager.class.getName());

    /**
     * Get a list of downloads for a user, which are associated with the given
     * facility.
     * 
     * @param manager
     * @param topcatSessionId
     *            a string containing the session id
     * @param facilityName
     *            a string containing the facility name
     * @return
     * @throws TopcatException
     */
    public List<TopcatUserDownload> getMyDownloadList(EntityManager manager, String topcatSessionId, String facilityName)
            throws TopcatException {
        logger.info("getMyDownloadList: topcatSessionId (" + topcatSessionId + "), facilityName (" + facilityName + ")");
        TopcatUserSession userSession = UserManager.getValidUserSessionByTopcatSessionAndServerName(manager,
                topcatSessionId, facilityName);
        manager.createNamedQuery("TopcatUserDownload.cleanup").executeUpdate();
        @SuppressWarnings("unchecked")
        List<TopcatUserDownload> userDownloads = manager.createNamedQuery("TopcatUserDownload.findByUserId")
                .setParameter("userId", userSession.getUserId()).getResultList();
        return userDownloads;
    }

    /**
     * Add a new record.
     * 
     * @param manager
     * @param topcatSessionId
     *            a string containing the session id
     * @param facilityName
     *            a string containing the facility name
     * @param submitTime
     * @param downloadName
     * @param status
     * @param expiryTime
     * @param url
     * @param preparedId
     * @return
     * @throws TopcatException
     */
    public Long addMyDownload(EntityManager manager, String topcatSessionId, String facilityName, Date submitTime,
            String downloadName, String status, Date expiryTime, String url, String preparedId) throws TopcatException {
        logger.info("addMyDownload: topcatSessionId (" + topcatSessionId + "), facilityName (" + facilityName
                + "), downloadName (" + downloadName + "), status (" + status + "), url (" + url + "), preparedId ("
                + preparedId + ")");
        TopcatUserSession userSession = UserManager.getValidUserSessionByTopcatSessionAndServerName(manager,
                topcatSessionId, facilityName);
        TopcatUserDownload download = new TopcatUserDownload();
        download.setName(downloadName);
        download.setUrl(url);
        download.setPreparedId(preparedId);
        download.setStatus(status);
        download.setSubmitTime(submitTime);
        download.setUserId(userSession.getUserId());
        download.setExpiryTime(expiryTime);
        manager.persist(download);
        return download.getId();
    }

    /**
     * Remove the record with the given id.
     * 
     * @param manager
     * @param topcatSessionId
     *            a string containing the session id
     * @param id
     *            the id of the record to remove
     */
    public void removeDownload(EntityManager manager, String topcatSessionId, Long id) {
        logger.info("removeDownload: topcatSessionId (" + topcatSessionId + "), id (" + id + ")");
        manager.createNamedQuery("TopcatUserDownload.cleanupById").setParameter("id", id).executeUpdate();
        manager.flush();
    }

    /**
     * Update the status and the url of the record with the given id.
     * 
     * @param manager
     * @param topcatSessionId
     *            a string containing the session id
     * @param id
     *            the id of the record to update
     * @param url
     *            the updated url
     * @param status
     *            the updated status
     */
    public void updateStatus(EntityManager manager, String topcatSessionId, Long id, String url, String status) {
        logger.info("updateStatus: topcatSessionId (" + topcatSessionId + "), id (" + id + "), url (" + url
                + "), status (" + status + ")");
        manager.createNamedQuery("TopcatUserDownload.updateStatusById").setParameter("id", id).setParameter("url", url)
                .setParameter("status", status).executeUpdate();
        manager.flush();
    }

    @Deprecated
    public void updateDownloadStatus(EntityManager manager, String topcatSessionId, String facilityName, String url,
            String updatedUrl, String status) {
        logger.info("updateDownloadStatus: topcatSessionId (" + topcatSessionId + "), facilityName (" + facilityName
                + "), url (" + url + "), updatedUrl (" + updatedUrl + "), status (" + status + ")");
        manager.createNamedQuery("TopcatUserDownload.updateStatus").setParameter("url", url)
                .setParameter("updatedUrl", updatedUrl).setParameter("status", status).executeUpdate();
        manager.flush();
    }

    @Deprecated
    public String getDatafilesDownloadURL(EntityManager manager, String topcatSessionId, String facilityName,
            List<Long> datafileIds) throws TopcatException {
        logger.info("getDatafilesDownloadURL: topcatSessionId (" + topcatSessionId + "), facilityName (" + facilityName
                + "), datafileIds.size (" + datafileIds.size() + ")");
        String result = "";
        try {
            TopcatUserSession userSession = UserManager.getValidUserSessionByTopcatSessionAndServerName(manager,
                    topcatSessionId, facilityName);
            ICATWebInterfaceBase service = ICATInterfaceFactory.getInstance().createICATInterface(facilityName,
                    userSession.getUserId().getServerId().getVersion(),
                    userSession.getUserId().getServerId().getServerUrl());
            return service.downloadDatafiles(userSession.getIcatSessionId(), datafileIds);
        } catch (MalformedURLException ex) {
            logger.error("getDatafilesDownloadURL: " + ex.getMessage());
        }
        return result;
    }

    /**
     * Get the URL of a file that contains the requested data set for the given
     * facility.
     * 
     * @param manager
     * @param topcatSessionId
     *            a string containing the session id
     * @param facilityName
     *            a string containing the facility name
     * @param datasetId
     *            the data set id
     * @return a string containing a URL
     * @throws TopcatException
     */
    @Deprecated
    public String getDatasetDownloadURL(EntityManager manager, String topcatSessionId, String facilityName,
            Long datasetId) throws TopcatException {
        logger.info("getDatasetDownloadURL: topcatSessionId (" + topcatSessionId + "), facilityName (" + facilityName
                + "), datasetId (" + datasetId + ")");
        String result = "";
        try {
            TopcatUserSession userSession = UserManager.getValidUserSessionByTopcatSessionAndServerName(manager,
                    topcatSessionId, facilityName);
            ICATWebInterfaceBase service = ICATInterfaceFactory.getInstance().createICATInterface(facilityName,
                    userSession.getUserId().getServerId().getVersion(),
                    userSession.getUserId().getServerId().getServerUrl());
            return service.downloadDataset(userSession.getIcatSessionId(), datasetId);
        } catch (MalformedURLException ex) {
            logger.error("getDatasetDownloadURL: " + ex.getMessage());
        }
        return result;
    }

}