package org.palladiosimulator.mosaic.scheduler.resources.active;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.palladiosimulator.mosaic.scheduler.resources.TaskObserver;
import org.palladiosimulator.mosaic.scheduler.resources.active.cfs.SimLeafCGroup;
import org.palladiosimulator.mosaic.scheduler.resources.active.cfs.ISimCGroup;
import org.palladiosimulator.mosaic.scheduler.resources.active.cfs.SimFairGroupScheduler;
import org.palladiosimulator.mosaic.scheduler.resources.active.cfs.SimInnerCGroup;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.LoggingWrapper;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.entities.SchedulerEntity;
import de.uka.ipd.sdq.scheduler.resources.active.AbstractActiveResource;
import de.uka.ipd.sdq.scheduler.resources.active.IResourceTableManager;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.resources.ScheduledResource;
import de.uka.ipd.sdq.simucomframework.resources.SimulatedResourceContainer;
import de.uka.ipd.sdq.simulation.abstractsimengine.AbstractSimEventDelegator;

public class SimContainerComputeResource extends AbstractActiveResource implements TaskObserver
{
	
	
	
	private SimFairGroupScheduler scheduler;
	private SimLeafCGroup rootGroup;
	final static int QUOTA_PERIOD_MSEC = 100;
    protected static final String CPU_RESOUREC_TYPE_ID = "_oro4gG3fEdy4YaaT-RYrLQ";

    private final TaskFinishedEvent taskFinished;

    private double last_time;

	
    private class TaskFinishedEvent extends AbstractSimEventDelegator<SimContainerComputeResource> {
    	
    	public TaskFinishedEvent(final SchedulerModel model) {
    		super(model, TaskFinishedEvent.class.getName());
    		

    	}
    	
    	@Override
    	public void eventRoutine(final SimContainerComputeResource resource) {
    		
    		toNow();
//    		running_processes.remove(last);
//    		
//    		reportCoreUsage();
//    		
//    		fireDemandCompleted(last);
    		LoggingWrapper.log(resource + " finished task.");
    		scheduleNextEvent();
//    		last.activate();
    	}
    	
    }
    
	
	public SimContainerComputeResource(SchedulerModel model, long capacity, String name, String id,
			IResourceTableManager resourceTableManager) {
		super(model, capacity, name, id, resourceTableManager);

		this.taskFinished = new TaskFinishedEvent(this.getModel());
	}

	

	private void initScheduler() {
		
		scheduler = new SimFairGroupScheduler();
		
		// find child containers
        var simuComModel = (SimuComModel)this.getModel();
        
        
        var simContainer = simuComModel.getResourceRegistry().getSimulatedResourceContainers().stream()
        	.filter(c -> c.getActiveResources().stream()
        			.anyMatch(r -> r.getUnderlyingResource() ==  this))
        	.findFirst();
		
        simContainer.ifPresent(container -> {
        	var activeResource = container.getActiveResources().stream().filter(r -> r.getUnderlyingResource() == this).findFirst();
        	var scheduledResource = (ScheduledResource) activeResource.get();
        	var rate = scheduledResource.getActiveResource().getProcessingRate_ProcessingResourceSpecification().getSpecification();
        	
        	scheduler.setProcessingRate(Double.parseDouble(rate)*scheduledResource.getNumberOfInstances());
        	
        	
        	var nestedGroups = createOrUpdateNestedCGroup(container);
        	nestedGroups.forEach((nestedId, group) -> {
        		scheduler.addGroup(group, nestedId, true);
        	});
        });
        
	}
	
	
	
	
	@Override
	public void notifyResourceChanged() {
		updateScheduler();
	}



	public void updateScheduler() {
		
		// find child containers
        var simuComModel = (SimuComModel)this.getModel();
        
        
        var simContainer = simuComModel.getResourceRegistry().getSimulatedResourceContainers().stream()
        	.filter(c -> c.getActiveResources().stream()
        			.anyMatch(r -> r.getUnderlyingResource() ==  this))
        	.findFirst();
		
        simContainer.ifPresent(container -> {
        	var activeResource = container.getActiveResources().stream().filter(r -> r.getUnderlyingResource() == this).findFirst();
        	var scheduledResource = (ScheduledResource) activeResource.get();
        	var rate = scheduledResource.getActiveResource().getProcessingRate_ProcessingResourceSpecification().getSpecification();
        	//Update ProcessingRate
        	//TODO: Evaluate static...
        	scheduler.setProcessingRate(Double.parseDouble(rate)*scheduledResource.getNumberOfInstances());
        	
        	
        	var nestedGroups = createOrUpdateNestedCGroup(container);
        	nestedGroups.forEach((nestedId, group) -> {
        		if(!scheduler.hasGroup(nestedId))
        			scheduler.addGroup(group, nestedId, true);
        	});
        });
        
	}



