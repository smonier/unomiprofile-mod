package org.jahia.se.modules.unomiProfile.initializers;

import org.apache.unomi.api.PropertyType;
import org.apache.jackrabbit.value.StringValue;
import org.jahia.modules.jexperience.admin.ContextServerService;
import org.jahia.se.modules.unomiProfile.utils.Utils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = ModuleChoiceListInitializer.class, immediate = true)
public class PropertiesInitializer implements ModuleChoiceListInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesInitializer.class);

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

                PropertyType[] profileProperties = Utils.getProfileProperties(site.getSiteKey(), contextServerService);
                if (profileProperties != null && profileProperties.length > 0) {
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
        Object systemTagsObj = property.getMetadata().getSystemTags();

        String cardName = Utils.getCardName(systemTagsObj);

        String displayName = String.format("%s - %s", cardName, propertyName);
        return new ChoiceListValue(displayName, null, new StringValue(propertyId));
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