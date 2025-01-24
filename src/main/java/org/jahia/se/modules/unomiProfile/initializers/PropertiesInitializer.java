package org.jahia.se.modules.unomiProfile.initializers;

import org.apache.unomi.api.PropertyType;
import org.apache.jackrabbit.value.StringValue;
import org.jahia.modules.jexperience.admin.ContextServerService;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.json.JSONArray;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h1>PropertiesInitializer</h1>
 * This class populates a choice list with profile properties retrieved from Unomi.
 * It supports filtering by single or multiple text properties based on a parameter.
 */
@Component(service = ModuleChoiceListInitializer.class, immediate = true)
public class PropertiesInitializer implements ModuleChoiceListInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesInitializer.class);

    private static final String DEFAULT_CARD_NAME = "Main Data";

    private ContextServerService contextServerService;
    private String key = "ProfilePropertiesInitializer";

    @Reference(service = ContextServerService.class)
    public void setContextServerService(ContextServerService contextServerService) {
        this.contextServerService = contextServerService;
    }

    @Override
    public List<ChoiceListValue> getChoiceListValues(
            ExtendedPropertyDefinition epd,
            String param,
            List<ChoiceListValue> values,
            Locale locale,
            Map<String, Object> context) {

        List<ChoiceListValue> choiceListValues = new ArrayList<>();
        logger.info("Initializing properties for parameter: {}", param);

        try {
            JCRNodeWrapper node = getNodeFromContext(context);
            JCRSiteNode site = node.getResolveSite();

            if (site == null) {
                throw new IllegalArgumentException("Site cannot be null");
            }

            if (contextServerService != null && contextServerService.isAvailable(site.getSiteKey())) {
                logger.info("ContextServerService is available for site: {}", site.getSiteKey());

                PropertyType[] profileProperties = getProfileProperties(site.getSiteKey());
                if (profileProperties != null && profileProperties.length > 0) {
                    // Build and sort choice list
                    choiceListValues = buildChoiceList(profileProperties);
                } else {
                    logger.warn("No profile properties retrieved for site: {}", site.getSiteKey());
                }
            } else {
                logger.error("ContextServerService is not available for site: {}", site.getSiteKey());
            }
        } catch (Exception e) {
            logger.error("Error while initializing choice list values: ", e);
        }

        return choiceListValues;
    }

    private JCRNodeWrapper getNodeFromContext(Map<String, Object> context) throws RepositoryException {
        Object parentNode = context.get("contextParent");
        Object currentNode = context.get("contextNode");
        return (JCRNodeWrapper) Optional.ofNullable(parentNode != null ? parentNode : currentNode)
                .orElseThrow(() -> new IllegalArgumentException("Context does not contain a valid node"));
    }

    private PropertyType[] getProfileProperties(String siteKey) {
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

    private List<ChoiceListValue> buildChoiceList(PropertyType[] profileProperties) {
        return Arrays.stream(profileProperties)
                .map(this::buildChoiceListValue)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChoiceListValue::getDisplayName))
                .collect(Collectors.toList());
    }

    private ChoiceListValue buildChoiceListValue(PropertyType property) {
        if (property.getMetadata() == null) {
            return null;
        }

        String propertyId = property.getMetadata().getId();
        String propertyName = property.getMetadata().getName();

        // Fix for systemTags handling
        Object systemTagsObj = property.getMetadata().getSystemTags();
        JSONArray systemTags = convertToJSONArray(systemTagsObj);

        String cardName = getCardName(systemTags);

        String displayName = String.format("%s - %s", cardName, propertyName);
        return new ChoiceListValue(displayName, null, new StringValue(propertyId));
    }

    private String getCardName(JSONArray systemTags) {
        if (systemTags == null) {
            return DEFAULT_CARD_NAME;
        }

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

    private JSONArray convertToJSONArray(Object systemTagsObj) {
        if (systemTagsObj instanceof JSONArray) {
            return (JSONArray) systemTagsObj;
        } else if (systemTagsObj instanceof Collection) {
            // Convert Collection (e.g., HashSet) to JSONArray
            JSONArray jsonArray = new JSONArray();
            ((Collection<?>) systemTagsObj).forEach(jsonArray::put);
            return jsonArray;
        } else {
            // Default to an empty JSONArray if the type is unsupported
            return new JSONArray();
        }
    }

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}