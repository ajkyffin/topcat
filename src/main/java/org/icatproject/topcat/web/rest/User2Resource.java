package org.icatproject.topcat.web.rest;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.ParseException;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.icatproject.ids.client.DataSelection;
import org.icatproject.ids.client.IdsClient.Flag;
import org.icatproject.topcat.Constants;
import org.icatproject.topcat.domain.Cart;
import org.icatproject.topcat.domain.CartDTO;
import org.icatproject.topcat.domain.CartItem;
import org.icatproject.topcat.domain.CartSubmitDTO;
import org.icatproject.topcat.domain.Download;
import org.icatproject.topcat.domain.DownloadItem;
import org.icatproject.topcat.domain.DownloadStatus;
import org.icatproject.topcat.domain.EntityType;
import org.icatproject.topcat.domain.LongValue;
import org.icatproject.topcat.domain.ParentEntity;
import org.icatproject.topcat.domain.StringValue;
import org.icatproject.topcat.exceptions.BadRequestException;
import org.icatproject.topcat.exceptions.ForbiddenException;
import org.icatproject.topcat.exceptions.TopcatException;
import org.icatproject.topcat.exceptions.NotFoundException;
import org.icatproject.topcat.icatclient.ICATClientBean;
import org.icatproject.topcat.idsclient.IdsClientBean;
import org.icatproject.topcat.repository.CartRepository;
import org.icatproject.topcat.repository.DownloadRepository;
import org.icatproject.topcat.utils.CartUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@LocalBean
@Path("v1/user")
public class User2Resource {
    private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

    @EJB
    private DownloadRepository downloadRepository;

    @EJB
    private CartRepository cartRepository;

    @EJB
    private ICATClientBean icatClientService;

    @EJB
    private IdsClientBean idsClientService;

    @PersistenceContext(unitName="topcatv2")
    EntityManager em;

    @GET
    @Path("/downloads")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDownloads(
        @QueryParam("icatUrl") String icatUrl,
        @QueryParam("sessionId") String sessionId,
        @QueryParam("queryOffset") String queryOffset)
        throws TopcatException, MalformedURLException, ParseException {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("userName", icatClientService.getUserName(icatUrl, sessionId));
        params.put("queryOffset", queryOffset);

        List<Download> downloads = new ArrayList<Download>();
        downloads = downloadRepository.getDownloads(params);

        return Response.ok().entity(new GenericEntity<List<Download>>(downloads){}).build();
    }

    @PUT
    @Path("/download/{id}/isDeleted")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteDownload(
        @PathParam("id") Long id,
        @FormParam("icatUrl") String icatUrl,
        @FormParam("sessionId") String sessionId,
        @FormParam("value") Boolean value)
        throws TopcatException, MalformedURLException, ParseException {

        Download download = downloadRepository.getDownload(id);
        if(download == null){
            throw new NotFoundException("could not find download");
        }

        String userName = icatClientService.getUserName(icatUrl, sessionId);
        if(!download.getUserName().equals(userName)){
            throw new ForbiddenException("You do not have permission to delete this download");
        }

        download.setIsDeleted(value);
        if(value){
            download.setDeletedAt(new Date());
        }

        downloadRepository.save(download);

        return Response.ok().build();
    }

    
    @GET
    @Path("/cart/{facilityName}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCart(
        @PathParam("facilityName") String facilityName,
        @QueryParam("icatUrl") String icatUrl,
        @QueryParam("sessionId") String sessionId)
        throws TopcatException, MalformedURLException, ParseException {

        String userName = icatClientService.getUserName(icatUrl, sessionId);
        Cart cart = cartRepository.getCart(userName, facilityName);
        
        if(cart != null){
            em.refresh(cart);
            return Response.ok().entity(cart).build();
        } else {
            return emptyCart(facilityName, userName);
        }
    }

    @POST
    @Path("/cart/{facilityName}/cartItem")
    @Produces({MediaType.APPLICATION_JSON})
    public Response addCartItem(
        @PathParam("facilityName") String facilityName,
        @FormParam("icatUrl") String icatUrl,
        @FormParam("sessionId") String sessionId,
        @FormParam("entityType") String entityType,
        @FormParam("entityId") Long entityId)
        throws TopcatException, MalformedURLException, ParseException {

        String userName = icatClientService.getUserName(icatUrl, sessionId);
        Cart cart = cartRepository.getCart(userName, facilityName);
        String name = icatClientService.getEntityName(icatUrl, sessionId, entityType, entityId);

        if(cart == null){
            cart = new Cart();
            cart.setFacilityName(facilityName);
            cart.setUserName(userName);
            em.persist(cart);
            em.flush();
        }

        em.refresh(cart);

        for(CartItem cartItem : cart.getCartItems()){
            if(cartItem.getEntityType() == EntityType.valueOf(entityType) && cartItem.getEntityId() == entityId){
                return Response.ok().entity(cart).build();
            }
        }

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setEntityType(EntityType.valueOf(entityType));
        cartItem.setEntityId(entityId);
        cartItem.setName(name);
        em.persist(cartItem);
        

        List<ParentEntity> parentEntities = icatClientService.getParentEntities(icatUrl, sessionId, entityType, entityId);
        for(ParentEntity parentEntity : parentEntities){
            parentEntity.setCartItem(cartItem);
            em.persist(parentEntity);
        }

        em.flush();
        em.refresh(cart);
        
        return Response.ok().entity(cart).build();
    }


