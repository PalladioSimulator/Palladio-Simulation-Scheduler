package org.palladiosimulator.mosaic.scheduler.resources.active;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.LoggingWrapper;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.entities.SchedulerEntity;
import de.uka.ipd.sdq.scheduler.resources.active.AbstractActiveResource;
import de.uka.ipd.sdq.scheduler.resources.active.IResourceTableManager;
import de.uka.ipd.sdq.simucomframework.SimuComSimProcess;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.resources.IDemandListener;
import de.uka.ipd.sdq.simucomframework.resources.SimulatedResourceContainer;
import de.uka.ipd.sdq.simulation.abstractsimengine.AbstractSimEventDelegator;

public class SimVirtualDelegatingResource extends AbstractActiveResource {

    protected static final String CPU_INTERFACE_ID = "_tw_Q8E5CEeCUKeckjJ_n-w";
	// Contains all running processes on the resource (key: process ID)
    private final Hashtable<String, ISchedulableProcess> running_processes = new Hashtable<String, ISchedulableProcess>();
    
     

    public SimVirtualDelegatingResource(final SchedulerModel model, final String name, final String id, IResourceTableManager resourceTableManager) {
        super(model, -1l, name, id, resourceTableManager);
    }

    
    
    @Override
    public void start() {
        running_processes.clear();
    }


    @Override
    protected void dequeue(final ISchedulableProcess process) {
//        if (!running_processes.containsKey(process.getId())) {
//            return;
//        }
//        running_processes.remove(process.getId());
//        fireStateChange(running_processes.size(), 0);
//        fireDemandCompleted(process);
//        process.activate();
    }

    @Override
    protected void doProcessing(final ISchedulableProcess process, final int resourceServiceId, final double demand) {
        LoggingWrapper.log("Delay: " + process + " demands " + demand);
        if (!running_processes.containsKey(process.getId())) {
            enqueue(process);
        }
        
        // Find parent Resource
        var simuComModel = (SimuComModel)this.getModel();
        
        var simContainer = simuComModel.getResourceRegistry().getSimulatedResourceContainers().stream()
        	.filter(c -> c.getActiveResources().stream()
        			.anyMatch(r -> r.getUnderlyingResource() ==  this))
        	.findFirst();
        
        if(simContainer.isEmpty())
        	return;
        
        var container = simContainer.get();
        
        var simActiveResource = container.getActiveResources().stream().filter(r -> r.getUnderlyingResource() == this).findFirst();
        
        
//        var container = (SimulatedResourceContainer)simuComModel.getResourceRegistry().getResourceContainer(getId());
//        assert container != null;
        
        var simucomProcess = (SimuComSimProcess)process;
        
        

        //Consume demand on parent
        // Variant 1
        
        String typeId = simActiveResource.get().getResourceTypeId();
        SimulatedResourceContainer parentResourceContainer = container.getParentResourceContainer();
        var parentResource = parentResourceContainer.getAllActiveResources().get(typeId);
        
        parentResource.addDemandListener(new IDemandListener() {

			@Override
			public void demand(double demand) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void demandCompleted(ISchedulableProcess simProcess) {
				if(running_processes.containsKey(simProcess.getId())) {
					running_processes.remove(simProcess.getId());
					fireStateChange(running_processes.size(), 0);
				}
			}
        	
        });
        
        
		Map<String, Serializable> params = new HashMap<>();
		params.put("__MOSAIC_CONTAINER_ID", container.getResourceContainerID());
		//parentResourceContainer.loadActiveResource(simucomProcess, typeId, demand);
		parentResourceContainer.loadActiveResource(simucomProcess, CPU_INTERFACE_ID, 0, params , demand);
        

    }

    @Override
    public double getRemainingDemand(final ISchedulableProcess process) {
        throw new UnsupportedOperationException("getRemainingDemand() not yet supported!");
    }

    @Override
    public void updateDemand(final ISchedulableProcess process, final double demand) {
        throw new UnsupportedOperationException("updateDemand() not yet supported!");
    }

    @Override
    protected void enqueue(final ISchedulableProcess process) {
        running_processes.put(process.getId(), process);
        fireStateChange(running_processes.size(), 0);
    }

    @Override
    public void stop() {
        running_processes.clear();
    }

    @Override
    public void registerProcess(final ISchedulableProcess process) {

    }

    @Override
    public int getQueueLengthFor(final SchedulerEntity schedulerEntity, final int coreID) {
        return running_processes.size();
    }

}
