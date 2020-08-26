package de.uka.ipd.sdq.scheduler.resources.active;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;

public class ResourceTableManager implements IResourceTableManager {
    private final Map<ISchedulableProcess, AbstractActiveResource> currentResourceTable = new ConcurrentHashMap<>();

    @Override
    public AbstractActiveResource getLastResource(ISchedulableProcess process) {
        return currentResourceTable.get(process);
    }

    @Override
    public void setLastResource(ISchedulableProcess process, AbstractActiveResource resource) {
        if (!currentResourceTable.containsKey(process)) {
            process.addTerminatedObserver(resource);
        }
        currentResourceTable.put(process, resource);
    }

    public void notifyTerminated(ISchedulableProcess simProcess) {
        currentResourceTable.remove(simProcess);
    }
    
    @Override
    public void cleanProcesses() {
        // Activate all waiting processes to yield process completion
        // Synchronization with process() avoids that processes are added after
        // the activation.
        for (ISchedulableProcess process : currentResourceTable.keySet()) {
            if (!process.isFinished()) {
                // TODO: to avoid exceptions at the end of the simulation,
                // these are being caught here. Maybe something can be fixed
                // in the simulation so that the exception does not occur here.
                try {
                    process.activate();
                } catch (IllegalStateException e) {

                }
            }
        }

        // assert that all threads have been terminated.
        assert currentResourceTable.size() == 0;
    }
}
