package org.palladiosimulator.mosaic.scheduler.resources.active.cfs.test;

import static org.mockito.Mockito.mockitoSession;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.palladiosimulator.mosaic.scheduler.resources.active.SimContainerComputeResource;

import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.resources.active.IResourceTableManager;
import de.uka.ipd.sdq.simucomframework.ResourceRegistry;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.resources.SimulatedResourceContainer;
import de.uka.ipd.sdq.simucomframework.variables.cache.StoExCache;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimEngineFactory;

public class TestSimContainerComputeResource {
	
	static {
		de.uka.ipd.sdq.probfunction.math.IProbabilityFunctionFactory probFunctionFactory = de.uka.ipd.sdq.probfunction.math.impl.ProbabilityFunctionFactoryImpl.getInstance();
		
		probFunctionFactory.setRandomGenerator(new de.uka.ipd.sdq.probfunction.math.impl.DefaultRandomGenerator());
		StoExCache.initialiseStoExCache(probFunctionFactory);
	}
	
	@Test
	public void testInitScheduler() {
		SimuComModel model = Mockito.mock(SimuComModel.class);
		//Sim mocks
		Mockito.when(model.getSimEngineFactory()).thenReturn(Mockito.mock(ISimEngineFactory.class));
		
		//Resource mocks
		ResourceRegistry resReg = Mockito.mock(ResourceRegistry.class);
		Mockito.when(model.getResourceRegistry()).thenReturn(resReg );
		List<SimulatedResourceContainer> simResConList = new ArrayList<>();
		var rootContainer = Mockito.mock(SimulatedResourceContainer.class);
		simResConList.add(rootContainer);
		Mockito.when(resReg.getSimulatedResourceContainers()).thenReturn(simResConList );
		
		
		IResourceTableManager resourceTableManager = Mockito.mock(IResourceTableManager.class);
		var resource = new SimContainerComputeResource(model, 1, "testName", "testId", resourceTableManager );
		
		resource.start();
		Mockito.verify(rootContainer).getNestedResourceContainers();
		
		
		
		
		
	}
		
	
	
	

}
