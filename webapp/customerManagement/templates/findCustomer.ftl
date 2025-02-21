<h1>Find Customer</h1>

<!-- Customer Search Form -->
<form method="get" action="<@ofbizUrl>findCustomer</@ofbizUrl>">
    <label>Party ID:</label>
    <input type="text" name="partyId" value="${requestParameters.partyId!}"><br>

    <label>Customer Name:</label>
    <input type="text" name="customerName" value="${requestParameters.customerName!}"><br>

    <label>Email Address:</label>
    <input type="text" name="emailAddress" value="${requestParameters.emailAddress!}"><br>

    <label>Phone Number:</label>
    <input type="text" name="contactNumber" value="${requestParameters.contactNumber!}"><br>

    <label>Address:</label>
    <input type="text" name="postalAddress" value="${requestParameters.postalAddress!}"><br>

    <button type="submit">Search</button>
</form>

<!-- Display Results -->
<#if requestAttributes.customerList?? && requestAttributes.customerList?size gt 0>
    <h2>Search Results</h2>
    <table border="1">
        <tr>
            <th>Party ID</th>
            <th>Customer Name</th>
            <th>Email Address</th>
            <th>Phone Number</th>
            <th>Address</th>
        </tr>
        <#list requestAttributes.customerList as customer>
            <tr>
                <td>${customer.partyId!}</td>
                <td>${customer.firstName!} ${customer.lastName!}</td>
                <td>${customer.emailAddress! "N/A"}</td>

                <td>
                    <#if customer?keys?seq_contains("contactNumber")>
                        ${customer.contactNumber}
                    <#else>
                        N/A
                    </#if>
                </td>

                <td>
                    <#if customer?keys?seq_contains("postalAddress")>
                        ${customer.postalAddress}
                    <#else>
                        N/A
                    </#if>
                </td>
            </tr>
        </#list>
    </table>

    <!-- Pagination Controls -->
    <#if requestAttributes.totalPages?? && requestAttributes.totalPages gt 1>
        <div>
            <#if requestAttributes.currentPage?? && requestAttributes.currentPage gt 1>
                <a href="<@ofbizUrl>findCustomer?currentPage=${requestAttributes.currentPage - 1}&partyId=${requestParameters.partyId!}&customerName=${requestParameters.customerName!}&emailAddress=${requestParameters.emailAddress!}&contactNumber=${requestParameters.contactNumber!}&postalAddress=${requestParameters.postalAddress!}</@ofbizUrl>">Previous</a>
            </#if>

            Page ${requestAttributes.currentPage} of ${requestAttributes.totalPages}

            <#if requestAttributes.currentPage?? && requestAttributes.currentPage lt requestAttributes.totalPages>
                <a href="<@ofbizUrl>findCustomer?currentPage=${requestAttributes.currentPage + 1}&partyId=${requestParameters.partyId!}&customerName=${requestParameters.customerName!}&emailAddress=${requestParameters.emailAddress!}&contactNumber=${requestParameters.contactNumber!}&postalAddress=${requestParameters.postalAddress!}</@ofbizUrl>">Next</a>
            </#if>
        </div>
    </#if>
<#else>
    <p>No customers found.</p>
</#if>
