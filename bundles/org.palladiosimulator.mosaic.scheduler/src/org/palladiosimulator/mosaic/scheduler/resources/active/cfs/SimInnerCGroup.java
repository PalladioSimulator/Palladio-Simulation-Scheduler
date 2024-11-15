package org.palladiosimulator.mosaic.scheduler.resources.active.cfs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.palladiosimulator.mosaic.scheduler.resources.TaskObserver;

import de.uka.ipd.sdq.simucomframework.Context;

public class SimInnerCGroup implements ISimCGroup {
	
	private Double processingRate;
	//defined in microseconds
	private long quota_period = 1000000;//µs
	//defined as portion of cores
	private double quota_cores = 1.0;
	
	private Map<ISimCGroup, Double> requestedRates = new HashMap<>();
	public List<ISimCGroup> subGroups = new LinkedList<>();

	
	public SimInnerCGroup(TaskObserver callback) {
		
	}
	
	public void addSubGroup(ISimCGroup group) {
		subGroups.add(group);
		requestedRates.put(group, group.getRate());
	}
	

	@Override
	public double getRemainingDemand() {
		return subGroups.stream().map(ISimCGroup::getRemainingDemand).reduce(0.0, Double::sum);
	}
	
	
	@Override
	public double grantDemand(double grantedDemand, long timePassed) {
		var periods = timePassed / quota_period;
		var allowance = (quota_cores * quota_period * periods) / 1000;
		var overQuota = 0.0;
		if (grantedDemand > allowance) { 
			grantedDemand = allowance;
			overQuota = grantedDemand - allowance;
		}
				
		var unused = SimFairGroupScheduler.splitDemandsPerRequest(grantedDemand, requestedRates, timePassed);
		
		
		return unused + overQuota;
	}
	



	
	@Override
	public double getRate() {
		return processingRate;
	}


	@Override
	public void setRate(String rate) {
		this.processingRate = Context.evaluateStatic(rate, Double.class);
	}


	@Override
	public long getQuota_period() {
		return quota_period;
	}


	@Override
	public void setQuota_period(long quota_period) {
		this.quota_period = quota_period;
	}


	@Override
	public double getQuota_cores() {
		return quota_cores;
	}


	@Override
	public void setQuota_cores(double quota_cores) {
		this.quota_cores = quota_cores;
	}


	@Override
	public int size() {
		return subGroups.stream().mapToInt(ISimCGroup::size).sum();
	}


	@Override
	public OptionalDouble getMinDemand() {
		return this.subGroups.stream().map(ISimCGroup::getMinDemand)
				.filter(OptionalDouble::isPresent)
				.mapToDouble(OptionalDouble::getAsDouble).min();
	}

	


}
