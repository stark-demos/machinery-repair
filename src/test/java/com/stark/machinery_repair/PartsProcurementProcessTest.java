package com.stark.machinery_repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartsProcurementProcessTest extends JbpmJUnitBaseTestCase {
    private static Logger logger = LoggerFactory.getLogger(PartsProcurementProcessTest.class);

    private static final String PROCESS_ID = "machinery-repair.parts-procurement_v2_0";

    public PartsProcurementProcessTest() {
        super(true, true);
    }

    @Test
    public void testAssignPartsToRepairRequestSuccees() {
        logger.debug("START testAssignPartsToRepairRequestSuccees");
        createRuntimeManager("com/stark/machinery_repair/parts-procurement.bpmn");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TestWorkItemHandler testHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Rest", testHandler);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("partsAvailable", Boolean.TRUE);
        parameters.put("repairRequestId", "RR-1");
        parameters.put("branchCode", "BC-1");
        parameters.put("partCode", "A");
        parameters.put("quantity", 1);
        parameters.put("wsJsonRequest", "");
        parameters.put("getInventoryUrl", "inventoryUrl");
        parameters.put("assignPartsUrl", "assignPartsUrl");
        parameters.put("createPurchaseOrderUrl", "");

        ProcessInstance processInstance = ksession.startProcess(PROCESS_ID, parameters);
        Long processInstanceId = processInstance.getId();
        assertProcessInstanceActive(processInstanceId);

        // check that Inventory has been Requested
        WorkItem workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("inventoryUrl", workItem.getParameter("Url"));
        assertEquals("GET", workItem.getParameter("Method"));

        String expectedWsRequest = "{\"partCode\":\"A\"}";
        assertEquals(expectedWsRequest, getVariableValue("wsJsonRequest", processInstanceId, ksession));

        Map<String, Object> workItemResult = new HashMap<>();
        String wsResult = "{\"partCode\":\"A\", \"availableQuantity\":20}";
        workItemResult.put("Result", wsResult);

        // complete the work item
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        assertEquals(wsResult, getVariableValue("wsJsonResponse", processInstanceId, ksession));
        assertNodeActive(processInstanceId, ksession, "Assign parts to Repair Request");
        assertEquals(true, getVariableValue("partsAvailable", processInstanceId, ksession));

        // Assign parts work item
        workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("assignPartsUrl", workItem.getParameter("Url"));
        assertEquals("POST", workItem.getParameter("Method"));

        expectedWsRequest = "{\"partCode\":\"A\",\"quantity\":1,\"repairRequestId\":\"RR-1\",\"branchCode\":\"BC-1\"}";
        assertEquals(expectedWsRequest, getVariableValue("wsJsonRequest", processInstanceId, ksession));

        workItemResult.clear();
        workItemResult.put("Result", "{\"reservationId\": \"abc-ABC-123\", \"remainingParts\": 0}");
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);

        assertProcessInstanceCompleted(processInstanceId);
        disposeRuntimeManager();
        logger.debug("END testAssignPartsToRepairRequestSuccees");
    }

    @Test
    public void testNoAssignPartsToRepairRequest() {
        logger.debug("START testNoAssignPartsToRepairRequest");
        createRuntimeManager("com/stark/machinery_repair/parts-procurement.bpmn");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TestWorkItemHandler testHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Rest", testHandler);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("partsAvailable", Boolean.TRUE);
        parameters.put("repairRequestId", "RR-1");
        parameters.put("branchCode", "BC-1");
        parameters.put("partCode", "A");
        parameters.put("quantity", 1);
        parameters.put("wsJsonRequest", "");
        parameters.put("getInventoryUrl", "inventoryUrl");
        parameters.put("assignPartsUrl", "assignPartsUrl");
        parameters.put("createPurchaseOrderUrl", "");

        ProcessInstance processInstance = ksession.startProcess(PROCESS_ID, parameters);
        Long processInstanceId = processInstance.getId();
        assertProcessInstanceActive(processInstanceId);

        // check that Inventory has been Requested
        WorkItem workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("inventoryUrl", workItem.getParameter("Url"));
        assertEquals("GET", workItem.getParameter("Method"));

        String expectedWsRequest = "{\"partCode\":\"A\"}";
        assertEquals(expectedWsRequest, getVariableValue("wsJsonRequest", processInstanceId, ksession));

        Map<String, Object> workItemResult = new HashMap<>();
        String wsResult = "{\"partCode\":\"A\", \"availableQuantity\":20}";
        workItemResult.put("Result", wsResult);

        // complete the work item
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        assertEquals(wsResult, getVariableValue("wsJsonResponse", processInstanceId, ksession));
        assertNodeActive(processInstanceId, ksession, "Assign parts to Repair Request");
        assertEquals(true, getVariableValue("partsAvailable", processInstanceId, ksession));

        // Assign parts work item
        workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("assignPartsUrl", workItem.getParameter("Url"));
        assertEquals("POST", workItem.getParameter("Method"));

        workItemResult.clear();
        workItemResult.put("Result", "{\"responseCode\": \"NOK\", \"Message\": \"No parts available\"}");
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);

        // Looped back to restart link.
        completeHappyPath(processInstanceId, "A", 1, ksession, testHandler);
    }

    @Test
    public void testFailAssignPartsToRepairRequest() {
        logger.debug("START testFailAssignPartsToRepairRequest");
        createRuntimeManager("com/stark/machinery_repair/parts-procurement.bpmn");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TestWorkItemHandler testHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Rest", testHandler);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("partsAvailable", Boolean.TRUE);
        parameters.put("repairRequestId", "RR-1");
        parameters.put("branchCode", "BC-1");
        parameters.put("partCode", "A");
        parameters.put("quantity", 1);
        parameters.put("wsJsonRequest", "");
        parameters.put("getInventoryUrl", "inventoryUrl");
        parameters.put("assignPartsUrl", "assignPartsUrl");
        parameters.put("createPurchaseOrderUrl", "");

        ProcessInstance processInstance = ksession.startProcess(PROCESS_ID, parameters);
        Long processInstanceId = processInstance.getId();
        assertProcessInstanceActive(processInstanceId);

        // check that Inventory has been Requested
        WorkItem workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("inventoryUrl", workItem.getParameter("Url"));
        assertEquals("GET", workItem.getParameter("Method"));

        String expectedWsRequest = "{\"partCode\":\"A\"}";
        assertEquals(expectedWsRequest, getVariableValue("wsJsonRequest", processInstanceId, ksession));

        Map<String, Object> workItemResult = new HashMap<>();
        String wsResult = "{\"partCode\":\"A\", \"availableQuantity\":20}";
        workItemResult.put("Result", wsResult);

        // complete the work item
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        assertEquals(wsResult, getVariableValue("wsJsonResponse", processInstanceId, ksession));
        assertNodeActive(processInstanceId, ksession, "Assign parts to Repair Request");
        assertEquals(true, getVariableValue("partsAvailable", processInstanceId, ksession));

        // Assign parts work item
        workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("assignPartsUrl", workItem.getParameter("Url"));
        assertEquals("POST", workItem.getParameter("Method"));

        workItemResult.clear();
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);

        // Looped back to restart link.
        assertNodeActive(processInstanceId, ksession, "Fix Procurement Details");

        List<Long> tasks = runtimeEngine.getTaskService().getTasksByProcessInstanceId(processInstanceId);
        assertNotNull(tasks);
        assertTrue("Single task in collection", tasks.size() == 1);
        Long taskId = tasks.get(0);
        String userId = "Administrator";
        runtimeEngine.getTaskService().claim(taskId, userId);
        runtimeEngine.getTaskService().start(taskId, userId);

        Map<String, Object> htResults = new HashMap<>();
        htResults.put("quantity", 10);
        htResults.put("partCode", "B");
        htResults.put("branchCode", "BC-1");

        runtimeEngine.getTaskService().complete(taskId, userId, htResults);
        logger.info("Task completed");

        completeHappyPath(processInstanceId, "B", 10, ksession, testHandler);
    }

    @Test
    public void testPurchaseOrderRequested() {
        logger.debug("START testPurchaseOrderRequested");
        createRuntimeManager("com/stark/machinery_repair/parts-procurement.bpmn");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TestWorkItemHandler testHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Rest", testHandler);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("partsAvailable", Boolean.TRUE);
        parameters.put("repairRequestId", "RR-1");
        parameters.put("branchCode", "BC-1");
        parameters.put("partCode", "A");
        parameters.put("quantity", 1);
        parameters.put("wsJsonRequest", "");
        parameters.put("getInventoryUrl", "inventoryUrl");
        parameters.put("assignPartsUrl", "assignPartsUrl");
        parameters.put("createPurchaseOrderUrl", "createPurchaseOrderUrl");

        ProcessInstance processInstance = ksession.startProcess(PROCESS_ID, parameters);
        Long processInstanceId = processInstance.getId();
        assertProcessInstanceActive(processInstanceId);

        // check that Inventory has been Requested
        WorkItem workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("inventoryUrl", workItem.getParameter("Url"));
        assertEquals("GET", workItem.getParameter("Method"));

        String expectedWsRequest = "{\"partCode\":\"A\"}";
        assertEquals(expectedWsRequest, getVariableValue("wsJsonRequest", processInstanceId, ksession));

        Map<String, Object> workItemResult = new HashMap<>();
        String wsResult = "{\"partCode\":\"A\", \"availableQuantity\":0}";
        workItemResult.put("Result", wsResult);

        // complete the work item
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        assertEquals(wsResult, getVariableValue("wsJsonResponse", processInstanceId, ksession));
        assertNodeActive(processInstanceId, ksession, "Purchase Order");
        assertEquals(false, getVariableValue("partsAvailable", processInstanceId, ksession));

        workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("createPurchaseOrderUrl", workItem.getParameter("Url"));
        assertEquals("POST", workItem.getParameter("Method"));

        workItemResult.clear();
        wsResult = "{\"partCode\":\"A\", \"quantity\":0, \"purchaseOrderReceiverId\":\"ABC-123\"}";
        workItemResult.put("Result", wsResult);
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        assertNodeActive(processInstanceId, ksession, "Received Materials");

        ksession.signalEvent("receivedMaterials", null);
        completeHappyPath(processInstanceId, "A", 1, ksession, testHandler);
    }

    @Test
    public void testRequestInventoryFail() {
        logger.debug("START testRequestInventoryFail");
        createRuntimeManager("com/stark/machinery_repair/parts-procurement.bpmn");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TestWorkItemHandler testHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Rest", testHandler);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("partsAvailable", Boolean.TRUE);
        parameters.put("repairRequestId", "RR-1");
        parameters.put("branchCode", "BC-1");
        parameters.put("partCode", "A");
        parameters.put("quantity", 1);
        parameters.put("wsJsonRequest", "");
        parameters.put("getInventoryUrl", "inventoryUrl");
        parameters.put("assignPartsUrl", "assignPartsUrl");
        parameters.put("createPurchaseOrderUrl", "createPurchaseOrderUrl");

        ProcessInstance processInstance = ksession.startProcess(PROCESS_ID, parameters);
        Long processInstanceId = processInstance.getId();
        assertProcessInstanceActive(processInstanceId);

        // check that Inventory has been Requested
        WorkItem workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("inventoryUrl", workItem.getParameter("Url"));
        assertEquals("GET", workItem.getParameter("Method"));

        String expectedWsRequest = "{\"partCode\":\"A\"}";
        assertEquals(expectedWsRequest, getVariableValue("wsJsonRequest", processInstanceId, ksession));

        Map<String, Object> workItemResult = new HashMap<>();
        String wsResult = "{\"Fail\":\"NOK\"}";
        workItemResult.put("Result", wsResult);

        // complete the work item
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        assertEquals(wsResult, getVariableValue("wsJsonResponse", processInstanceId, ksession));
        assertNodeActive(processInstanceId, ksession, "Fix Procurement Details");
        assertNull(getVariableValue("partsAvailable", processInstanceId, ksession));

        List<Long> tasks = runtimeEngine.getTaskService().getTasksByProcessInstanceId(processInstanceId);
        assertNotNull(tasks);
        assertTrue("Single task in collection", tasks.size() == 1);
        Long taskId = tasks.get(0);
        String userId = "Administrator";
        runtimeEngine.getTaskService().claim(taskId, userId);
        runtimeEngine.getTaskService().start(taskId, userId);

        Map<String, Object> htResults = new HashMap<>();
        htResults.put("quantity", 10);
        htResults.put("partCode", "B");
        htResults.put("branchCode", "BC-1");

        runtimeEngine.getTaskService().complete(taskId, userId, htResults);
        logger.info("Task completed");

        completeHappyPath(processInstanceId, "B", 10, ksession, testHandler);
    }

    @Test
    public void testNoBranchCodeFail() {
        logger.debug("START testNoBranchCodeFail");
        createRuntimeManager("com/stark/machinery_repair/parts-procurement.bpmn");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TestWorkItemHandler testHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Rest", testHandler);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("partsAvailable", Boolean.TRUE);
        parameters.put("repairRequestId", "RR-1");
        parameters.put("partCode", "A");
        parameters.put("quantity", 1);
        parameters.put("wsJsonRequest", "");
        parameters.put("getInventoryUrl", "inventoryUrl");
        parameters.put("assignPartsUrl", "assignPartsUrl");
        parameters.put("createPurchaseOrderUrl", "");

        ProcessInstance processInstance = ksession.startProcess(PROCESS_ID, parameters);
        Long processInstanceId = processInstance.getId();
        assertProcessInstanceActive(processInstanceId);

        // check that Inventory has been Requested
        WorkItem workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("inventoryUrl", workItem.getParameter("Url"));
        assertEquals("GET", workItem.getParameter("Method"));

        String expectedWsRequest = "{\"partCode\":\"A\"}";
        assertEquals(expectedWsRequest, getVariableValue("wsJsonRequest", processInstanceId, ksession));

        Map<String, Object> workItemResult = new HashMap<>();
        String wsResult = "{\"partCode\":\"A\", \"availableQuantity\":20}";
        workItemResult.put("Result", wsResult);

        // complete the work item
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        assertEquals(wsResult, getVariableValue("wsJsonResponse", processInstanceId, ksession));
        assertNodeActive(processInstanceId, ksession, "Assign parts to Repair Request");
        assertEquals(true, getVariableValue("partsAvailable", processInstanceId, ksession));

        // Assign parts work item
        workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("assignPartsUrl", workItem.getParameter("Url"));
        assertEquals("POST", workItem.getParameter("Method"));

        expectedWsRequest = "{\"partCode\":\"A\",\"quantity\":1,\"repairRequestId\":\"RR-1\",\"branchCode\":\"null\"}";
        assertEquals(expectedWsRequest, getVariableValue("wsJsonRequest", processInstanceId, ksession));

        workItemResult.clear();
        workItemResult.put("Result", null);
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);

        List<Long> tasks = runtimeEngine.getTaskService().getTasksByProcessInstanceId(processInstanceId);
        assertNotNull(tasks);
        assertTrue("Single task in collection", tasks.size() == 1);
        Long taskId = tasks.get(0);
        String userId = "Administrator";
        runtimeEngine.getTaskService().claim(taskId, userId);
        runtimeEngine.getTaskService().start(taskId, userId);

        Map<String, Object> htResults = new HashMap<>();
        htResults.put("quantity", 10);
        htResults.put("partCode", "B");
        htResults.put("branchCode", "BC-1");

        runtimeEngine.getTaskService().complete(taskId, userId, htResults);
        logger.info("Task completed");

        completeHappyPath(processInstanceId, "B", 10, ksession, testHandler);
        logger.debug("END testNoBranchCodeFail");
    }

    private void completeHappyPath(Long processInstanceId, String partCode, Integer quantity, KieSession ksession, TestWorkItemHandler testHandler) {
        assertNodeActive(processInstanceId, ksession, "Request Inventory Availability");
        WorkItem workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("inventoryUrl", workItem.getParameter("Url"));
        assertEquals("GET", workItem.getParameter("Method"));

        Map<String, Object> workItemResult = new HashMap<>();
        String wsResult = "{\"partCode\":\"A\", \"availableQuantity\":20}";
        workItemResult.put("Result", wsResult);
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        assertEquals(wsResult, getVariableValue("wsJsonResponse", processInstanceId, ksession));
        assertNodeActive(processInstanceId, ksession, "Assign parts to Repair Request");
        assertEquals(true, getVariableValue("partsAvailable", processInstanceId, ksession));

        workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("assignPartsUrl", workItem.getParameter("Url"));
        assertEquals("POST", workItem.getParameter("Method"));

        String expectedWsRequest = "{\"partCode\":\"" + partCode + "\",\"quantity\":" + quantity + ",\"repairRequestId\":\"RR-1\",\"branchCode\":\"BC-1\"}";
        assertEquals(expectedWsRequest, getVariableValue("wsJsonRequest", processInstanceId, ksession));

        workItemResult.clear();
        workItemResult.put("Result", "{\"reservationId\": \"abc-ABC-123\", \"remainingParts\": 0}");
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);

        assertProcessInstanceCompleted(processInstanceId);
        disposeRuntimeManager();
        logger.debug("END testAssignPartsToRepairRequestSuccees");
    }

}
