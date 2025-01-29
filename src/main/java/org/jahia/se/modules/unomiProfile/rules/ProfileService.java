package org.jahia.se.modules.unomiProfile.rules;

import org.jahia.se.modules.unomiProfile.utils.Utils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jahia.modules.jexperience.admin.ContextServerService;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component(service = ProfileService.class, immediate = true)
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private String key="profileService";
    private ContextServerService contextServerService;

    @Reference(service = ContextServerService.class)
    public void setContextServerService(ContextServerService contextServerService) {
        this.contextServerService = contextServerService;
    }

    /**
     * Formats and saves the `unomiProperties` for the given node.
     *
     * @param node The JCR node where `unomiProperties` has changed.
     */
    public void formatAndSaveProperties(JCRNodeWrapper node) {
        try {
            logger.info("Starting formatAndSaveProperties for node: {}", node.getPath());

            // Retrieve the site key from the node
            String siteKey = node.getResolveSite().getSiteKey();
            logger.info("Retrieved siteKey: {}", siteKey);

            // Retrieve the unomiProperties value as a list of property IDs
            JCRValueWrapper[] propertyValues = node.getProperty("unomiProperties").getValues();
            List<String> propertiesList = Arrays.stream(propertyValues)
                    .map(value -> {
                        try {
                            return value.getString();
                        } catch (Exception e) {
                            logger.error("Error retrieving string value from property: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            logger.info("Retrieved unomiProperties values: {}", propertiesList);

            // Retrieve the session from the node
            JCRSessionWrapper session = node.getSession();
            logger.info("Session retrieved for node: {}", node.getPath());

            // Fetch and format the properties
            logger.info("Fetching and formatting properties using Utils.getPropertyById...");
            JSONArray formattedProperties = formatProperties(siteKey, propertiesList);

            // Save the formatted properties to the node
            logger.info("Saving formatted properties to node...");
            node.setProperty("formatedProperties", formattedProperties.toString());
            session.save();
            logger.info("Formatted properties saved successfully for node: {}", node.getPath());

        } catch (Exception e) {
            logger.error("Error while formatting and saving unomiProperties for node: {}", node.getPath(), e);
        }
    }

    /**
     * Fetches and formats the properties into a JSON array containing propertyId, propertyName, cardName, and type.
     *
     * @param siteKey       The site key for the context server.
     * @param propertiesList The list of property IDs to fetch and format.
     * @return A JSON array containing the formatted property data.
     */
    private JSONArray formatProperties(String siteKey, List<String> propertiesList) throws JSONException {
        JSONArray resultArray = new JSONArray();

        logger.info("Starting to fetch and format properties for siteKey: {}", siteKey);

        for (String propertyId : propertiesList) {
            try {
                logger.info("Fetching property with ID: {}", propertyId);
                JSONObject propertyJson = Utils.getPropertyById(siteKey, contextServerService, propertyId);

                if (propertyJson != null) {
                    logger.info("Adding property to formatted list: {}", propertyJson);
                    resultArray.put(propertyJson);
                } else {
                    logger.warn("Property with ID '{}' not found. Skipping.", propertyId);
                }
            } catch (Exception e) {
                logger.error("Error fetching or formatting property with ID '{}': {}", propertyId, e.getMessage(), e);
            }
        }

        logger.info("Finished fetching and formatting properties. Total formatted properties: {}", resultArray.length());
        return resultArray;
    }
}