    @DELETE
    @Path("/cart/{facilityName}/cartItem/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteCartItem(
        @PathParam("facilityName") String facilityName,
        @PathParam("id") Long id,
        @QueryParam("icatUrl") String icatUrl,
        @QueryParam("sessionId") String sessionId)
        throws TopcatException, MalformedURLException, ParseException {

        String userName = icatClientService.getUserName(icatUrl, sessionId);
        Cart cart = cartRepository.getCart(userName, facilityName);
        if(cart == null){
            return emptyCart(facilityName, userName);
        }

        CartItem cartItem = em.find(CartItem.class, id);
        if(cartItem != null){
            em.remove(cartItem);
            em.flush();
        }

        em.refresh(cart);
        if(cart.getCartItems().size() == 0){
            em.remove(cart);
            em.flush();
            return emptyCart(facilityName, userName);
        }

        return Response.ok().entity(cart).build();
    }

    @POST
    @Path("/cart/{facilityName}/submit")
    @Produces({MediaType.APPLICATION_JSON})
    public Response submitCart(
        @PathParam("facilityName") String facilityName,
        @FormParam("icatUrl") String icatUrl,
        @FormParam("sessionId") String sessionId,
        @FormParam("transport") String transport,
        @FormParam("transportUrl") String transportUrl,
        @FormParam("email") String email,
        @FormParam("fileName") String fileName,
        @FormParam("zipType") String zipType)
        throws TopcatException, MalformedURLException, ParseException {

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BadRequestException("fileName is required");
        }

        if (transport == null || transport.trim().isEmpty()) {
            throw new BadRequestException("transport is required");
        }

        String userName = icatClientService.getUserName(icatUrl, sessionId);
        Cart cart = cartRepository.getCart(userName, facilityName);
        String fullName = icatClientService.getFullName(icatUrl, sessionId);
        
        if(cart != null){
            em.refresh(cart);

            String preparedId = idsClientService.prepareData(transportUrl, sessionId, cartToDataSelection(cart), getZipFlag(zipType));

            if (preparedId != null) {
                Download download = new Download();
                download.setPreparedId(preparedId);
                download.setFacilityName(cart.getFacilityName());
                download.setFileName(fileName);
                download.setUserName(cart.getUserName());
                download.setFullName(fullName);
                download.setTransport(transport);
                download.setTransportUrl(transportUrl);
                download.setIcatUrl(icatUrl);
                download.setEmail(email);
                boolean isTwoLevel = idsClientService.isTwoLevel(transportUrl);
                download.setIsTwoLevel(isTwoLevel);
                if (isTwoLevel == true) {
                    download.setStatus(DownloadStatus.RESTORING);
                } else {
                    download.setStatus(DownloadStatus.COMPLETE);
                    download.setCompletedAt(new Date());
                }

                List<DownloadItem> downloadItems = new ArrayList<DownloadItem>();

                for(CartItem cartItem : cart.getCartItems()) {
                    DownloadItem downloadItem = new DownloadItem();
                    downloadItem.setEntityId(cartItem.getEntityId());
                    downloadItem.setEntityType(cartItem.getEntityType());
                    downloadItem.setDownload(download);
                    downloadItems.add(downloadItem);
                }

                download.setDownloadItems(downloadItems);

                try {
                    em.persist(download);
                    em.remove(cart);
                    em.flush();
                } catch (Exception e) {
                    logger.debug(e.getMessage());
                    throw new BadRequestException("Unable to submit for cart for download");
                }

            } else {
                throw new BadRequestException("Unable to submit for cart for download");
            }
           
        }

        return emptyCart(facilityName, userName);
    }

    private Response emptyCart(String facilityName, String userName){
        JsonObject emptyCart = Json.createObjectBuilder()
            .add("facilityName", facilityName)
            .add("userName", userName)
            .add("cartItems", Json.createArrayBuilder().build())
            .build();
        return Response.ok().entity(emptyCart.toString()).build();
    }

    private Flag getZipFlag(String zip) {
        if (zip == null ) {
            return Flag.ZIP;
        }
        zip = zip.toUpperCase();
        if(zip.equals("ZIP_AND_COMPRESS")) {
            return Flag.ZIP_AND_COMPRESS;
        } else {
            return Flag.ZIP;
        }
    }

    private DataSelection cartToDataSelection(Cart cart) {
        DataSelection dataSelection = new DataSelection();

        for (CartItem cartItem : cart.getCartItems()) {
            if (cartItem.getEntityType() == EntityType.investigation) {
                dataSelection.addInvestigation(cartItem.getEntityId());
            }

            if (cartItem.getEntityType() == EntityType.dataset) {
                dataSelection.addDataset(cartItem.getEntityId());
            }

            if (cartItem.getEntityType() == EntityType.datafile) {
                dataSelection.addDatafile(cartItem.getEntityId());
            }
        }

        return dataSelection;
    }
    

}