package org.jahia.se.modules.unomiProfile.rules;


import org.jahia.services.content.rules.ModuleGlobalObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ModuleGlobalObject.class, immediate = true)
public class ProfileServiceModuleObject extends ModuleGlobalObject {
    @Reference(service = ProfileService.class)
    public void set_ProfileProperty(ProfileService profileService) {
        getGlobalRulesObject().put("profileService", profileService);
    }
}