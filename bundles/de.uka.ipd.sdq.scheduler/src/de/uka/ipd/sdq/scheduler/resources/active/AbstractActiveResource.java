package de.uka.ipd.sdq.scheduler.resources.active;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.uka.ipd.sdq.scheduler.IActiveResource;
import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.resources.AbstractSimResource;
import de.uka.ipd.sdq.scheduler.sensors.IActiveResourceStateSensor;

public abstract class AbstractActiveResource extends AbstractSimResource implements IActiveResource {

    private final List<IActiveResourceStateSensor> observers;
    private final IResourceTableManager resourceTableManager;

    public AbstractActiveResource(SchedulerModel model, long capacity, String name, String id, IResourceTableManager resourceTableManager) {
        super(model, capacity, name, id);
        observers = new ArrayList<IActiveResourceStateSensor>();
        this.resourceTableManager = resourceTableManager;
    }

    protected abstract void doProcessing(ISchedulableProcess process, int resourceServiceID, double demand);
    protected abstract void enqueue(ISchedulableProcess process);
    protected abstract void dequeue(ISchedulableProcess process);

    @Override
    public final void process(ISchedulableProcess process, int resourceServiceID,
            Map<String, Serializable> parameterMap, double demand) {
        if (!getModel().getSimulationControl().isRunning()) {
            // Do nothing, but allows calling process to complete
            return;
        }

        AbstractActiveResource last = resourceTableManager.getLastResource(process);
        if (!this.equals(last)) {
            if (last != null) {
                last.dequeue(process);
            }
            this.enqueue(process);
            resourceTableManager.setLastResource(process, this);
        }
        if (parameterMap != null && parameterMap.isEmpty()) {
            doProcessing(process, resourceServiceID, demand);
        } else {
            doProcessing(process, resourceServiceID, parameterMap, demand);
        }
    }


    protected void doProcessing(ISchedulableProcess process, int resourceServiceID,
            Map<String, Serializable> parameterMap, double demand) {
        throw new RuntimeException(
                "doProcessing has to be overwritten to allow additional Parameters for active Resources");
    }

    @Override
    public void notifyTerminated(ISchedulableProcess simProcess) {
        resourceTableManager.notifyTerminated(simProcess);
    }

    @Override
    public void addObserver(IActiveResourceStateSensor observer) {
        this.observers.add(observer);
    }

    @Override
    public void removeObserver(IActiveResourceStateSensor observer) {
        this.observers.remove(observer);
    }

    protected void fireStateChange(long state, int instanceId) {
        for (IActiveResourceStateSensor l : observers) {
            l.update(state, instanceId);
        }
    }

    protected void fireDemandCompleted(ISchedulableProcess simProcess) {
        for (IActiveResourceStateSensor l : observers) {
            l.demandCompleted(simProcess);
        }
    }

}
