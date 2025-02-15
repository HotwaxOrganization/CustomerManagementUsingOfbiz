package com.companyname.ofbizdemo.events;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class CustomerEvents {

    public static final String MODULE = CustomerEvents.class.getName();

    public static String createCustomerEvent(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        String emailAddress = request.getParameter("emailAddress");
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");

        if (emailAddress == null || firstName == null || lastName == null) {
            request.setAttribute("_ERROR_MESSAGE_", "Missing required fields: emailAddress, firstName, or lastName.");
            return "error";
        }

        try {
            // Create input parameters for the service
            Map<String, Object> input = new HashMap<>();
            input.put("emailAddress", emailAddress);
            input.put("firstName", firstName);
            input.put("lastName", lastName);

            // Call createCustomer service
            Map<String, Object> result = dispatcher.runSync("createCustomer", input);

            if (ServiceUtil.isError(result)) {
                request.setAttribute("_ERROR_MESSAGE_", result.get("errorMessage"));
                return "error";
            }

            request.setAttribute("_EVENT_MESSAGE_", "Customer successfully created.");
            return "success";

        } catch (Exception e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", "Error creating customer: " + e.getMessage());
            return "error";
        }
    }



    public static String updateCustomerEvent(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        String emailAddress = request.getParameter("emailAddress");
        String contactNumber = request.getParameter("contactNumber");
        String postalAddress = request.getParameter("postalAddress");


        try {
            // Create input parameters for the service
            Map<String, Object> input = new HashMap<>();
            input.put("emailAddress", emailAddress);
            input.put("contactNumber", contactNumber);
            input.put("postalAddress", postalAddress);

            // Call updateCustomer service
            Map<String, Object> result = dispatcher.runSync("updateCustomer", input);

            if (ServiceUtil.isError(result)) {
                request.setAttribute("_ERROR_MESSAGE_", result.get("errorMessage"));
                return "error";
            }

            request.setAttribute("_EVENT_MESSAGE_", "Customer details successfully updated.");
            return "success";

        } catch (Exception e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", "Error updating customer details: " + e.getMessage());
            return "error";
        }
    }
}
