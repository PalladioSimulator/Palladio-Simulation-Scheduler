package org.palladiosimulator.mosaic.scheduler.resources.active.cfs;

import java.util.OptionalDouble;

public interface ISimCGroup {

	double getRemainingDemand();

	/**
	 * Assign the specified demand to the cgroup for a given time span. 
	 * @param grantedDemand Demand assigned
	 * @param timePassed Time passed given in µseconds
	 * @return
	 */
	double grantDemand(double grantedDemand, long timePassed);

	double getRate();

	void setRate(String rate);

	long getQuota_period();

	void setQuota_period(long quota_period);

	double getQuota_cores();

	void setQuota_cores(double quota_cores);

	int size();

	OptionalDouble getMinDemand();

}