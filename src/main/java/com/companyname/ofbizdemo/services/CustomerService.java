package com.companyname.ofbizdemo.services;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.DispatchContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;
import java.util.ArrayList;



public class CustomerService {

    public static final String MODULE = CustomerService.class.getName();

    public static Map<String, Object> findCustomer(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();  // Initialize result
        List<EntityCondition> entityConditions = new ArrayList<>();

        String emailAddress = (String) context.get("emailAddress");
        String firstName = (String) context.get("firstName");
        String lastName = (String) context.get("lastName");
        String contactNumber = (String) context.get("contactNumber");
        String postalAddress = (String) context.get("postalAddress");

        try {
            // Build the EntityCondition list
            if (emailAddress != null && !emailAddress.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("emailAddress", EntityOperator.LIKE, "%" + emailAddress.toLowerCase() + "%"));
            }
            if (firstName != null && !firstName.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("firstName", EntityOperator.LIKE, "%" + firstName.toLowerCase() + "%"));
            }
            if (lastName != null && !lastName.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("lastName", EntityOperator.LIKE, "%" + lastName.toLowerCase() + "%"));
            }
            if (contactNumber != null && !contactNumber.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("contactNumber", EntityOperator.LIKE, "%" + contactNumber.toLowerCase() + "%"));
            }
            if (postalAddress != null && !postalAddress.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("postalAddress", EntityOperator.LIKE, "%" + postalAddress.toLowerCase() + "%"));
            }

            // Combine conditions (if any) with an OR operator
            EntityCondition combinedCondition = null;
            if (!entityConditions.isEmpty()) {
                combinedCondition = EntityCondition.makeCondition(entityConditions, EntityOperator.OR);
            }

            // Build and execute the query
            EntityQuery query = EntityQuery.use(delegator).from("FindCustomerView");
            if (combinedCondition != null) {
                query.where(combinedCondition);
            }

            List<GenericValue> customerList = query.queryList();
            result.put("customerList", customerList);

        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("Error finding customers: " + e.getMessage());
        }

        return result;  // Return success with the customer list
    }







    public static Map<String, Object> createCustomer(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String emailAddress = (String) context.get("emailAddress");
        String firstName = (String) context.get("firstName");
        String lastName = (String) context.get("lastName");

        // Validate required fields
        if (firstName == null || lastName == null) {
            return ServiceUtil.returnError("Missing required fields: firstName or lastName");
        }

        // Check if the customer already exists
        try {
            List<GenericValue> existingCustomers = delegator.findByAnd("FindCustomerView",
                    UtilMisc.toMap("emailAddress", emailAddress), null, false); // Updated line

            if (existingCustomers != null && !existingCustomers.isEmpty()) {
                return ServiceUtil.returnError("Customer already exists with email: " + emailAddress);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("Error checking existing customer: " + e.getMessage());
        }

        try {
            String partyId = delegator.getNextSeqId("Party");

            // Create Party
            GenericValue newParty = delegator.makeValue("Party");
            newParty.set("partyId", partyId);
            newParty.set("partyTypeId", "PERSON");
            newParty.set("statusId", "PARTY_ENABLED");
            newParty.set("createdDate", UtilDateTime.nowTimestamp());
            delegator.create(newParty);

            // Create Person entry
            GenericValue newPerson = delegator.makeValue("Person");
            newPerson.set("partyId", partyId);
            newPerson.set("firstName", firstName);
            newPerson.set("lastName", lastName);
            delegator.create(newPerson);

            // Create Customer Role
            GenericValue partyRole = delegator.makeValue("PartyRole");
            partyRole.set("partyId", partyId);
            partyRole.set("roleTypeId", "CUSTOMER");
            delegator.create(partyRole);

            // Create Email ContactMech if provided
            if (emailAddress != null && !emailAddress.isEmpty()) {
                String contactMechId = delegator.getNextSeqId("ContactMech");

                GenericValue newContactMech = delegator.makeValue("ContactMech");
                newContactMech.set("contactMechId", contactMechId);
                newContactMech.set("contactMechTypeId", "EMAIL_ADDRESS");
                newContactMech.set("infoString", emailAddress);
                delegator.create(newContactMech);

                // Link ContactMech to Party
                GenericValue newPartyContactMech = delegator.makeValue("PartyContactMech");
                newPartyContactMech.set("partyId", partyId);
                newPartyContactMech.set("contactMechId", contactMechId);
                newPartyContactMech.set("fromDate", UtilDateTime.nowTimestamp());
                delegator.create(newPartyContactMech);

                // Assign ContactMech Purpose
                GenericValue partyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose");
                partyContactMechPurpose.set("partyId", partyId);
                partyContactMechPurpose.set("contactMechId", contactMechId);
                partyContactMechPurpose.set("contactMechPurposeTypeId", "PRIMARY_EMAIL");
                partyContactMechPurpose.set("fromDate", UtilDateTime.nowTimestamp());
                delegator.create(partyContactMechPurpose);
            }

            result.put("partyId", partyId);
            result.put("message", "Customer created successfully");


        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("Error creating customer: " + e.getMessage());
        }

        return result;
    }







    public static Map<String, Object> updateCustomer(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String emailAddress = (String) context.get("emailAddress");
        String postalAddress = (String) context.get("postalAddress");
        String contactNumber = (String) context.get("contactNumber");

        if (emailAddress == null || emailAddress.isEmpty()) {
            return ServiceUtil.returnError("Missing required field: emailAddress");
        }

        try {
            // Find the customer using emailAddress
            GenericValue customer = EntityQuery.use(delegator)
                    .from("FindCustomerView")
                    .where("emailAddress", emailAddress)
                    .queryFirst();

            if (customer == null) {
                return ServiceUtil.returnError("No customer found with email: " + emailAddress);
            }

            String partyId = customer.getString("partyId");

            //  Always Create a New Postal Address If Provided
            if (postalAddress != null && !postalAddress.isEmpty()) {
                String contactMechId = delegator.getNextSeqId("ContactMech");

                GenericValue newContactMech = delegator.makeValue("ContactMech");
                newContactMech.set("contactMechId", contactMechId);
                newContactMech.set("contactMechTypeId", "POSTAL_ADDRESS");
                delegator.create(newContactMech);

                GenericValue newPostalAddress = delegator.makeValue("PostalAddress");
                newPostalAddress.set("contactMechId", contactMechId);
                newPostalAddress.set("address1", postalAddress);
                delegator.create(newPostalAddress);

                GenericValue newPartyContactMech = delegator.makeValue("PartyContactMech");
                newPartyContactMech.set("partyId", partyId);
                newPartyContactMech.set("contactMechId", contactMechId);
                newPartyContactMech.set("fromDate", UtilDateTime.nowTimestamp());
                delegator.create(newPartyContactMech);
            }

            //  Always Create a New Phone Number If Provided
            if (contactNumber != null && !contactNumber.isEmpty()) {
                String contactMechId = delegator.getNextSeqId("ContactMech");

                GenericValue newContactMech = delegator.makeValue("ContactMech");
                newContactMech.set("contactMechId", contactMechId);
                newContactMech.set("contactMechTypeId", "TELECOM_NUMBER");
                delegator.create(newContactMech);

                GenericValue newTelecomNumber = delegator.makeValue("TelecomNumber");
                newTelecomNumber.set("contactMechId", contactMechId);
                newTelecomNumber.set("contactNumber", contactNumber);
                delegator.create(newTelecomNumber);

                GenericValue newPartyContactMech = delegator.makeValue("PartyContactMech");
                newPartyContactMech.set("partyId", partyId);
                newPartyContactMech.set("contactMechId", contactMechId);
                newPartyContactMech.set("fromDate", UtilDateTime.nowTimestamp());
                delegator.create(newPartyContactMech);
            }

            result.put("partyId", partyId);
            result.put("message", "Customer details updated successfully");

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("Error updating customer: " + e.getMessage());
        }

        return result;
    }



//    public static Map<String, Object> createCustomerRelationship(DispatchContext dctx, Map<String, ? extends Object> context) {
//        Delegator delegator = dctx.getDelegator();
//        Map<String, Object> result = ServiceUtil.returnSuccess();
//
//        String partyIdFrom = (String) context.get("partyIdFrom");
//        String partyIdTo = (String) context.get("partyIdTo");
//        String partyRelationshipTypeId = (String) context.get("partyRelationshipTypeId");
//        Timestamp fromDate = (Timestamp) context.get("fromDate");
//
//        if (partyIdFrom == null || partyIdTo == null || partyRelationshipTypeId == null) {
//            return ServiceUtil.returnError("Missing required fields: partyIdFrom, partyIdTo, or partyRelationshipTypeId.");
//        }
//
//        if (fromDate == null) {
//            fromDate = UtilDateTime.nowTimestamp();
//        }
//
//        try {
//            // Validate that both parties exist
//            GenericValue partyFrom = EntityQuery.use(delegator)
//                    .from("Party")
//                    .where("partyId", partyIdFrom)
//                    .queryOne();
//
//            GenericValue partyTo = EntityQuery.use(delegator)
//                    .from("Party")
//                    .where("partyId", partyIdTo)
//                    .queryOne();
//
//            if (partyFrom == null || partyTo == null) {
//                return ServiceUtil.returnError("One or both party IDs do not exist.");
//            }
//
//            // Create a new PartyRelationship entry
//            String relationshipId = delegator.getNextSeqId("PartyRelationship");
//            GenericValue newRelationship = delegator.makeValue("PartyRelationship");
//            newRelationship.set("partyIdFrom", partyIdFrom);
//            newRelationship.set("partyIdTo", partyIdTo);
////            newRelationship.set("roleTypeIdFrom", "CUSTOMER");  // Can be dynamic based on use case
////            newRelationship.set("roleTypeIdTo", "SUPPLIER");    // Example: Customer-Supplier relationship
//            newRelationship.set("partyRelationshipTypeId", partyRelationshipTypeId);
//            newRelationship.set("fromDate", fromDate);
//            delegator.create(newRelationship);
//
//            result.put("relationshipId", relationshipId);
//            result.put("message", "Customer relationship created successfully.");
//
//        } catch (GenericEntityException e) {
//            Debug.logError(e, MODULE);
//            return ServiceUtil.returnError("Error creating customer relationship: " + e.getMessage());
//        }
//
//        return result;
//    }


public static Map<String, Object> createCustomerRelationship(DispatchContext dctx, Map<String, ? extends Object> context) {
    Delegator delegator = dctx.getDelegator();
    Map<String, Object> result = ServiceUtil.returnSuccess();

    String partyIdFrom = (String) context.get("partyIdFrom");
    String partyIdTo = (String) context.get("partyIdTo");
    String partyRelationshipTypeId = (String) context.get("partyRelationshipTypeId");
    Timestamp fromDate = (Timestamp) context.get("fromDate");

    if (partyIdFrom == null || partyIdTo == null || partyRelationshipTypeId == null) {
        return ServiceUtil.returnError("Missing required fields: partyIdFrom, partyIdTo, or partyRelationshipTypeId.");
    }

    if (fromDate == null) {
        fromDate = UtilDateTime.nowTimestamp();
    }

    try {
        //  Validate that both parties exist
        GenericValue partyFrom = EntityQuery.use(delegator)
                .from("Party")
                .where("partyId", partyIdFrom)
                .queryOne();

        GenericValue partyTo = EntityQuery.use(delegator)
                .from("Party")
                .where("partyId", partyIdTo)
                .queryOne();

        if (partyFrom == null || partyTo == null) {
            return ServiceUtil.returnError("One or both party IDs do not exist.");
        }

        //  Fetch roleTypeIdFrom from PartyRole
        GenericValue roleFrom = EntityQuery.use(delegator)
                .from("PartyRole")
                .where("partyId", partyIdFrom)
                .queryFirst();

        if (roleFrom == null) {
            return ServiceUtil.returnError("No role found for partyIdFrom: " + partyIdFrom);
        }

        String roleTypeIdFrom = roleFrom.getString("roleTypeId");

        //  Fetch roleTypeIdTo from PartyRole
        GenericValue roleTo = EntityQuery.use(delegator)
                .from("PartyRole")
                .where("partyId", partyIdTo)
                .queryFirst();

        if (roleTo == null) {
            return ServiceUtil.returnError("No role found for partyIdTo: " + partyIdTo);
        }

        String roleTypeIdTo = roleTo.getString("roleTypeId");

        //  Create a new PartyRelationship entry
        GenericValue newRelationship = delegator.makeValue("PartyRelationship");
        newRelationship.set("partyIdFrom", partyIdFrom);
        newRelationship.set("partyIdTo", partyIdTo);
        newRelationship.set("roleTypeIdFrom", roleTypeIdFrom);
        newRelationship.set("roleTypeIdTo", roleTypeIdTo);
        newRelationship.set("partyRelationshipTypeId", partyRelationshipTypeId);
        newRelationship.set("fromDate", fromDate);
        delegator.create(newRelationship);

        result.put("message", "Customer relationship created successfully.");

    } catch (GenericEntityException e) {
        Debug.logError(e, MODULE);
        return ServiceUtil.returnError("Error creating customer relationship: " + e.getMessage());
    }

    return result;
}



    public static Map<String, Object> updateCustomerRelationship(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String partyIdFrom = (String) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");
        String partyRelationshipTypeId = (String) context.get("partyRelationshipTypeId");
        String statusId = (String) context.get("statusId");

        if (partyIdFrom == null || partyIdTo == null || partyRelationshipTypeId == null) {
            return ServiceUtil.returnError("Missing required fields: partyIdFrom, partyIdTo, or partyRelationshipTypeId.");
        }

        try {
            // ✅ Find the existing active relationship
            GenericValue existingRelationship = EntityQuery.use(delegator)
                    .from("PartyRelationship")
                    .where("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo, "partyRelationshipTypeId", partyRelationshipTypeId)
                    .orderBy("-fromDate") // Get the latest relationship
                    .filterByDate() // Ensure it is still active
                    .queryFirst();

            if (existingRelationship != null) {
                // ✅ Close the existing relationship by setting `thruDate`
                Timestamp now = UtilDateTime.nowTimestamp();
                existingRelationship.set("thruDate", now);
                existingRelationship.store();
            }

            // ✅ Fetch roleTypeIdFrom from PartyRole
            GenericValue roleFrom = EntityQuery.use(delegator)
                    .from("PartyRole")
                    .where("partyId", partyIdFrom)
                    .queryFirst();

            if (roleFrom == null) {
                return ServiceUtil.returnError("No role found for partyIdFrom: " + partyIdFrom);
            }

            String roleTypeIdFrom = roleFrom.getString("roleTypeId");

            // ✅ Fetch roleTypeIdTo from PartyRole
            GenericValue roleTo = EntityQuery.use(delegator)
                    .from("PartyRole")
                    .where("partyId", partyIdTo)
                    .queryFirst();

            if (roleTo == null) {
                return ServiceUtil.returnError("No role found for partyIdTo: " + partyIdTo);
            }

            String roleTypeIdTo = roleTo.getString("roleTypeId");

            // ✅ Create a new relationship with the updated status
            GenericValue newRelationship = delegator.makeValue("PartyRelationship");
            newRelationship.set("partyIdFrom", partyIdFrom);
            newRelationship.set("partyIdTo", partyIdTo);
            newRelationship.set("roleTypeIdFrom", roleTypeIdFrom);
            newRelationship.set("roleTypeIdTo", roleTypeIdTo);
            newRelationship.set("partyRelationshipTypeId", partyRelationshipTypeId);
            newRelationship.set("fromDate", UtilDateTime.nowTimestamp());
            if (statusId != null && !statusId.isEmpty()) {
                newRelationship.set("statusId", statusId);
            }
            delegator.create(newRelationship);

            result.put("message", "Customer relationship updated successfully. Old relationship closed.");

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("Error updating customer relationship: " + e.getMessage());
        }

        return result;
    }


}
