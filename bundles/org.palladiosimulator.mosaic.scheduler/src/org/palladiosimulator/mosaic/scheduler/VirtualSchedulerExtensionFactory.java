package org.palladiosimulator.mosaic.scheduler;

import org.palladiosimulator.mosaic.scheduler.resources.active.SimVirtualDelegatingResource;

import de.uka.ipd.sdq.scheduler.IActiveResource;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.factory.SchedulerExtensionFactory;
import de.uka.ipd.sdq.scheduler.resources.active.IResourceTableManager;

public class VirtualSchedulerExtensionFactory implements SchedulerExtensionFactory {

	public VirtualSchedulerExtensionFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IActiveResource getExtensionScheduler(SchedulerModel model, String resourceName, String resourceId,
			long numberOfCores, IResourceTableManager resourceTableManager) {

		return new SimVirtualDelegatingResource(model, resourceId, resourceId, resourceTableManager);
	}

}
