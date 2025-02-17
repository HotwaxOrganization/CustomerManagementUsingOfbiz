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


    private static final int PAGE_SIZE = 10;

    public static Map<String, Object> findCustomer(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<EntityCondition> entityConditions = new ArrayList<>();

        String partyId = (String) context.get("partyId");
        String customerName = (String) context.get("customerName");
        String emailAddress = (String) context.get("emailAddress");
        String contactNumber = (String) context.get("contactNumber");
        String postalAddress = (String) context.get("postalAddress");
        Integer currentPage = (Integer) context.get("currentPage");
        if (currentPage == null || currentPage < 1) currentPage = 1;

        try {
            if (partyId != null && !partyId.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
            }
            if (customerName != null && !customerName.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("firstName", EntityOperator.LIKE, "%" + customerName + "%"));
                entityConditions.add(EntityCondition.makeCondition("lastName", EntityOperator.LIKE, "%" + customerName + "%"));
            }
            if (emailAddress != null && !emailAddress.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("emailAddress", EntityOperator.LIKE, "%" + emailAddress + "%"));
            }
            if (contactNumber != null && !contactNumber.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("contactNumber", EntityOperator.LIKE, "%" + contactNumber + "%"));
            }
            if (postalAddress != null && !postalAddress.isEmpty()) {
                entityConditions.add(EntityCondition.makeCondition("postalAddress", EntityOperator.LIKE, "%" + postalAddress + "%"));
            }

            EntityCondition combinedCondition = entityConditions.isEmpty() ? null : EntityCondition.makeCondition(entityConditions, EntityOperator.AND);

            List<GenericValue> fullCustomerList = EntityQuery.use(delegator)
                    .from("FindCustomerView")
                    .where(combinedCondition)
                    .orderBy("partyId")
                    .queryList();

            int totalRecords = fullCustomerList.size(); // 🔥 Get total records count
            int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
            int offset = (currentPage - 1) * PAGE_SIZE;
            int endIndex = Math.min(offset + PAGE_SIZE, totalRecords);

            List<GenericValue> paginatedList = fullCustomerList.subList(offset, endIndex); // 🔥 Manual Pagination

            result.put("customerList", paginatedList);
            result.put("currentPage", currentPage);
            result.put("totalPages", totalPages);
            result.put("totalRecords", totalRecords);


        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("Error finding customers: " + e.getMessage());
        }

        return result;
    }


    public static Map<String, Object> createCustomer(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String emailAddress = (String) context.get("emailAddress");
        String firstName = (String) context.get("firstName");
        String lastName = (String) context.get("lastName");
        String contactNumber = (String) context.get("contactNumber");
        String postalAddress = (String) context.get("postalAddress");

        if (firstName == null || lastName == null || emailAddress == null) {
            return ServiceUtil.returnError("Missing required fields: firstName, lastName, or emailAddress");
        }

        try {
            //  Ensure PRIMARY_EMAIL exists in ContactMechPurposeType
            GenericValue emailPurposeType = EntityQuery.use(delegator)
                    .from("ContactMechPurposeType")
                    .where("contactMechPurposeTypeId", "PRIMARY_EMAIL")
                    .queryOne();
            if (emailPurposeType == null) {
                return ServiceUtil.returnError("PRIMARY_EMAIL does not exist in ContactMechPurposeType.");
            }

            GenericValue existingCustomer = EntityQuery.use(delegator)
                    .from("FindCustomerView")
                    .where("emailAddress", emailAddress)
                    .queryFirst();

            if (existingCustomer != null) {
                return ServiceUtil.returnError("Customer with email " + emailAddress + " already exists.");
            }

            String partyId = delegator.getNextSeqId("Party");

            GenericValue newParty = delegator.makeValue("Party");
            newParty.set("partyId", partyId);
            newParty.set("partyTypeId", "PERSON");
            newParty.set("statusId", "PARTY_ENABLED");
            newParty.set("createdDate", UtilDateTime.nowTimestamp());
            delegator.create(newParty);

            GenericValue newPerson = delegator.makeValue("Person");
            newPerson.set("partyId", partyId);
            newPerson.set("firstName", firstName);
            newPerson.set("lastName", lastName);
            delegator.create(newPerson);

            GenericValue partyRole = delegator.makeValue("PartyRole");
            partyRole.set("partyId", partyId);
            partyRole.set("roleTypeId", "CUSTOMER");
            delegator.create(partyRole);

            String emailMechId = delegator.getNextSeqId("ContactMech");
            GenericValue newEmailMech = delegator.makeValue("ContactMech");
            newEmailMech.set("contactMechId", emailMechId);
            newEmailMech.set("contactMechTypeId", "EMAIL_ADDRESS");
            newEmailMech.set("infoString", emailAddress);
            delegator.create(newEmailMech);

            GenericValue newPartyEmailMech = delegator.makeValue("PartyContactMech");
            newPartyEmailMech.set("partyId", partyId);
            newPartyEmailMech.set("contactMechId", emailMechId);
            newPartyEmailMech.set("fromDate", UtilDateTime.nowTimestamp());
            delegator.create(newPartyEmailMech);

            GenericValue emailPurpose = delegator.makeValue("PartyContactMechPurpose");
            emailPurpose.set("partyId", partyId);
            emailPurpose.set("contactMechId", emailMechId);
            emailPurpose.set("contactMechPurposeTypeId", "PRIMARY_EMAIL");
            emailPurpose.set("fromDate", UtilDateTime.nowTimestamp());
            delegator.create(emailPurpose);

            result.put("partyId", partyId);
            result.put("message", "Customer created successfully.");
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
            //  Find the existing active relationship
            GenericValue existingRelationship = EntityQuery.use(delegator)
                    .from("PartyRelationship")
                    .where("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo, "partyRelationshipTypeId", partyRelationshipTypeId)
                    .orderBy("-fromDate") // Get the latest relationship
                    .filterByDate() // Ensure it is still active
                    .queryFirst();

            if (existingRelationship != null) {
                //  Close the existing relationship by setting `thruDate`
                Timestamp now = UtilDateTime.nowTimestamp();
                existingRelationship.set("thruDate", now);
                existingRelationship.store();
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

            //  Create a new relationship with the updated status
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
