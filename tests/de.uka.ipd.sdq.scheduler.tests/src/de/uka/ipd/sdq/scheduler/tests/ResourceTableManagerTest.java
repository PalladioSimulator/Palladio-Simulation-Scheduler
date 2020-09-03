package de.uka.ipd.sdq.scheduler.tests;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.resources.active.AbstractActiveResource;
import de.uka.ipd.sdq.scheduler.resources.active.ResourceTableManager;

public class ResourceTableManagerTest {

    private ResourceTableManager resourceTableManager;
    
    @Mock private ISchedulableProcess process;
    @Mock private AbstractActiveResource resource;
    
    
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        resourceTableManager = new ResourceTableManager();
    }

    @Test
    public void testSetProcessForResourceFirstTime() {
        resourceTableManager.setLastResource(process, resource);
        
        verify(process, times(1)).addTerminatedObserver(resource);
        
        AbstractActiveResource result = resourceTableManager.getLastResource(process);
        assertNotNull("Failed to set process for resource", result);
    }
    
    @Test
    public void testSetProcessForResourceSecondTimeWithoutObserverReRegistration() throws Exception {
        resourceTableManager.setLastResource(process, resource);
        resourceTableManager.setLastResource(process, resource);

        verify(process, times(1)).addTerminatedObserver(resource);
        
        AbstractActiveResource result = resourceTableManager.getLastResource(process);
        assertNotNull("Failed to set process for resource a second time", result);
        
    }

}
