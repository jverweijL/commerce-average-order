<%@ include file="/init.jsp" %>
<portlet:actionURL name="<%=MVCCommandNames.ADD_TO_CART%>" var="addToCartURL" />
<%
List<Long> products = (List<Long>)request.getAttribute("productsIntances");
%>
<h3>work in progress...</h3>
<div class="dataset-display-content-wrapper">
    <div class="table-style-stacked">
        <div class="table-responsive">
            <table class="table table-autofit">
                <thead>
                    <tr>
                        <th>

                        </th>
                        <th>
                            <p class="table-list-title">SKU</p>
                        </th>
                        <th>
                            <p class="table-list-title">Name</p>
                        </th>
                        <th>
                            <p class="table-list-title">Quantity</p>
                        </th>
                        <th>
                            <p class="table-list-title">In Stock</p>
                        </th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach var="product" items="${products}">
                    <tr>
                         <td class="">
                             <a href="${product.friendlyURL}"><img class="card-img-top img-fluid" src="${product.thumbnailSrc}"></a>
                         </td>
                        <td class=""><a href="${product.friendlyURL}">${product.sku}</a></td>
                        <td class=""><a href="${product.friendlyURL}">${product.name}</a></td>
                        <td class="">${product.quantity}</td>
                        <td class="">${product.quantityInStock}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>

<aui:form action="${addToCartURL}" method="post" name="add-to-cart" id="addToCart">
    <aui:input type="hidden" name="products" value="<%= StringUtil.merge(products) %>"/>
    <aui:button-row>
        <aui:button type="submit" name="submit-cart" value="Add to Cart" />
    </aui:button-row>
</aui:form>