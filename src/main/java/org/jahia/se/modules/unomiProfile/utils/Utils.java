package org.jahia.se.modules.unomiProfile.utils;

import org.apache.unomi.api.PropertyType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jahia.modules.jexperience.admin.ContextServerService;

import java.io.IOException;
import java.util.Collection;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final String DEFAULT_CARD_NAME = "Main Data";

    public static PropertyType[] getProfileProperties(String siteKey, ContextServerService contextServerService) {
        try {
            return contextServerService.executeGetRequest(
                    siteKey,
                    "/cxs/profiles/properties/targets/profiles",
                    null,
                    null,
                    PropertyType[].class
            );
        } catch (IOException e) {
            logger.error("Failed to retrieve profile properties for site: {}", siteKey, e);
            return null;
        }
    }
    /**
     * Fetches a specific property from Unomi based on the itemId.
     *
     * @param siteKey             The site key for the context server.
     * @param contextServerService The ContextServerService to make the request.
     * @param itemId              The itemId of the property to fetch.
     * @return A JSONObject representing the fetched property, or null if an error occurs.
     */
    public static JSONObject getPropertyById(String siteKey, ContextServerService contextServerService, String itemId) {
        try {
            // Construct the endpoint URL
            String endpoint = "/cxs/profiles/properties/" + itemId;

            // Make the request
            logger.info("Fetching property with itemId '{}' from endpoint: {}", itemId, endpoint);
            PropertyType property = contextServerService.executeGetRequest(
                    siteKey,
                    endpoint,
                    null,
                    null,
                    PropertyType.class
            );

            // Convert the property to a JSON object
            if (property != null) {
                JSONObject propertyJson = new JSONObject();
                propertyJson.put("propertyId", property.getMetadata().getId());
                propertyJson.put("propertyName", property.getMetadata().getName());
                propertyJson.put("cardName", getCardName(property.getMetadata().getSystemTags()));
                propertyJson.put("type", property.getValueTypeId());
                logger.info("Successfully fetched property: {}", propertyJson.toString());
                return propertyJson;
            } else {
                logger.warn("Property with itemId '{}' was not found.", itemId);
            }
        } catch (IOException e) {
            logger.error("Error while fetching property with itemId '{}': {}", itemId, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching property with itemId '{}': {}", itemId, e.getMessage(), e);
        }

        return null;
    }

    public static String getCardName(Object systemTagsObj) {
        JSONArray systemTags = convertToJSONArray(systemTagsObj);

        for (int i = 0; i < systemTags.length(); i++) {
            String tag = systemTags.optString(i, "");
            if (tag.startsWith("cardDataTag")) {
                String[] parts = tag.split("/");
                if (parts.length > 3) {
                    return parts[parts.length - 1];
                }
            }
        }
        return DEFAULT_CARD_NAME;
    }

    private static JSONArray convertToJSONArray(Object systemTagsObj) {
        if (systemTagsObj instanceof JSONArray) {
            return (JSONArray) systemTagsObj;
        } else if (systemTagsObj instanceof Collection) {
            JSONArray jsonArray = new JSONArray();
            ((Collection<?>) systemTagsObj).forEach(jsonArray::put);
            return jsonArray;
        }
        return new JSONArray();
    }
}