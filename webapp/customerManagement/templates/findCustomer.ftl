<h1>Find Customer</h1>

<!-- Customer Search Form -->
<form method="get" action="<@ofbizUrl>findCustomer</@ofbizUrl>">
    <label>Party ID:</label>
    <input type="text" name="partyId" ><br>

    <label>Customer Name:</label>
    <input type="text" name="customerName" ><br>

    <label>Email Address:</label>
    <input type="text" name="emailAddress" ><br>

    <label>Phone Number:</label>
    <input type="text" name="contactNumber" ><br>

    <label>Address:</label>
    <input type="text" name="postalAddress" ><br>

    <button type="submit">Search</button>
</form>

<!-- Display Results -->
<#if requestAttributes.customerList??>
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


<#else>
    <p>No customers found.</p>
</#if>
