<%@ include file="/init.jsp" %>
<portlet:actionURL var="userCSVDataUploadURL" name="<%=MVCCommandNames.UPLOAD_CSV%>"></portlet:actionURL>

<p>
	<b><liferay-ui:message key="commerceaverageorder.caption"/></b>
</p>

<aui:form action="${userCSVDataUploadURL}" method="post" id="csvDataFileForm">
    <aui:button-row>
        <aui:button type="submit" name="submit-cart" value="Suggest Next Best Order" />
    </aui:button-row>
</aui:form>