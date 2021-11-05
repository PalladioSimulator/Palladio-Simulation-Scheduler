package org.palladiosimulator.mosaic.scheduler.resources.active;

public interface ISchedulableEntity {
	
	
	public double getStretchFactor();
	
	public double getRemainingDemand();
	
	public void grantDemand(double demand);
	

}
