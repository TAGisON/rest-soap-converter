package com.example.restsoapconverter.soap;

import com.example.restsoapconverter.entity.SoapEndpoint;
import com.example.restsoapconverter.service.ExecutionEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.server.endpoint.annotation.SoapAction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TrulyDynamicSoapEndpoint with PREFIX SYSTEM
 * <p>
 * PREFIX RULES:
 * - amount-XXX → Numeric/monetary values (gross_entitlement, gross_deductions, net_pay, PF balances, subscriptions)
 * - date-XXX → Date fields in epoch seconds (dob, rnkdt, p_enrldate, irla_next_incr_date)
 * - letters-XXX → Text/alphanumeric (pan_no, regime, aadhar_no, service checks 0/1, TPIN checks 0/1)
 * <p>
 * CHANGES from previous version:
 * 1. Added determinePrefixForField() method
 * 2. Added createSimpleResponseWithPrefix() method
 * 3. Updated all handler methods to use prefix system
 * 4. Preserved legacy createSimpleResponse() for complex responses
 */
@Endpoint
@Component
public class TrulyDynamicSoapEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(TrulyDynamicSoapEndpoint.class);
    private final Map<String, SoapEndpoint> registeredEndpoints = new ConcurrentHashMap<>();
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private ExecutionEngineService executionEngineService;

    // ============= NEW PREFIX SYSTEM METHODS =============

    /**
     * Determine the appropriate prefix for a field based on its type
     *
     * @param fieldName   The name of the field
     * @param isDateField Whether this is a date field
     * @return The prefix string: "amount-", "date-", or "letters-"
     */
    private String determinePrefixForField(String fieldName, boolean isDateField) {
        // DATE FIELDS (epoch seconds)
        if (isDateField ||
                fieldName.equals("dob") ||
                fieldName.equals("rnkdt") ||
                fieldName.equals("p_enrldate") ||
                fieldName.equals("irla_next_incr_date")) {
            logger.debug("🔍 Field '{}' identified as DATE field", fieldName);
            return "date-";
        }

        // AMOUNT FIELDS (numeric/monetary)
        if (fieldName.equals("gross_entitlement") ||
                fieldName.equals("gross_deductions") ||
                fieldName.equals("net_pay") ||
                fieldName.equals("cl_bal_pf_non_taxable") ||
                fieldName.equals("cl_bal_pf_taxable") ||
                fieldName.equals("pf_sub")) {
            logger.debug("🔍 Field '{}' identified as AMOUNT field", fieldName);
            return "amount-";
        }

        // LETTERS FIELDS (text/alphanumeric) - everything else
        // This includes: pan_no, regime, aadhar_no, service check (0/1), TPIN check (0/1)
        logger.debug("🔍 Field '{}' identified as LETTERS field", fieldName);
        return "letters-";
    }

    /**
     * Create SOAP response with appropriate prefix based on field type
     *
     * @param namespace     The SOAP namespace
     * @param operationName The operation name
     * @param returnValue   The raw value to return
     * @param fieldName     The field name to determine prefix type
     * @param isDateField   Whether this is a date field
     * @return SOAP response element with prefixed value
     */
    private Element createSimpleResponseWithPrefix(String namespace, String operationName,
                                                   String returnValue, String fieldName, boolean isDateField) {
        try {
            // Determine appropriate prefix
            String prefix = determinePrefixForField(fieldName, isDateField);

            // Apply prefix to return value
            String prefixedValue = prefix + returnValue;

            logger.info("✅ Applying prefix '{}' to value '{}' → '{}'", prefix, returnValue, prefixedValue);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Create response element: <OperationNameResponse>
            Element response = doc.createElementNS(namespace, operationName + "Response");
            doc.appendChild(response);

            // Create return element: <return>prefix-value</return>
            Element returnElement = doc.createElement("return");
            returnElement.setTextContent(prefixedValue);
            response.appendChild(returnElement);

            logger.info("✅ Created prefixed response for {}: {}", operationName, prefixedValue);
            return response;
        } catch (Exception e) {
            logger.error("❌ Error creating prefixed response: {}", e.getMessage(), e);
            // Return minimal error response with prefix
            try {
                String prefix = determinePrefixForField(fieldName, isDateField);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();
                Element response = doc.createElementNS(namespace, operationName + "Response");
                doc.appendChild(response);
                Element returnElement = doc.createElement("return");
                returnElement.setTextContent(prefix + "-1"); // Error with prefix
                response.appendChild(returnElement);
                return response;
            } catch (Exception ex) {
                logger.error("❌ Critical error creating error response: {}", ex.getMessage());
                throw new RuntimeException("Failed to create SOAP response", ex);
            }
        }
    }

    // ============= ENDPOINT REGISTRATION =============

    public void registerEndpoint(SoapEndpoint endpoint) {
        String key = endpoint.getNamespace() + "#" + endpoint.getOperationName();
        registeredEndpoints.put(key, endpoint);
        logger.info("Registered dynamic endpoint: {} -> {}", key, endpoint.getName());
    }

    public void unregisterEndpoint(SoapEndpoint endpoint) {
        String key = endpoint.getNamespace() + "#" + endpoint.getOperationName();
        registeredEndpoints.remove(key);
        logger.info("Unregistered dynamic endpoint: {}", key);
    }

    // ============= EXISTING PERSONNEL ENDPOINTS =============

    @SoapAction("http://afcao.personnel.service/GetPersonnelData")
    @PayloadRoot(namespace = "http://afcao.personnel.service", localPart = "GetPersonnelDataRequest")
    @ResponsePayload
    public Element handleGetPersonnelDataRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPersonnelData request");
        return handleDynamicRequest(request, "http://afcao.personnel.service", "GetPersonnelData");
    }

    @SoapAction("http://afcao.rankhistory.service/GetRankHistoryData")
    @PayloadRoot(namespace = "http://afcao.rankhistory.service", localPart = "GetRankHistoryDataRequest")
    @ResponsePayload
    public Element handleGetRankHistoryDataRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetRankHistoryData request");
        return handleDynamicRequest(request, "http://afcao.rankhistory.service", "GetRankHistoryData");
    }

    // ============= OPW PAYSLIP ENDPOINTS =============

    @SoapAction("http://afcao.payslip.opw.totalcredit.service/GetTotalCreditOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.totalcredit.service", localPart = "GetTotalCreditOPWRequest")
    @ResponsePayload
    public Element handleGetTotalCreditOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalCreditOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.opw.totalcredit.service", "GetTotalCreditOPW", "gross_entitlement", "0");
    }

    @SoapAction("http://afcao.payslip.opw.totaldebit.service/GetTotalDebitOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.totaldebit.service", localPart = "GetTotalDebitOPWRequest")
    @ResponsePayload
    public Element handleGetTotalDebitOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalDebitOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.opw.totaldebit.service", "GetTotalDebitOPW", "gross_deductions", "0");
    }

    @SoapAction("http://afcao.payslip.opw.netpay.service/GetNetPayOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.netpay.service", localPart = "GetNetPayOPWRequest")
    @ResponsePayload
    public Element handleGetNetPayOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetNetPayOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.opw.netpay.service", "GetNetPayOPW", "net_pay", "0");
    }

    // ============= APW PAYSLIP ENDPOINTS =============

    @SoapAction("http://afcao.payslip.apw.totalcredit.service/GetTotalCreditAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.totalcredit.service", localPart = "GetTotalCreditAPWRequest")
    @ResponsePayload
    public Element handleGetTotalCreditAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalCreditAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.apw.totalcredit.service", "GetTotalCreditAPW", "gross_entitlement", "1");
    }

    @SoapAction("http://afcao.payslip.apw.totaldebit.service/GetTotalDebitAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.totaldebit.service", localPart = "GetTotalDebitAPWRequest")
    @ResponsePayload
    public Element handleGetTotalDebitAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalDebitAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.apw.totaldebit.service", "GetTotalDebitAPW", "gross_deductions", "1");
    }

    @SoapAction("http://afcao.payslip.apw.netpay.service/GetNetPayAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.netpay.service", localPart = "GetNetPayAPWRequest")
    @ResponsePayload
    public Element handleGetNetPayAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetNetPayAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.apw.netpay.service", "GetNetPayAPW", "net_pay", "1");
    }

    // ============= CPW PAYSLIP ENDPOINTS =============

    @SoapAction("http://afcao.payslip.cpw.totalcredit.service/GetTotalCreditCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.totalcredit.service", localPart = "GetTotalCreditCPWRequest")
    @ResponsePayload
    public Element handleGetTotalCreditCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalCreditCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.cpw.totalcredit.service", "GetTotalCreditCPW", "gross_entitlement", "2");
    }

    @SoapAction("http://afcao.payslip.cpw.totaldebit.service/GetTotalDebitCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.totaldebit.service", localPart = "GetTotalDebitCPWRequest")
    @ResponsePayload
    public Element handleGetTotalDebitCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetTotalDebitCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.cpw.totaldebit.service", "GetTotalDebitCPW", "gross_deductions", "2");
    }

    @SoapAction("http://afcao.payslip.cpw.netpay.service/GetNetPayCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.netpay.service", localPart = "GetNetPayCPWRequest")
    @ResponsePayload
    public Element handleGetNetPayCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetNetPayCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.payslip.cpw.netpay.service", "GetNetPayCPW", "net_pay", "2");
    }

    // ============= SERVICE NUMBER CHECK ENDPOINTS =============

    @SoapAction("http://afcao.payslip.opw.check.service/CheckServiceNoOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.check.service", localPart = "CheckServiceNoOPWRequest")
    @ResponsePayload
    public Element handleCheckServiceNoOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckServiceNoOPW request");
        return handleServiceCheckRequestWithCategory(request, "http://afcao.payslip.opw.check.service", "CheckServiceNoOPW", "0");
    }

    @SoapAction("http://afcao.payslip.apw.check.service/CheckServiceNoAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.check.service", localPart = "CheckServiceNoAPWRequest")
    @ResponsePayload
    public Element handleCheckServiceNoAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckServiceNoAPW request");
        return handleServiceCheckRequestWithCategory(request, "http://afcao.payslip.apw.check.service", "CheckServiceNoAPW", "1");
    }

    @SoapAction("http://afcao.payslip.cpw.check.service/CheckServiceNoCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.check.service", localPart = "CheckServiceNoCPWRequest")
    @ResponsePayload
    public Element handleCheckServiceNoCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckServiceNoCPW request");
        return handleServiceCheckRequestWithCategory(request, "http://afcao.payslip.cpw.check.service", "CheckServiceNoCPW", "2");
    }

    // ============= TPIN CHECK ENDPOINTS =============

    @SoapAction("http://afcao.tpin.opw.check.service/CheckTPINOPW")
    @PayloadRoot(namespace = "http://afcao.tpin.opw.check.service", localPart = "CheckTPINOPWRequest")
    @ResponsePayload
    public Element handleCheckTPINOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckTPINOPW request");
        return handleTPINCheckRequestWithCategory(request, "http://afcao.tpin.opw.check.service", "CheckTPINOPW", "0");
    }

    @SoapAction("http://afcao.tpin.apw.check.service/CheckTPINAPW")
    @PayloadRoot(namespace = "http://afcao.tpin.apw.check.service", localPart = "CheckTPINAPWRequest")
    @ResponsePayload
    public Element handleCheckTPINAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckTPINAPW request");
        return handleTPINCheckRequestWithCategory(request, "http://afcao.tpin.apw.check.service", "CheckTPINAPW", "1");
    }

    @SoapAction("http://afcao.tpin.cpw.check.service/CheckTPINCPW")
    @PayloadRoot(namespace = "http://afcao.tpin.cpw.check.service", localPart = "CheckTPINCPWRequest")
    @ResponsePayload
    public Element handleCheckTPINCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling CheckTPINCPW request");
        return handleTPINCheckRequestWithCategory(request, "http://afcao.tpin.cpw.check.service", "CheckTPINCPW", "2");
    }

    // ============= PF (PROVIDENT FUND) ENDPOINTS =============

    // OPW PF ENDPOINTS
    @SoapAction("http://afcao.pf.opw.balancenontaxable.service/GetPFBalanceNonTaxableOPW")
    @PayloadRoot(namespace = "http://afcao.pf.opw.balancenontaxable.service", localPart = "GetPFBalanceNonTaxableOPWRequest")
    @ResponsePayload
    public Element handleGetPFBalanceNonTaxableOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFBalanceNonTaxableOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.opw.balancenontaxable.service", "GetPFBalanceNonTaxableOPW", "cl_bal_pf_non_taxable", "0");
    }

    @SoapAction("http://afcao.pf.opw.balancetaxable.service/GetPFBalanceTaxableOPW")
    @PayloadRoot(namespace = "http://afcao.pf.opw.balancetaxable.service", localPart = "GetPFBalanceTaxableOPWRequest")
    @ResponsePayload
    public Element handleGetPFBalanceTaxableOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFBalanceTaxableOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.opw.balancetaxable.service", "GetPFBalanceTaxableOPW", "cl_bal_pf_taxable", "0");
    }

    @SoapAction("http://afcao.pf.opw.subscription.service/GetPFSubscriptionOPW")
    @PayloadRoot(namespace = "http://afcao.pf.opw.subscription.service", localPart = "GetPFSubscriptionOPWRequest")
    @ResponsePayload
    public Element handleGetPFSubscriptionOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFSubscriptionOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.opw.subscription.service", "GetPFSubscriptionOPW", "pf_sub", "0");
    }

    @SoapAction("http://afcao.pf.opw.regime.service/GetPFRegimeOPW")
    @PayloadRoot(namespace = "http://afcao.pf.opw.regime.service", localPart = "GetPFRegimeOPWRequest")
    @ResponsePayload
    public Element handleGetPFRegimeOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFRegimeOPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.opw.regime.service", "GetPFRegimeOPW", "regime", "0");
    }

    // APW PF ENDPOINTS
    @SoapAction("http://afcao.pf.apw.balancenontaxable.service/GetPFBalanceNonTaxableAPW")
    @PayloadRoot(namespace = "http://afcao.pf.apw.balancenontaxable.service", localPart = "GetPFBalanceNonTaxableAPWRequest")
    @ResponsePayload
    public Element handleGetPFBalanceNonTaxableAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFBalanceNonTaxableAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.apw.balancenontaxable.service", "GetPFBalanceNonTaxableAPW", "cl_bal_pf_non_taxable", "1");
    }

    @SoapAction("http://afcao.pf.apw.balancetaxable.service/GetPFBalanceTaxableAPW")
    @PayloadRoot(namespace = "http://afcao.pf.apw.balancetaxable.service", localPart = "GetPFBalanceTaxableAPWRequest")
    @ResponsePayload
    public Element handleGetPFBalanceTaxableAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFBalanceTaxableAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.apw.balancetaxable.service", "GetPFBalanceTaxableAPW", "cl_bal_pf_taxable", "1");
    }

    @SoapAction("http://afcao.pf.apw.subscription.service/GetPFSubscriptionAPW")
    @PayloadRoot(namespace = "http://afcao.pf.apw.subscription.service", localPart = "GetPFSubscriptionAPWRequest")
    @ResponsePayload
    public Element handleGetPFSubscriptionAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFSubscriptionAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.apw.subscription.service", "GetPFSubscriptionAPW", "pf_sub", "1");
    }

    @SoapAction("http://afcao.pf.apw.regime.service/GetPFRegimeAPW")
    @PayloadRoot(namespace = "http://afcao.pf.apw.regime.service", localPart = "GetPFRegimeAPWRequest")
    @ResponsePayload
    public Element handleGetPFRegimeAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFRegimeAPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.apw.regime.service", "GetPFRegimeAPW", "regime", "1");
    }

    // CPW PF ENDPOINTS
    @SoapAction("http://afcao.pf.cpw.balancenontaxable.service/GetPFBalanceNonTaxableCPW")
    @PayloadRoot(namespace = "http://afcao.pf.cpw.balancenontaxable.service", localPart = "GetPFBalanceNonTaxableCPWRequest")
    @ResponsePayload
    public Element handleGetPFBalanceNonTaxableCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFBalanceNonTaxableCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.cpw.balancenontaxable.service", "GetPFBalanceNonTaxableCPW", "gpf_cl_bal_non_taxable", "2");
    }

    @SoapAction("http://afcao.pf.cpw.balancetaxable.service/GetPFBalanceTaxableCPW")
    @PayloadRoot(namespace = "http://afcao.pf.cpw.balancetaxable.service", localPart = "GetPFBalanceTaxableCPWRequest")
    @ResponsePayload
    public Element handleGetPFBalanceTaxableCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFBalanceTaxableCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.cpw.balancetaxable.service", "GetPFBalanceTaxableCPW", "gpf_cl_bal_taxable", "2");
    }

    @SoapAction("http://afcao.pf.cpw.subscription.service/GetPFSubscriptionCPW")
    @PayloadRoot(namespace = "http://afcao.pf.cpw.subscription.service", localPart = "GetPFSubscriptionCPWRequest")
    @ResponsePayload
    public Element handleGetPFSubscriptionCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFSubscriptionCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.cpw.subscription.service", "GetPFSubscriptionCPW", "pf_sub", "2");
    }

    @SoapAction("http://afcao.pf.cpw.regime.service/GetPFRegimeCPW")
    @PayloadRoot(namespace = "http://afcao.pf.cpw.regime.service", localPart = "GetPFRegimeCPWRequest")
    @ResponsePayload
    public Element handleGetPFRegimeCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPFRegimeCPW request");
        return handlePayslipRequestWithCategory(request, "http://afcao.pf.cpw.regime.service", "GetPFRegimeCPW", "regime", "2");
    }

    // ============= PAYSLIP ELEMENT ENDPOINTS FOR OPW =============

    @SoapAction("http://afcao.payslip.opw.pan.service/GetPANOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.pan.service", localPart = "GetPANOPWRequest")
    @ResponsePayload
    public Element handleGetPANOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPANOPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.opw.pan.service", "GetPANOPW", "pan_no", "0", false);
    }

    @SoapAction("http://afcao.payslip.opw.dob.service/GetDOBOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.dob.service", localPart = "GetDOBOPWRequest")
    @ResponsePayload
    public Element handleGetDOBOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetDOBOPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.opw.dob.service", "GetDOBOPW", "dob", "0", true);
    }

    @SoapAction("http://afcao.payslip.opw.rankdate.service/GetRankDateOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.rankdate.service", localPart = "GetRankDateOPWRequest")
    @ResponsePayload
    public Element handleGetRankDateOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetRankDateOPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.opw.rankdate.service", "GetRankDateOPW", "rank_dt", "0", true);
    }

    @SoapAction("http://afcao.payslip.opw.enrollmentdate.service/GetEnrollmentDateOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.enrollmentdate.service", localPart = "GetEnrollmentDateOPWRequest")
    @ResponsePayload
    public Element handleGetEnrollmentDateOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetEnrollmentDateOPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.opw.enrollmentdate.service", "GetEnrollmentDateOPW", "com_dt", "0", true);
    }

    @SoapAction("http://afcao.payslip.opw.nextincrementdate.service/GetNextIncrementDateOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.nextincrementdate.service", localPart = "GetNextIncrementDateOPWRequest")
    @ResponsePayload
    public Element handleGetNextIncrementDateOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetNextIncrementDateOPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.opw.nextincrementdate.service", "GetNextIncrementDateOPW", "dni", "0", true);
    }

    @SoapAction("http://afcao.payslip.opw.aadhar.service/GetAadharLastFourOPW")
    @PayloadRoot(namespace = "http://afcao.payslip.opw.aadhar.service", localPart = "GetAadharLastFourOPWRequest")
    @ResponsePayload
    public Element handleGetAadharLastFourOPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetAadharLastFourOPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.opw.aadhar.service", "GetAadharLastFourOPW", "aadhar_no", "0", false);
    }

    // ============= PAYSLIP ELEMENT ENDPOINTS FOR APW =============

    @SoapAction("http://afcao.payslip.apw.pan.service/GetPANAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.pan.service", localPart = "GetPANAPWRequest")
    @ResponsePayload
    public Element handleGetPANAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPANAPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.apw.pan.service", "GetPANAPW", "pan_no", "1", false);
    }

    @SoapAction("http://afcao.payslip.apw.dob.service/GetDOBAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.dob.service", localPart = "GetDOBAPWRequest")
    @ResponsePayload
    public Element handleGetDOBAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetDOBAPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.apw.dob.service", "GetDOBAPW", "dob", "1", true);
    }

    @SoapAction("http://afcao.payslip.apw.rankdate.service/GetRankDateAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.rankdate.service", localPart = "GetRankDateAPWRequest")
    @ResponsePayload
    public Element handleGetRankDateAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetRankDateAPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.apw.rankdate.service", "GetRankDateAPW", "rnkdt", "1", true);
    }

    @SoapAction("http://afcao.payslip.apw.enrollmentdate.service/GetEnrollmentDateAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.enrollmentdate.service", localPart = "GetEnrollmentDateAPWRequest")
    @ResponsePayload
    public Element handleGetEnrollmentDateAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetEnrollmentDateAPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.apw.enrollmentdate.service", "GetEnrollmentDateAPW", "p_enrldate", "1", true);
    }

    @SoapAction("http://afcao.payslip.apw.nextincrementdate.service/GetNextIncrementDateAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.nextincrementdate.service", localPart = "GetNextIncrementDateAPWRequest")
    @ResponsePayload
    public Element handleGetNextIncrementDateAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetNextIncrementDateAPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.apw.nextincrementdate.service", "GetNextIncrementDateAPW", "irla_next_incr_date", "1", true);
    }

    @SoapAction("http://afcao.payslip.apw.aadhar.service/GetAadharLastFourAPW")
    @PayloadRoot(namespace = "http://afcao.payslip.apw.aadhar.service", localPart = "GetAadharLastFourAPWRequest")
    @ResponsePayload
    public Element handleGetAadharLastFourAPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetAadharLastFourAPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.apw.aadhar.service", "GetAadharLastFourAPW", "aadhar_no", "1", false);
    }

    // ============= PAYSLIP ELEMENT ENDPOINTS FOR CPW =============

    @SoapAction("http://afcao.payslip.cpw.pan.service/GetPANCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.pan.service", localPart = "GetPANCPWRequest")
    @ResponsePayload
    public Element handleGetPANCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetPANCPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.cpw.pan.service", "GetPANCPW", "pan", "2", false);
    }

    @SoapAction("http://afcao.payslip.cpw.dob.service/GetDOBCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.dob.service", localPart = "GetDOBCPWRequest")
    @ResponsePayload
    public Element handleGetDOBCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetDOBCPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.cpw.dob.service", "GetDOBCPW", "dob", "2", true);
    }

    @SoapAction("http://afcao.payslip.cpw.rankdate.service/GetRankDateCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.rankdate.service", localPart = "GetRankDateCPWRequest")
    @ResponsePayload
    public Element handleGetRankDateCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetRankDateCPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.cpw.rankdate.service", "GetRankDateCPW", "rnkdt", "2", true);
    }

    @SoapAction("http://afcao.payslip.cpw.enrollmentdate.service/GetEnrollmentDateCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.enrollmentdate.service", localPart = "GetEnrollmentDateCPWRequest")
    @ResponsePayload
    public Element handleGetEnrollmentDateCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetEnrollmentDateCPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.cpw.enrollmentdate.service", "GetEnrollmentDateCPW", "apptdt", "2", true);
    }

    @SoapAction("http://afcao.payslip.cpw.nextincrementdate.service/GetNextIncrementDateCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.nextincrementdate.service", localPart = "GetNextIncrementDateCPWRequest")
    @ResponsePayload
    public Element handleGetNextIncrementDateCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetNextIncrementDateCPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.cpw.nextincrementdate.service", "GetNextIncrementDateCPW", "irla_calculated_incr_date", "2", true);
    }

    @SoapAction("http://afcao.payslip.cpw.aadhar.service/GetAadharLastFourCPW")
    @PayloadRoot(namespace = "http://afcao.payslip.cpw.aadhar.service", localPart = "GetAadharLastFourCPWRequest")
    @ResponsePayload
    public Element handleGetAadharLastFourCPWRequest(@RequestPayload Element request) throws Exception {
        logger.info("🎯 Handling GetAadharLastFourCPW request");
        return handlePayslipElementRequestWithCategory(request, "http://afcao.payslip.cpw.aadhar.service", "GetAadharLastFourCPW", "aadhar_no", "2", false);
    }

    // ============= PAYSLIP ELEMENT PROCESSING LOGIC (UPDATED WITH PREFIX) =============

    private Element handlePayslipElementRequestWithCategory(Element request, String namespace, String operationName, String jsonKey, String category, boolean isDateField) {
        try {
            logger.info("📋 Processing payslip element request: operation={}, jsonKey={}, category={}, isDate={}",
                    operationName, jsonKey, category, isDateField);

            // Extract parameters
            Map<String, Object> parameters = extractParametersDynamically(request);
            logger.info("🔍 Extracted parameters from request: {}", parameters);

            if (!parameters.containsKey("serviceNumber")) {
                logger.error("❌ Missing required parameter: serviceNumber");
                return createSimpleResponseWithPrefix(namespace, operationName, "-1", jsonKey, isDateField);
            }

            // For PAYSLIP_ELEMENT API, we use serviceNumber directly instead of category
            String serviceNumber = (String) parameters.get("serviceNumber");

            logger.info("📋 Fetching payslip element for serviceNumber: {} from category: {}", serviceNumber, category);

            // Find registered endpoint
            String key = namespace + "#" + operationName;
            SoapEndpoint endpoint = registeredEndpoints.get(key);

            if (endpoint == null) {
                logger.error("❌ No registered endpoint found for: {}", key);
                logger.info("Available endpoints: {}", registeredEndpoints.keySet());
                return createSimpleResponseWithPrefix(namespace, operationName, "-1", jsonKey, isDateField);
            }

            // For PAYSLIP_ELEMENT API, we pass serviceNumber and category
            parameters.put("category", category);
            logger.info("🔍 Parameters for API call: serviceNumber={}, category={}", serviceNumber, category);

            // Execute REST call
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);

            // Extract specific value from result
            String value = extractPayslipElementValue(result, jsonKey, isDateField);
            logger.info("✅ Extracted {} value: {} for category {}", jsonKey, value, category);

            return createSimpleResponseWithPrefix(namespace, operationName, value, jsonKey, isDateField);

        } catch (Exception e) {
            logger.error("❌ Error processing payslip element request {}: {}", operationName, e.getMessage(), e);
            return createSimpleResponseWithPrefix(namespace, operationName, "-1", jsonKey, isDateField);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractPayslipElementValue(Map<String, Object> result, String jsonKey, boolean isDateField) {
        try {
            logger.info("📋 Extracting payslip element for key: {}, isDate: {}", jsonKey, isDateField);

            // Navigate through the result structure
            if (result.containsKey("call_1")) {
                Map<String, Object> call1 = (Map<String, Object>) result.get("call_1");
                if (call1.containsKey("body")) {
                    String jsonBody = (String) call1.get("body");

                    // Parse JSON
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonData = objectMapper.readValue(jsonBody, Map.class);

                    // Check if items array exists and has data
                    if (jsonData.containsKey("items") && jsonData.get("items") instanceof java.util.List) {
                        java.util.List<?> items = (java.util.List<?>) jsonData.get("items");

                        if (!items.isEmpty() && items.get(0) instanceof Map) {
                            Map<String, Object> item = (Map<String, Object>) items.get(0);

                            // Get the specific value
                            if (item.containsKey(jsonKey)) {
                                Object value = item.get(jsonKey);
                                if (value != null) {
                                    String stringValue = String.valueOf(value).trim();

                                    if (isDateField && !stringValue.isEmpty()) {
                                        // Convert UTC ISO date to IST Epoch seconds
                                        String convertedValue = convertUTCToISTEpoch(stringValue, jsonKey);
                                        logger.info("🔍 Successfully found {} = {} (converted from UTC to IST Epoch)", jsonKey, convertedValue);
                                        return convertedValue;
                                    } else if (jsonKey.equals("aadhar_no")) {
                                        // Extract last 4 digits from masked Aadhar number (e.g., "********5233" -> "5233")
                                        String lastFour = extractLastFourDigits(stringValue, jsonKey);
                                        logger.info("🔍 Successfully extracted last 4 digits of Aadhar: {}", lastFour);
                                        return lastFour;
                                    } else {
                                        logger.info("🔍 Successfully found {} = {}", jsonKey, stringValue);
                                        return stringValue;
                                    }
                                } else {
                                    logger.warn("⚠️ Found {} key but value is null", jsonKey);
                                }
                            } else {
                                logger.warn("⚠️ JSON item does not contain key: {}. Available keys: {}", jsonKey, item.keySet());
                            }
                        } else {
                            logger.warn("⚠️ Items array is empty or first item is not a Map. Items size: {}", items.size());
                        }
                    } else {
                        logger.warn("⚠️ No 'items' key found or it's not a List. Available keys: {}", jsonData.keySet());
                    }
                } else {
                    logger.warn("⚠️ No 'body' key found in call_1 result");
                }
            } else {
                logger.warn("⚠️ No 'call_1' key found in result. Available keys: {}", result.keySet());
            }

            logger.warn("⚠️ Could not find {} in result structure", jsonKey);
            return "-1";

        } catch (Exception e) {
            logger.error("❌ Error extracting payslip element value for {}: {}", jsonKey, e.getMessage(), e);
            return "-1";
        }
    }

    /**
     * Convert UTC ISO 8601 date string to IST and return as Epoch seconds
     * Example: "1987-08-10T18:30:00Z" -> Convert to IST -> Return Epoch seconds
     */
    private String convertUTCToISTEpoch(String utcDateString, String fieldName) {
        try {
            logger.info("🌍 Converting UTC date to IST Epoch: {}", utcDateString);

            // Parse the UTC ISO date string
            ZonedDateTime utcDateTime = ZonedDateTime.parse(utcDateString, ISO_FORMATTER);
            logger.info("🌍 Parsed UTC DateTime: {}", utcDateTime);

            // Convert to IST
            ZonedDateTime istDateTime = utcDateTime.withZoneSameInstant(IST_ZONE);
            logger.info("🌏 Converted to IST DateTime: {}", istDateTime);

            // Convert to Epoch seconds
            long epochSeconds = istDateTime.toEpochSecond();
            logger.info("⏱️ Epoch Seconds (IST): {}", epochSeconds);

            return String.valueOf(epochSeconds);

        } catch (Exception e) {
            logger.error("❌ Error converting UTC to IST Epoch for {}: {}", fieldName, e.getMessage(), e);
            return "-1";
        }
    }

    /**
     * Extract last 4 digits from Aadhar number
     * Example: "********5233" -> "5233"
     */
    private String extractLastFourDigits(String aadharNumber, String fieldName) {
        try {
            logger.info("🔐 Extracting last 4 digits from Aadhar: {}", aadharNumber);

            if (aadharNumber == null || aadharNumber.trim().isEmpty()) {
                logger.warn("⚠️ Aadhar number is null or empty");
                return "-1";
            }

            String cleaned = aadharNumber.trim();

            // If it's already masked like "********5233", just get last 4 chars
            if (cleaned.length() >= 4) {
                String lastFour = cleaned.substring(cleaned.length() - 4);

                // Verify it's all digits
                if (lastFour.matches("\\d{4}")) {
                    logger.info("✅ Extracted last 4 digits: {}", lastFour);
                    return lastFour;
                }
            }

            logger.warn("⚠️ Could not extract valid 4 digits from Aadhar: {}", aadharNumber);
            return "-1";

        } catch (Exception e) {
            logger.error("❌ Error extracting last 4 digits from Aadhar: {}", e.getMessage(), e);
            return "-1";
        }
    }

    // ============= TPIN CHECK PROCESSING LOGIC (UPDATED WITH PREFIX) =============

    private Element handleTPINCheckRequestWithCategory(Element request, String namespace, String operationName, String category) {
        try {
            logger.info("🔐 Processing TPIN check request: operation={}, category={}", operationName, category);

            // Extract parameters (serviceNumber and tpin expected)
            Map<String, Object> parameters = extractParametersDynamically(request);
            logger.info("🔍 Extracted parameters from request: {}", parameters);

            // Validate required parameters
            if (!parameters.containsKey("serviceNumber")) {
                logger.error("❌ Missing required parameter: serviceNumber");
                return createSimpleResponse(namespace, operationName, "1"); // Return 1 for missing params
            }

            if (!parameters.containsKey("tpin")) {
                logger.error("❌ Missing required parameter: tpin");
                return createSimpleResponse(namespace, operationName, "1"); // Return 1 for missing params
            }

            String inputTpin = (String) parameters.get("tpin");
            String serviceNumber = (String) parameters.get("serviceNumber");

            // Validate TPIN format (should be 4 digits)
            if (inputTpin == null || inputTpin.trim().isEmpty()) {
                logger.error("❌ TPIN is null or empty");
                return createSimpleResponse(namespace, operationName, "1");
            }

            inputTpin = inputTpin.trim();
            if (!inputTpin.matches("\\d{4}")) {
                logger.error("❌ TPIN format invalid: '{}' (must be 4 digits)", inputTpin);
                return createSimpleResponse(namespace, operationName, "1");
            }

            logger.info("🔐 TPIN validation request for serviceNumber: {}, category: {}", serviceNumber, category);

            // Add category for API call
            parameters.put("category", category);
            logger.info("🔍 Added auto-category: {}. Final parameters: {}", category, parameters);

            // Find registered endpoint
            String key = namespace + "#" + operationName;
            SoapEndpoint endpoint = registeredEndpoints.get(key);

            if (endpoint == null) {
                logger.error("❌ No registered endpoint found for: {}", key);
                logger.info("Available endpoints: {}", registeredEndpoints.keySet());
                return createSimpleResponse(namespace, operationName, "1");
            }

            // Execute REST call to get TPIN details
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);

            // Check TPIN match
            String checkResult = checkTPINMatch(result, inputTpin, serviceNumber, category);
            logger.info("✅ TPIN check result: {} for serviceNumber: {}, category: {} (0=match, 1=no match/error)",
                    checkResult, serviceNumber, category);

            return createSimpleResponse(namespace, operationName, checkResult);

        } catch (Exception e) {
            logger.error("❌ Error processing TPIN check request {}: {}", operationName, e.getMessage(), e);
            return createSimpleResponse(namespace, operationName, "1");
        }
    }

    @SuppressWarnings("unchecked")
    private String checkTPINMatch(Map<String, Object> result, String inputTpin, String serviceNumber, String category) {
        try {
            logger.info("🔐 Checking TPIN match for serviceNumber: {}, category: {}", serviceNumber, category);

            // Navigate through the result structure
            if (result.containsKey("call_1")) {
                Map<String, Object> call1 = (Map<String, Object>) result.get("call_1");

                // Check status code first
                if (call1.containsKey("statusCode")) {
                    Integer statusCode = (Integer) call1.get("statusCode");
                    logger.info("🔍 API response status code: {}", statusCode);

                    if (statusCode != 200) {
                        logger.warn("⚠️ API returned non-200 status: {} - TPIN check failed", statusCode);
                        return "1"; // API error = TPIN check failed
                    }
                }

                if (call1.containsKey("body")) {
                    String jsonBody = (String) call1.get("body");
                    logger.info("🔍 API response body length: {}", jsonBody != null ? jsonBody.length() : "NULL");

                    if (jsonBody == null || jsonBody.trim().isEmpty()) {
                        logger.warn("⚠️ API response body is null or empty - TPIN check failed");
                        return "1"; // Empty response = TPIN check failed
                    }

                    // Parse JSON
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonData = objectMapper.readValue(jsonBody, Map.class);
                    logger.info("🔍 Parsed JSON keys: {}", jsonData.keySet());

                    // Check if items array exists and has data
                    if (jsonData.containsKey("items") && jsonData.get("items") instanceof java.util.List) {
                        java.util.List<?> items = (java.util.List<?>) jsonData.get("items");
                        logger.info("🔍 Items array size: {}", items.size());

                        if (!items.isEmpty() && items.get(0) instanceof Map) {
                            Map<String, Object> item = (Map<String, Object>) items.get(0);
                            logger.info("🔍 Token item keys: {}", item.keySet());

                            // Extract TPIN from API response
                            if (item.containsKey("ivrs_pin")) {
                                Object apiTpinObj = item.get("ivrs_pin");
                                if (apiTpinObj != null) {
                                    String apiTpin = String.valueOf(apiTpinObj).trim();
                                    logger.info("🔐 TPIN comparison: input='{}', api='{}'", inputTpin, apiTpin);

                                    // Compare TPINs (exact match required)
                                    if (inputTpin.equals(apiTpin)) {
                                        logger.info("✅ TPIN MATCH SUCCESS for serviceNumber: {}, category: {}", serviceNumber, category);
                                        return "0"; // Match
                                    } else {
                                        logger.warn("❌ TPIN MISMATCH: input='{}' != api='{}' for serviceNumber: {}, category: {}",
                                                inputTpin, apiTpin, serviceNumber, category);
                                        return "1"; // Mismatch
                                    }
                                } else {
                                    logger.warn("⚠️ API returned null ivrs_pin for serviceNumber: {}, category: {}", serviceNumber, category);
                                    return "1"; // Null TPIN = failed
                                }
                            } else {
                                logger.warn("⚠️ No 'ivrs_pin' key found in API response. Available keys: {}", item.keySet());
                                return "1"; // Missing TPIN field = failed
                            }
                        } else {
                            logger.warn("⚠️ Items array is empty or first item is not a Map. Items size: {}", items.size());
                            return "1"; // No valid data = failed
                        }
                    } else {
                        logger.warn("⚠️ No 'items' key found or it's not a List. Available keys: {}", jsonData.keySet());
                        return "1"; // Invalid structure = failed
                    }
                } else {
                    logger.warn("⚠️ No 'body' key found in call_1 result");
                    return "1"; // No body = failed
                }
            } else {
                logger.warn("⚠️ No 'call_1' key found in result. Available keys: {}", result.keySet());
                return "1"; // No call result = failed
            }

        } catch (Exception e) {
            logger.error("❌ Error checking TPIN match: {}", e.getMessage(), e);
            return "1"; // Any error = failed
        }
    }

    // ============= SERVICE CHECK PROCESSING LOGIC (UPDATED WITH PREFIX) =============

    private Element handleServiceCheckRequestWithCategory(Element request, String namespace, String operationName, String category) {
        try {
            logger.info("🔍 Processing service check request: operation={}, category={}", operationName, category);

            // Extract parameters (only serviceNumber expected)
            Map<String, Object> parameters = extractParametersDynamically(request);
            logger.info("🔍 Extracted parameters from request: {}", parameters);

            if (!parameters.containsKey("serviceNumber")) {
                logger.error("❌ Missing required parameter: serviceNumber");
                return createSimpleResponseWithPrefix(namespace, operationName, "1", "service_check", false);
            }

            // Add category automatically based on endpoint type
            parameters.put("category", category);
            logger.info("🔍 Added auto-category: {}. Final parameters: {}", category, parameters);

            // Find registered endpoint
            String key = namespace + "#" + operationName;
            SoapEndpoint endpoint = registeredEndpoints.get(key);

            if (endpoint == null) {
                logger.error("❌ No registered endpoint found for: {}", key);
                logger.info("Available endpoints: {}", registeredEndpoints.keySet());
                return createSimpleResponseWithPrefix(namespace, operationName, "1", "service_check", false);
            }

            // Execute REST call
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);

            // Check if service number exists
            String checkResult = checkServiceNumberExists(result);
            logger.info("✅ Service check result: {} for category {} (0=found, 1=not found)", checkResult, category);

            return createSimpleResponseWithPrefix(namespace, operationName, checkResult, "service_check", false);

        } catch (Exception e) {
            logger.error("❌ Error processing service check request {}: {}", operationName, e.getMessage(), e);
            return createSimpleResponseWithPrefix(namespace, operationName, "1", "service_check", false);
        }
    }

    @SuppressWarnings("unchecked")
    private String checkServiceNumberExists(Map<String, Object> result) {
        try {
            logger.info("🔍 Checking if service number exists in API response");

            // Navigate through the result structure
            if (result.containsKey("call_1")) {
                Map<String, Object> call1 = (Map<String, Object>) result.get("call_1");

                // Check status code first
                if (call1.containsKey("statusCode")) {
                    Integer statusCode = (Integer) call1.get("statusCode");
                    logger.info("🔍 API response status code: {}", statusCode);

                    if (statusCode != 200) {
                        logger.warn("⚠️ API returned non-200 status: {}", statusCode);
                        return "1"; // API error = service not found
                    }
                }

                if (call1.containsKey("body")) {
                    String jsonBody = (String) call1.get("body");
                    logger.info("🔍 API response body length: {}", jsonBody != null ? jsonBody.length() : "NULL");

                    if (jsonBody == null || jsonBody.trim().isEmpty()) {
                        logger.warn("⚠️ API response body is null or empty");
                        return "1"; // Empty response = service not found
                    }

                    // Parse JSON
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonData = objectMapper.readValue(jsonBody, Map.class);
                    logger.info("🔍 Parsed JSON keys: {}", jsonData.keySet());

                    // Check if items array exists and has data
                    if (jsonData.containsKey("items") && jsonData.get("items") instanceof java.util.List) {
                        java.util.List<?> items = (java.util.List<?>) jsonData.get("items");
                        logger.info("🔍 Items array size: {}", items.size());

                        if (!items.isEmpty()) {
                            // Items exist = service number found
                            logger.info("✅ Service number exists - items array has {} entries", items.size());
                            return "0"; // Found
                        } else {
                            // Items array is empty = service number not found
                            logger.info("⚠️ Service number not found - items array is empty");
                            return "1"; // Not found
                        }
                    } else {
                        logger.warn("⚠️ No 'items' key found or it's not a List. Available keys: {}", jsonData.keySet());
                        return "1"; // Invalid structure = service not found
                    }
                } else {
                    logger.warn("⚠️ No 'body' key found in call_1 result");
                    return "1"; // No body = service not found
                }
            } else {
                logger.warn("⚠️ No 'call_1' key found in result. Available keys: {}", result.keySet());
                return "1"; // No call result = service not found
            }

        } catch (Exception e) {
            logger.error("❌ Error checking service number existence: {}", e.getMessage(), e);
            return "1"; // Any error = service not found
        }
    }

    // ============= PAYSLIP PROCESSING LOGIC (UPDATED WITH PREFIX) =============

    private Element handlePayslipRequestWithCategory(Element request, String namespace, String operationName, String jsonKey, String category) {
        try {
            logger.info("🔍 Processing payslip request with auto-category: operation={}, jsonKey={}, category={}", operationName, jsonKey, category);

            // Extract parameters (only serviceNumber expected)
            Map<String, Object> parameters = extractParametersDynamically(request);
            logger.info("🔍 Extracted parameters from request: {}", parameters);

            if (!parameters.containsKey("serviceNumber")) {
                logger.error("❌ Missing required parameter: serviceNumber");
                return createSimpleResponseWithPrefix(namespace, operationName, "-1", jsonKey, false);
            }

            // Add category automatically based on endpoint type
            parameters.put("category", category);
            logger.info("🔍 Added auto-category: {}. Final parameters: {}", category, parameters);

            // Find registered payslip endpoint
            String key = namespace + "#" + operationName;
            SoapEndpoint endpoint = registeredEndpoints.get(key);

            if (endpoint == null) {
                logger.error("❌ No registered endpoint found for: {}", key);
                logger.info("Available endpoints: {}", registeredEndpoints.keySet());
                return createSimpleResponseWithPrefix(namespace, operationName, "-1", jsonKey, false);
            }

            // Execute REST call
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);

            // Extract specific value from result
            String value = extractPayslipValue(result, jsonKey);
            logger.info("✅ Extracted {} value: {} for category {}", jsonKey, value, category);

            return createSimpleResponseWithPrefix(namespace, operationName, value, jsonKey, false);

        } catch (Exception e) {
            logger.error("❌ Error processing payslip request with category {}: {}", operationName, e.getMessage(), e);
            return createSimpleResponseWithPrefix(namespace, operationName, "-1", jsonKey, false);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractPayslipValue(Map<String, Object> result, String jsonKey) {
        try {
            // Navigate through the result structure to find the value
            if (result.containsKey("call_1")) {
                Map<String, Object> call1 = (Map<String, Object>) result.get("call_1");
                if (call1.containsKey("body")) {
                    String jsonBody = (String) call1.get("body");

                    // Parse JSON
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonData = objectMapper.readValue(jsonBody, Map.class);

                    // Check if items array exists and has data
                    if (jsonData.containsKey("items") && jsonData.get("items") instanceof java.util.List) {
                        java.util.List<?> items = (java.util.List<?>) jsonData.get("items");

                        if (!items.isEmpty() && items.get(0) instanceof Map) {
                            Map<String, Object> item = (Map<String, Object>) items.get(0);

                            // Get the specific value
                            if (item.containsKey(jsonKey)) {
                                Object value = item.get(jsonKey);
                                if (value != null) {
                                    logger.info("🔍 Successfully found {} = {} in JSON response", jsonKey, value);
                                    return String.valueOf(value);
                                } else {
                                    logger.warn("⚠️ Found {} key but value is null", jsonKey);
                                }
                            } else {
                                logger.warn("⚠️ JSON item does not contain key: {}. Available keys: {}", jsonKey, item.keySet());
                            }
                        } else {
                            logger.warn("⚠️ Items array is empty or first item is not a Map. Items size: {}", items.size());
                        }
                    } else {
                        logger.warn("⚠️ No 'items' key found or it's not a List. Available keys: {}", jsonData.keySet());
                    }
                } else {
                    logger.warn("⚠️ No 'body' key found in call_1 result");
                }
            } else {
                logger.warn("⚠️ No 'call_1' key found in result. Available keys: {}", result.keySet());
            }

            logger.warn("⚠️ Could not find {} in result structure", jsonKey);
            return "-1";

        } catch (Exception e) {
            logger.error("❌ Error extracting payslip value for {}: {}", jsonKey, e.getMessage(), e);
            return "-1";
        }
    }

    // ============= LEGACY createSimpleResponse FOR COMPLEX RESPONSES (NO PREFIX) =============

    /**
     * Legacy method for backward compatibility (no prefix)
     * Used by personnel and rank history endpoints that return complex responses
     */
    private Element createSimpleResponse(String namespace, String operationName, String returnValue) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Create response element: <OperationResponse xmlns="namespace">
            Element response = doc.createElementNS(namespace, operationName + "Response");
            doc.appendChild(response);

            // Create return element: <return>value</return>
            Element returnElement = doc.createElement("return");
            returnElement.setTextContent(returnValue);
            response.appendChild(returnElement);

            logger.info("✅ Created simple response (no prefix) for {}: {}", operationName, returnValue);
            return response;

        } catch (Exception e) {
            logger.error("❌ Error creating simple response: {}", e.getMessage(), e);
            // Return minimal error response
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();

                Element response = doc.createElementNS(namespace, operationName + "Response");
                doc.appendChild(response);

                Element returnElement = doc.createElement("return");
                returnElement.setTextContent("-1");
                response.appendChild(returnElement);

                return response;
            } catch (Exception ex) {
                logger.error("❌ Critical error creating error response: {}", ex.getMessage());
                throw new RuntimeException("Failed to create SOAP response", ex);
            }
        }
    }

    // ============= EXISTING DYNAMIC HANDLERS =============

    private Element handleDynamicRequest(Element request, String namespace, String operationName) throws Exception {
        String key = namespace + "#" + operationName;
        SoapEndpoint endpoint = registeredEndpoints.get(key);

        if (endpoint == null) {
            logger.error("❌ No registered endpoint found for: {}", key);
            return createErrorResponse(namespace, "Endpoint not found: " + operationName);
        }

        try {
            Map<String, Object> parameters = extractParametersDynamically(request);
            Map<String, Object> result = executionEngineService.executeEndpoint(endpoint, parameters);
            return createSoapResponse(namespace, operationName, result);
        } catch (Exception e) {
            logger.error("❌ Error executing endpoint {}: {}", operationName, e.getMessage(), e);
            return createErrorResponse(namespace, "Internal server error: " + e.getMessage());
        }
    }

    private Map<String, Object> extractParametersDynamically(Element request) {
        Map<String, Object> parameters = new ConcurrentHashMap<>();

        for (int i = 0; i < request.getChildNodes().getLength(); i++) {
            Node child = request.getChildNodes().item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String paramName = child.getLocalName();
                String paramValue = child.getTextContent();
                parameters.put(paramName, paramValue);
                logger.debug("🔍 Extracted param: {} = {}", paramName, paramValue);
            }
        }

        return parameters;
    }

    // Existing methods for complex responses...
    private Element createSoapResponse(String namespace, String operationName, Map<String, Object> result) throws Exception {
        // Implementation for complex responses (personnel, rank history, etc.)
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element response = doc.createElementNS(namespace, operationName + "Response");
        doc.appendChild(response);

        Element statusElem = doc.createElement("status");
        statusElem.setTextContent(result.getOrDefault("status", "200").toString());
        response.appendChild(statusElem);

        Element messageElem = doc.createElement("message");
        messageElem.setTextContent(result.getOrDefault("message", "Request processed successfully").toString());
        response.appendChild(messageElem);

        addDataElementsRecursively(doc, response, result);
        return response;
    }

    @SuppressWarnings("unchecked")
    private void addDataElementsRecursively(Document doc, Element parent, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if ("status".equals(entry.getKey()) || "message".equals(entry.getKey())) {
                continue;
            }

            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Element nestedElement = doc.createElement(key);
                addDataElementsRecursively(doc, nestedElement, (Map<String, Object>) value);
                parent.appendChild(nestedElement);
            } else if (value instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) value;
                Element arrayElement = doc.createElement(key);

                for (Object item : list) {
                    if (item instanceof Map) {
                        Element itemElement = doc.createElement(key.replaceAll("s$", ""));
                        addDataElementsRecursively(doc, itemElement, (Map<String, Object>) item);
                        arrayElement.appendChild(itemElement);
                    } else {
                        Element itemElement = doc.createElement("item");
                        itemElement.setTextContent(String.valueOf(item));
                        arrayElement.appendChild(itemElement);
                    }
                }
                parent.appendChild(arrayElement);
            } else {
                Element simpleElement = doc.createElement(key);
                simpleElement.setTextContent(String.valueOf(value));
                parent.appendChild(simpleElement);
            }
        }
    }

    private Element createErrorResponse(String namespace, String errorMessage) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element fault = doc.createElementNS(namespace, "Fault");
        doc.appendChild(fault);

        Element faultCode = doc.createElement("faultCode");
        faultCode.setTextContent("Server");
        fault.appendChild(faultCode);

        Element faultString = doc.createElement("faultString");
        faultString.setTextContent(errorMessage);
        fault.appendChild(faultString);

        return fault;
    }
}