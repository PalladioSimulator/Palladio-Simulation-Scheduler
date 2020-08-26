package de.uka.ipd.sdq.scheduler.resources.active;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;

public interface IResourceTableManager {
    AbstractActiveResource getLastResource(ISchedulableProcess process);
    
    void setLastResource(ISchedulableProcess process, AbstractActiveResource resource);
    
    void notifyTerminated(ISchedulableProcess simProcess);
    
    void cleanProcesses();
}
