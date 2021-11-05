package org.palladiosimulator.mosaic.scheduler.resources.active;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.palladiosimulator.mosaic.scheduler.resources.TaskObserver;
import org.palladiosimulator.mosaic.scheduler.resources.active.cfs.SimCGroup;
import org.palladiosimulator.mosaic.scheduler.resources.active.cfs.SimFairGroupScheduler;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.LoggingWrapper;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.entities.SchedulerEntity;
import de.uka.ipd.sdq.scheduler.resources.active.AbstractActiveResource;
import de.uka.ipd.sdq.scheduler.resources.active.IResourceTableManager;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.resources.ScheduledResource;
import de.uka.ipd.sdq.simulation.abstractsimengine.AbstractSimEventDelegator;

public class SimContainerComputeResource extends AbstractActiveResource implements TaskObserver
{
	
	
	
	private SimFairGroupScheduler scheduler;
	private SimCGroup rootGroup;
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
        	
        	
        	container.getNestedResourceContainers().forEach(nested -> {
        		SimCGroup group = new SimCGroup(this);
        		var nestedCPUResource = nested.getActiveResources().stream().filter(nestedResource -> nestedResource.getResourceTypeId().equals(CPU_RESOUREC_TYPE_ID)).map(ScheduledResource.class::cast).findFirst();
        		nestedCPUResource.ifPresent(nestedScheduledResource -> {
	        		var nestedActiveResource = nestedScheduledResource.getActiveResource();
					var nestedRate = nestedActiveResource.getProcessingRate_ProcessingResourceSpecification().getSpecification();
	   		
	        		group.setRate(nestedRate);
	        		scheduler.addGroup(group, nested.getResourceContainerID());
        		});
        		
        	});
        });
        
	}




	private void toNow() {
		final double now = this.getModel().getSimulationControl().getCurrentSimulationTime();
		double passed_time = now - last_time;
		double processedDemand = passed_time * scheduler.getProcessingRate();
		
		scheduler.grantDemand(processedDemand);
		

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

		rootGroup = new SimCGroup(this);
		rootGroup.setRate("100");
		scheduler.addGroup(rootGroup, this.getId());
		
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
		if (group == null)// TODO this does not work as it is the wrong id
			groupId = this.getId();

		// schedule demand
		scheduler.enqueueProcessDemand(groupId, process, demand);

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
