package org.palladiosimulator.mosaic.scheduler.resources;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;

public interface TaskObserver {

	public void fireTaskFinished(ISchedulableProcess process);
	
}
