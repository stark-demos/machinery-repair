package com.stark.machinery_repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
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
        parameters.put("repairRequest", "");
        parameters.put("partCode", "");
        parameters.put("quantity", 0L);
        parameters.put("wsJsonRequest", "");
        parameters.put("getInventoryUrl", "inventoryUrl");
        parameters.put("assignPartsUrl", "assignPartsUrl");
        parameters.put("createPurchaseOrderUrl", "");

        ProcessInstance processInstance = ksession.startProcess("machinery-repair.parts-procurement_v1_0", parameters);
        Long processInstanceId = processInstance.getId();
        assertProcessInstanceActive(processInstanceId);

        // check that Inventory has been Requested
        WorkItem workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("inventoryUrl", workItem.getParameter("Url"));
        assertEquals("GET", workItem.getParameter("Method"));

        Map<String, Object> workItemResult = new HashMap<>();
        workItemResult.put("Result", "GET INVENTORY RESPONSE");

        // complete the work item
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        assertEquals("GET INVENTORY RESPONSE", getVariableValue("wsJsonResponse", processInstanceId, ksession));
        assertNodeActive(processInstanceId, ksession, "Assign parts to Repair Request");

        // Assign parts work item
        workItem = testHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("Rest", workItem.getName());
        assertEquals("assignPartsUrl", workItem.getParameter("Url"));
        assertEquals("POST", workItem.getParameter("Method"));
        
        workItemResult.clear();
        workItemResult.put("Result", "ASSIGN PARTS RESPONSE");
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), workItemResult);
        
        assertProcessInstanceCompleted(processInstanceId);
        disposeRuntimeManager();
        logger.debug("END testAssignPartsToRepairRequestSuccees");
    }
}