	public Map<String, ISimCGroup> createOrUpdateNestedCGroup(SimulatedResourceContainer container) {
		var retMap = new HashMap<String, ISimCGroup>();
		container.getNestedResourceContainers().forEach(nested -> {
			ISimCGroup group;
			if (scheduler.hasGroup(nested.getResourceContainerID())) {
				//Lookup existing groups from scheduler
				group = scheduler.getGroup(nested.getResourceContainerID());
			} else if(nested.getNestedResourceContainers().isEmpty()) {
				group = new SimLeafCGroup(this);
			} else {
				group = new SimInnerCGroup(null);
			}
			
			
			if(group instanceof SimInnerCGroup) {
				var subGroups = createOrUpdateNestedCGroup(nested);
				subGroups.forEach((nestedId, sgroup) -> {
	        		scheduler.addGroup(sgroup, nestedId, false);
	        		((SimInnerCGroup) group).addSubGroup(sgroup);
	        	});
			}
			
			
			// Update rate anyhow
			var nestedCPUResource = nested.getActiveResources().stream().filter(nestedResource -> nestedResource.getResourceTypeId().equals(CPU_RESOUREC_TYPE_ID)).map(ScheduledResource.class::cast).findFirst();
			nestedCPUResource.ifPresent(nestedScheduledResource -> {
				var nestedActiveResource = nestedScheduledResource.getActiveResource();
				var nestedRate = nestedActiveResource.getProcessingRate_ProcessingResourceSpecification().getSpecification();
		
				group.setRate(nestedRate);
				retMap.put(nested.getResourceContainerID(), group);
	
			});
			
		});
		return retMap;
	}




	private void toNow() {
		final double now = this.getModel().getSimulationControl().getCurrentSimulationTime();
		Double passed_time = now - last_time;
		double processedDemand = passed_time * scheduler.getProcessingRate();
		
		scheduler.grantDemand(processedDemand, passed_time.longValue() * 1000);
		

		last_time = now;
	}
	
	public void scheduleNextEvent() {
		var nextEventTime = scheduler.getNextSchedule();
		
		taskFinished.removeEvent();
		if (nextEventTime > 0.0) {
			
			taskFinished.schedule(this, nextEventTime);
		}
	}

	@Override
	public double getRemainingDemand(ISchedulableProcess process) {
		throw new UnsupportedOperationException("getRemainingDemand() not yet supported!");
	}

	@Override
	public void updateDemand(ISchedulableProcess process, double demand) {
		throw new UnsupportedOperationException("updateDemand() not yet supported!");

	}

	@Override
	public void start() {
		initScheduler();

		rootGroup = new SimLeafCGroup(this);
		rootGroup.setRate("100");
		scheduler.addGroup(rootGroup, this.getId(), true);
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerProcess(ISchedulableProcess runningProcess) {
		
		// var rootProcess = runningProcess.getRootProcess();
		
		// TODO Auto-generated method stub

	}

	@Override
	public int getQueueLengthFor(SchedulerEntity schedulerEntity, int coreID) {
		
		return scheduler.getActiveProcessSize();
	}
	
	

	@Override
	protected void doProcessing(ISchedulableProcess process, int resourceServiceID,
			Map<String, Serializable> parameterMap, double demand) {

		toNow();
		// extract CGroup
		var group = rootGroup;

		String groupId = (String) parameterMap.get("__MOSAIC_CONTAINER_ID");
		if (groupId == null)// TODO this does not work as it is the wrong id
			groupId = this.getId();
		
		Double containerDemand = (Double) parameterMap.get("__MOSAIC_CONTAINER_DEMAND");
		
		// schedule demand
		scheduler.enqueueProcessDemand(groupId, process, containerDemand);

		scheduleNextEvent();
		process.passivate();

	}





	@Override
	protected void doProcessing(ISchedulableProcess process, int resourceServiceID, double demand) {
		this.doProcessing(process, resourceServiceID, Collections.emptyMap(), demand);

	}

	@Override
	protected void enqueue(ISchedulableProcess process) {
		// Only called when 

	}

	@Override
	protected void dequeue(ISchedulableProcess process) {
		// TODO Auto-generated method stub

	}



	@Override
	public void fireTaskFinished(ISchedulableProcess process) {
		this.fireDemandCompleted(process);
		
	}

}
