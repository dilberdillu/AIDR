package qa.qcri.aidr.manager.controller;

import static qa.qcri.aidr.manager.util.CollectionStatus.RUNNING;
import static qa.qcri.aidr.manager.util.CollectionStatus.RUNNING_WARNING;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import qa.qcri.aidr.manager.dto.TaggerCrisis;
import qa.qcri.aidr.manager.dto.TaggerModel;
import qa.qcri.aidr.manager.persistence.entities.Collection;
import qa.qcri.aidr.manager.persistence.entities.UserAccount;
import qa.qcri.aidr.manager.service.CollectionLogService;
import qa.qcri.aidr.manager.service.CollectionService;
import qa.qcri.aidr.manager.service.TaggerService;
import qa.qcri.aidr.manager.service.UserService;
import qa.qcri.aidr.manager.util.CollectionType;


@Controller
public class ScreenController extends BaseController{

    @Autowired
    private CollectionService collectionService;
    @Autowired
    private UserService userService;
    @Autowired
    private TaggerService taggerService;
    @Value("${fetchMainUrl}")
    private String fetchMainUrl;
    @Autowired
    private CollectionLogService collectionLogService;

	private final Logger logger = Logger.getLogger(ScreenController.class);
    
	@RequestMapping("protected/home")
	public ModelAndView home() throws Exception {
        UserAccount authenticatedUser = getAuthenticatedUser();

        ModelAndView model = new ModelAndView("home");
        model.addObject("userName", authenticatedUser.getUserName());
        model.addObject("signInProvider", authenticatedUser.getProvider());
        model.addObject("collectionTypes", CollectionType.JSON());
        return model;
	}

	@RequestMapping("signin")
	public String signin(Map<String, String> model) throws Exception {
		return "signin";
	}

    @RequestMapping("protected/access-error")
    public ModelAndView accessError() throws Exception {
        return new ModelAndView("access-error");
    }

    private boolean isHasPermissionForCollection(String code) throws Exception{
        UserAccount user = getAuthenticatedUser();
        if (user == null){
            return false;
        }

//        current user is Admin
        if (userService.isUserAdmin(user)) {
            return true;
        }

        Collection collection = collectionService.findByCode(code);
        if (collection == null){
            return false;
        }

//        current user is a owner of the collection
        if(user.getUserName().equals(collection.getOwner().getUserName())){
            return true;
        }

//        current user is in managers list of the collection
        if (userService.isUserInCollectionManagersList(user, collection)){
            return true;
        }
        return false;
    }

    @RequestMapping("protected/{code}/collection-details")
    public ModelAndView collectionDetails(@PathVariable(value="code") String code) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        UserAccount authenticatedUser = getAuthenticatedUser();
        Collection collection = collectionService.findByCode(code);

        ModelAndView model = new ModelAndView("collection-details");
        model.addObject("id", collection.getId());
        model.addObject("collectionCode", code);
        model.addObject("userName", authenticatedUser.getUserName());
        model.addObject("signInProvider", authenticatedUser.getProvider());
        model.addObject("fetchMainUrl", fetchMainUrl);
        model.addObject("collectionType", collection.getProvider());
        model.addObject("collectionTypes", CollectionType.JSON());

