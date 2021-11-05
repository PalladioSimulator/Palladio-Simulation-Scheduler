package org.palladiosimulator.mosaic.scheduler.resources.active.cfs.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.palladiosimulator.mosaic.scheduler.resources.TaskObserver;
import org.palladiosimulator.mosaic.scheduler.resources.active.cfs.SimCGroup;
import org.palladiosimulator.mosaic.scheduler.resources.active.cfs.SimFairGroupScheduler;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.simucomframework.variables.cache.StoExCache;

public class TestCFSScheduler {
	
	static {
		de.uka.ipd.sdq.probfunction.math.IProbabilityFunctionFactory probFunctionFactory = de.uka.ipd.sdq.probfunction.math.impl.ProbabilityFunctionFactoryImpl.getInstance();
		
		probFunctionFactory.setRandomGenerator(new de.uka.ipd.sdq.probfunction.math.impl.DefaultRandomGenerator());
		StoExCache.initialiseStoExCache(probFunctionFactory);
	}
	
	
	@Test
	public void testCGroup() {
		var cgroup = new SimCGroup(Mockito.mock(TaskObserver.class));
		var p1 = Mockito.mock(ISchedulableProcess.class);
		cgroup.addTask(p1, 10.0);
		var p2 = Mockito.mock(ISchedulableProcess.class);
		cgroup.addTask(p2, 20.0);
		var p3 = Mockito.mock(ISchedulableProcess.class);
		cgroup.addTask(p3, 30.0);
		
		assertEquals(3, cgroup.size());
		assertEquals(60.0, cgroup.getRemainingDemand());
		assertEquals(10.0, cgroup.getMinDemand().orElse(0.0));
		
		cgroup.grantDemand(15.0);
		assertEquals(3, cgroup.size());
		assertEquals(45.0, cgroup.getRemainingDemand());
		assertEquals(5.0, cgroup.getMinDemand().orElse(0.0));
		
		cgroup.grantDemand(15.0);
		Mockito.verify(p1).activate();
		assertEquals(2, cgroup.size());
		assertEquals(30.0, cgroup.getRemainingDemand());
		assertEquals(10.0, cgroup.getMinDemand().orElse(0.0));
		
		cgroup.grantDemand(25.0);
		Mockito.verify(p2).activate();
		assertEquals(1, cgroup.size());
		assertEquals(5.0, cgroup.getRemainingDemand());
		assertEquals(5.0, cgroup.getMinDemand().orElse(0.0));
	}
	
	
	@Test
	public void testGroupScheduler() {
		
		var groupScheduler = new SimFairGroupScheduler();
		groupScheduler.setProcessingRate(10.0);
		
		var cgroup1 = new SimCGroup(Mockito.mock(TaskObserver.class));
		cgroup1.setRate("1.0");
		var p1 = Mockito.mock(ISchedulableProcess.class);
		groupScheduler.enqueueProcessDemand(cgroup1, p1, 100.0);

		var cgroup2 = new SimCGroup(Mockito.mock(TaskObserver.class));
		cgroup2.setRate("4.0");
		var p2 = Mockito.mock(ISchedulableProcess.class);
		groupScheduler.enqueueProcessDemand(cgroup2, p2, 100.0);		

		assertEquals(12.5, groupScheduler.getNextSchedule());
		
		//grant 100 units to the scheduler
		groupScheduler.grantDemand(100.0);
		
		assertEquals(80.0, cgroup1.getRemainingDemand());
		assertEquals(20.0, cgroup2.getRemainingDemand());
		
		assertEquals(2.5, groupScheduler.getNextSchedule());
		
		//grant 25 units to the scheduler
		groupScheduler.grantDemand(25.0);
		Mockito.verify(p2).activate();
		assertEquals(75.0, cgroup1.getRemainingDemand());
		assertEquals(0.0, cgroup2.getRemainingDemand());
		
		assertEquals(7.5, groupScheduler.getNextSchedule());
		
		
	}
	
	
	

}
