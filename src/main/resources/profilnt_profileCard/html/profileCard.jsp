<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<jsp:useBean id="random" class="java.util.Random" scope="application"/>

<%--Add files used by the webapp--%>
<template:addResources type="css" resources="webapp/${requestScope.webappCssFileName}" media="screen"/>
<template:addResources type="css" resources="profile-overrides.css"/>
<template:addResources type="javascript" resources="webapp/${requestScope.webappJsFileName}"/>


<jcr:nodeProperty node="${currentNode}" name="unomiProperties" var="unomiProperties"/>
<jcr:nodeProperty node="${currentNode}" name="layout" var="layout"/>

<c:set var="_uuid_" value="${currentNode.identifier}"/>
<c:set var="language" value="${currentResource.locale.language}"/>
<c:set var="workspace" value="${renderContext.workspace}"/>
<c:set var="isEdit" value="${renderContext.editMode}"/>

<c:set var="site" value="${renderContext.site.siteKey}"/>
<c:set var="host" value="${url.server}"/>

<c:set var="targetId" value="REACT_UnomiProfile_${fn:replace(random.nextInt(),'-','_')}"/>
<jcr:node var="user" path="${renderContext.user.localPath}"/>


<div id="${targetId}"></div>

<script>
    // Convert unomiProperties to a JavaScript object
    const unomiprofile_context_${targetId} = {
        host: "${host}",
        workspace: "${workspace}",
        isEdit: ${isEdit},
        scope: "${site}", // Site key
        locale: "${language}",
        unomiProfileWidgetId: "${_uuid_}",
        layout: "${layout}",
        gqlServerUrl: "${host}/modules/graphql",
        contextServerUrl: window.digitalData ? window.digitalData.contextServerPublicUrl : undefined, // digitalData is set in live mode only

    };

    window.addEventListener("DOMContentLoaded", (event) => {
        const callUnomiProfileApp = () => {
            if (typeof window.UnomiProfileUIApp === 'function') {
                window.UnomiProfileUIApp("${targetId}", unomiprofile_context_${targetId});
            } else {
                console.error("Error: window.UnomiProfileUIApp is not defined or is not a function.");
            }
        };

        <c:choose>
            <c:when test="${isEdit}">
                setTimeout(callUnomiProfileApp, 500); // Delayed execution in edit mode
            </c:when>
            <c:otherwise>
                callUnomiProfileApp(); // Immediate execution in non-edit mode
            </c:otherwise>
        </c:choose>
    });
</script>