        return model;
    }

    @RequestMapping("protected/collection-create")
    public ModelAndView collectionCreate() throws Exception {
    	
    	String signInProviderName = getAuthenticatedProviderName();
    	/*if(!signInProviderName.equalsIgnoreCase(SocialSignInProvider.TWITTER)){
    		logger.info("protected access-error");
    		return new ModelAndView("redirect:/protected/access-error");
    	}*/
    	
        ModelAndView model = new ModelAndView("collection-create");

        UserAccount authenticatedUser = getAuthenticatedUser();
        model.addObject("collectionTypes", CollectionType.JSON());
        model.addObject("userName", authenticatedUser.getUserName());
        model.addObject("signInProvider", authenticatedUser.getProvider());
        model.addObject("userId", getAuthenticatedUser().getId());

        return model;

    }
    
    @RequestMapping("protected/{code}/tagger-collection-details")
    public ModelAndView taggerCollectionDetails(@PathVariable(value="code") String code) throws Exception {
    	logger.info("Received request for crisis code = " + code);
    	if (!isHasPermissionForCollection(code)){
            logger.info("protected access-error");
    		return new ModelAndView("redirect:/protected/access-error");
        }

        //TaggerCrisis crisis = taggerService.getCrisesByCode(code);
        logger.info("returned from getCrisesByCode");
        Collection collection = collectionService.findByCode(code);
        logger.info("returned from findByCode");
        
        String signInProviderName = getAuthenticatedProviderName();
        Long crisisId = 0L;
        String crisisName = "";
        Long crisisTypeId = 0L;
        Boolean isMicromapperEnabled = false;
        if (collection != null && collection.getId() != null && collection.getName() != null){
            crisisId = collection.getId();
            crisisName = collection.getName();
            if (collection.getCrisisType() != null) {
                crisisTypeId = collection.getCrisisType().getId();
            }
            
        	isMicromapperEnabled = collection.isMicromappersEnabled();
        }
        logger.info("Fetched tagger crisis: " + collection.getCode() + ", aidr collection: " + collection.getCode());
        
        ModelAndView model = new ModelAndView("tagger/tagger-collection-details");
        model.addObject("crisisId", crisisId);
        model.addObject("name", crisisName);
        model.addObject("crisisTypeId", crisisTypeId);
        model.addObject("code", code);
        model.addObject("isMicromapperEnabled", isMicromapperEnabled);
        model.addObject("collectionType", collection.getProvider());
        model.addObject("collectionTypes", CollectionType.JSON());
        model.addObject("signInProvider", signInProviderName);
        logger.info("Returning model: " + model.getModel());
        return model;
    }

    @RequestMapping("protected/{code}/predict-new-attribute")
    public ModelAndView predictNewAttribute(@PathVariable(value="code") String code) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }
        
        TaggerCrisis crisis = taggerService.getCrisesByCode(code);

        Integer crisisId = 0;
        String crisisName = "";
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
        }
        String signInProviderName = getAuthenticatedProviderName();
        
        ModelAndView model = new ModelAndView("tagger/predict-new-attribute");
        model.addObject("crisisId", crisisId);
        model.addObject("name", crisisName);
        model.addObject("code", code);
        model.addObject("signInProvider", signInProviderName);
        return model;
    }

    @RequestMapping("protected/{id}/attribute-details")
    public ModelAndView attributeDetails(@PathVariable(value="id") Integer id) throws Exception {
        ModelAndView model = new ModelAndView("tagger/attribute-details");
        UserAccount authenticatedUser = getAuthenticatedUser();
        model.addObject("id", id);
        model.addObject("userId", authenticatedUser.getId());
        model.addObject("signInProvider", authenticatedUser.getProvider());
        return model;
    }

    @RequestMapping("protected/{code}/{id}/model-details")
    public ModelAndView modelDetails(@PathVariable(value="code") String code, @PathVariable(value="id") Integer modelId) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);

        Integer crisisId = 0;
        Integer modelFamilyId = 0;
        Integer attributeId = 0;
        String crisisName = "";
        String modelName = "";
        double modelAuc = 0;
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
        }

        List<TaggerModel> modelsForCrisis = taggerService.getModelsForCrisis(crisisId);
        for (TaggerModel model : modelsForCrisis) {
            if (modelId.equals(model.getModelID())){
                modelName = model.getAttribute();
                if (model.getModelFamilyID() != null) {
                    modelFamilyId = model.getModelFamilyID();
                }
                modelAuc = model.getAuc();
                attributeId = model.getAttributeID();
            }
        }

        UserAccount authenticatedUser = getAuthenticatedUser();

        Collection collection = collectionService.findByCode(code);
        String signInProviderName = getAuthenticatedProviderName();
        
        ModelAndView model = new ModelAndView("tagger/model-details");
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("modelName", modelName);
        model.addObject("modelId", modelId);
        model.addObject("modelAuc", modelAuc);
        model.addObject("modelFamilyId", modelFamilyId);
        model.addObject("code", code);
        model.addObject("userId", authenticatedUser.getId());
        model.addObject("signInProvider", authenticatedUser.getProvider());
        model.addObject("attributeId", attributeId);
        model.addObject("collectionType", collection.getProvider());
        model.addObject("collectionTypes", CollectionType.JSON());
        model.addObject("signInProvider", signInProviderName);
        return model;
    }

    @RequestMapping("protected/{code}/new-custom-attribute")
    public ModelAndView newCustomAttribute(@PathVariable(value="code") String code) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        Collection collection = collectionService.findByCode(code);
        if(collection.getProvider() == CollectionType.Facebook) {
        	return new ModelAndView("redirect:/protected/access-error");
        }
        
        TaggerCrisis crisis = taggerService.getCrisesByCode(code);
        Integer crisisId = 0;
        String crisisName = "";

        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
        }

        //Collection collection = collectionService.findByCode(code);
        String signInProviderName = getAuthenticatedProviderName();
        
        ModelAndView model = new ModelAndView("tagger/new-custom-attribute");
        model.addObject("code", code);
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("collectionType", collection.getProvider());
        model.addObject("collectionTypes", CollectionType.JSON());
        model.addObject("signInProvider", signInProviderName);
        
        return model;
    }

    @RequestMapping("protected/{code}/{modelId}/{modelFamilyId}/{attributeID}/training-data")
    public ModelAndView trainingData(@PathVariable(value="code") String code,
                                     @PathVariable(value="modelId") Integer modelId,
                                     @PathVariable(value="modelFamilyId") Integer modelFamilyId,
                                     @PathVariable(value="attributeID") Integer attributeID) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);

        Integer crisisId = 0;
        String crisisName = "";
        String modelName = "";
        double modelAuc = 0;
        long trainingExamples = 0;
        Integer retrainingThreshold = 50;
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();

        }

        List<TaggerModel> modelsForCrisis = taggerService.getModelsForCrisis(crisisId);
        for (TaggerModel model : modelsForCrisis) {
            if (attributeID.equals(model.getAttributeID())){
                modelName = model.getAttribute();
                trainingExamples = model.getTrainingExamples();
                modelAuc = model.getAuc();
                retrainingThreshold = model.getRetrainingThreshold();
            }
        }

        Collection collection = collectionService.findByCode(code);
        String signInProviderName = getAuthenticatedProviderName();
        
        ModelAndView model = new ModelAndView("tagger/training-data");
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("modelName", modelName);
        model.addObject("modelId", modelId);
        model.addObject("modelFamilyId", modelFamilyId);
        model.addObject("attributeID", attributeID);
        model.addObject("code", code);
        model.addObject("trainingExamples", trainingExamples);
        model.addObject("modelAuc", modelAuc);
        model.addObject("retrainingThreshold", retrainingThreshold);
        model.addObject("collectionType", collection.getProvider());
        model.addObject("collectionTypes", CollectionType.JSON());
        model.addObject("signInProvider", signInProviderName);
        model.addObject("imageTagCount",taggerService.getTaggedImageCount(crisisId));
        return model;
    }

    @RequestMapping("protected/{code}/{modelId}/{modelFamilyId}/{nominalAttributeId}/training-examples")
    public ModelAndView trainingExamples(@PathVariable(value="code") String code,
                                         @PathVariable(value="modelId") Integer modelId,
                                         @PathVariable(value="modelFamilyId") Integer modelFamilyId,
                                         @PathVariable(value="nominalAttributeId") Integer nominalAttributeId) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);

        Integer crisisId = 0;
        String crisisName = "";
        String modelName = "";
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
        }

        List<TaggerModel> modelsForCrisis = taggerService.getModelsForCrisis(crisisId);
        for (TaggerModel model : modelsForCrisis) {
            if (nominalAttributeId.equals(model.getAttributeID())){
                modelName = model.getAttribute();
            }
        }


        Collection collection = collectionService.findByCode(code);
        String signInProviderName = getAuthenticatedProviderName();
        
        ModelAndView model = new ModelAndView("tagger/training-examples");
        model.addObject("code", code);
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("modelName", modelName);
        model.addObject("modelId", modelId);
        model.addObject("modelFamilyId", modelFamilyId);
        model.addObject("nominalAttributeId", nominalAttributeId);
        model.addObject("collectionType", collection.getProvider());
        model.addObject("collectionTypes", CollectionType.JSON());
        model.addObject("signInProvider", signInProviderName);
        return model;
    }

    @RequestMapping("protected/{code}/image-training-examples")
    public ModelAndView imageTrainingExamples(@PathVariable(value="code") String code) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);

        Integer crisisId = 0;
        String crisisName = "";
        String modelName = "";
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
        }


        Collection collection = collectionService.findByCode(code);
        String signInProviderName = getAuthenticatedProviderName();
        
        ModelAndView model = new ModelAndView("tagger/image-training-examples");
        model.addObject("code", code);
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("modelName", modelName);
        model.addObject("modelId", 0);
        model.addObject("modelFamilyId", 0);
        
        model.addObject("collectionType", collection.getProvider());
        model.addObject("collectionTypes", CollectionType.JSON());
        model.addObject("signInProvider", signInProviderName);
        return model;
    }
    @RequestMapping("protected/administration/admin-console")
    public ModelAndView adminConsole(Map<String, String> model) throws Exception {
        UserAccount user = getAuthenticatedUser();
        if (!userService.isUserAdmin(user)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        return new ModelAndView( "administration/admin-console");
    }

    @RequestMapping("protected/administration/admin-health")
    public ModelAndView adminHealth(Map<String, String> model) throws Exception {
        UserAccount user = getAuthenticatedUser();
        if (!userService.isUserAdmin(user)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        return new ModelAndView("administration/health");
    }

    @RequestMapping("protected/{code}/image-training-data")
    public ModelAndView imageTrainingData(@PathVariable("code") String code) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }
        
        Collection collection = collectionService.findByCode(code);
        String signInProviderName = getAuthenticatedProviderName();
        
        ModelAndView model = new ModelAndView("tagger/image-training-data");
        model.addObject("crisisId", collection.getId());
        model.addObject("crisisName", collection.getName());
        model.addObject("code", code);
        model.addObject("collectionType", collection.getProvider());
        model.addObject("collectionTypes", CollectionType.JSON());
        model.addObject("signInProvider", signInProviderName);
        

        return model;
    }

    @RequestMapping("protected/{code}/interactive-view-download")
         public ModelAndView interactiveViewDownload(@PathVariable(value="code") String code) throws Exception {

        UserAccount user =null;
       // System.out.println("interactiveViewDownload : ");

        if (isHasPermissionForCollection(code)){
            user = getAuthenticatedUser();
        }

        return getInteractiveViewDownload(code,user.getUserName(), user.getProvider());
    }

    @RequestMapping("public/{code}/interactive-view-download")
    public ModelAndView publicInteractiveViewDownload(@PathVariable(value="code") String code) throws Exception {
        return getInteractiveViewDownload(code, "", "");
    }

    @RequestMapping("public/{code}/{username}/interactive-view-download")
    public ModelAndView privateInteractiveViewDownload(@PathVariable(value="code") String code,
                                                       @PathVariable(value="username") String username) throws Exception {

    	UserAccount user = userService.fetchByUserName(username);
        return getInteractiveViewDownload(code, username, user.getProvider());
    }

    private ModelAndView getInteractiveViewDownload(String code, String userName, String signInProvider){

        TaggerCrisis crisis = null;
        Collection collection = null;
        try {
            crisis = taggerService.getCrisesByCode(code);
            collection = collectionService.findByCode(code);
        } catch (Exception e) {
        	logger.error("Exception while getting interactive view download", e);
        }

        Integer crisisId = 0;
        String crisisName = "";
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            //crisisName = crisis.getName();
            crisisName = collection.getName();
        }

        Integer collectionCount = 0;
        Long collectionId = 0L;
        CollectionType type = CollectionType.Twitter;
        if (collection != null){
            if (collection.getId() != null) {
                collectionId = collection.getId();
                try {
                    collectionCount = collectionLogService.countTotalDownloadedItemsForCollection(collectionId);
                } catch (Exception e) {
                	logger.error("Exception while counting total download items for collectionID: "+collectionId, e);
                }
            }
            if (collection.getCount() != null && (collection.getStatus() != null || RUNNING == collection.getStatus() || RUNNING_WARNING == collection.getStatus())) {
                collectionCount += collection.getCount();
            }
            if (collection.getProvider() != null) {
                type = collection.getProvider();
            }
        }

        ModelAndView model = new ModelAndView("../public/interactive-view-download");
        model.addObject("collectionId", collectionId);
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("code", code);
        model.addObject("count", collectionCount);
        model.addObject("userName", userName);
        model.addObject("signInProvider", signInProvider);
        model.addObject("collectionType", type);
        model.addObject("collectionTypes", CollectionType.JSON());

        return model;
    }    

}
