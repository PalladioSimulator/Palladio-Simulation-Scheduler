package org.palladiosimulator.mosaic.scheduler.resources.active.cfs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Map.Entry;

import de.uka.ipd.sdq.probfunction.math.util.MathTools;
import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.LoggingWrapper;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.simulation.abstractsimengine.AbstractSimEventDelegator;

public class SimFairGroupScheduler {
	
	// the processingRate for the group scheduler is assumed to be noOfCores * processingRateOfCore
	private double processingRate = 1.0;

	/**
     * The minimum amount of time used for scheduling an event
     */
    private final static double JIFFY = 1e-9;

    
	/*
	 * Quota variables per group
	 * cpu.cfs_quota_us: the total available run-time within a period (in microseconds)
	 * cpu.cfs_period_us: the length of a period (in microseconds)
	 */
	


	private List<SimCGroup> groups = new LinkedList<>();
	private Map<String, SimCGroup> groupIds = new HashMap<>();
	private Map<SimCGroup, Double> requestedRates = new HashMap<>();
	private Map<SimCGroup, Double> quotas = new HashMap<>();
	
	
	
	
	public double getNextSchedule() {
		// calculate quotient by summing up the rates
		final double grantedRateQuotient = requestedRates.entrySet().stream()
				.filter(e -> e.getKey().size() > 0).mapToDouble(e -> e.getValue()).sum();
		
		// calculate minimal time span until a cgroup finishes its minimum demand process using the current ratio
		OptionalDouble minFinishTime =  requestedRates.entrySet().stream()
		.filter(e -> e.getKey().size() > 0).mapToDouble(e -> {
			var g = e.getKey();
			// the overall min time is the smallest remaining slot time the number of waiting process
			var minGroupDemand = g.getMinDemand().getAsDouble() * g.size();
			// the groupSlowDown is the portion of the overall demand that is assigned to this group
			var groupSlowDown = e.getValue() / grantedRateQuotient;
			// divide the demand by the slowdown to get the minimalTime for this group to finish
			return minGroupDemand / groupSlowDown;
			}).min();
		
		if(minFinishTime.isEmpty())
			return -1.0;
		
		return Math.max( minFinishTime.getAsDouble() / processingRate, JIFFY);
	}
	
	
	public void grantDemand(double grantedDemand) {
		
		if(grantedDemand <= 0.0)
			return;
		
		// calculate quotient by summing up the rates
		final double grantedRateQuotient = requestedRates.entrySet().stream()
				.filter(e -> e.getKey().size() > 0).mapToDouble(e -> e.getValue()).sum();
		
		// grant every group the demand according to its fair share
		var unconsumedDemands = requestedRates.entrySet().stream()
		.filter(e -> e.getKey().size() > 0).mapToDouble( e -> {
			var group = e.getKey();
			var groupSlowDown = e.getValue() / grantedRateQuotient;
			return group.grantDemand(grantedDemand * groupSlowDown);
		}).sum();
		
		//should not happen under normal circumstances as it indicates that we have granted to much demand to groups
		if(unconsumedDemands > 0 && getNextSchedule() > JIFFY)
			grantDemand(unconsumedDemands); //TODO: if really needed unroll to while loop
		
	}
	
	public void enqueueProcessDemand(String groupId, ISchedulableProcess process, double demand) {
		
//		TODO:if(!groupIds.containsKey(groupId))
			
		
		var group = groupIds.get(groupId);
		enqueueProcessDemand(group, process, demand);
		
	}
	
	
	public void enqueueProcessDemand(SimCGroup group, ISchedulableProcess process, double demand) {
		
		if(!groups.contains(group)) {
			groups.add(group);
			// TODO: do we want to update more frequently?
			updateGroup(group);
		}
		
		group.addTask(process, demand);
		
		
	}
	
	public void addGroup(SimCGroup group, String id) {
		groupIds.put(id, group);
		groups.add(group);
		updateGroup(group);
	}


	private void updateGroup(SimCGroup group) {
		requestedRates.put(group, group.getRate());
	}
	
	
	public double getProcessingRate() {
		return processingRate;
	}
	
	
	public void setProcessingRate(double processingRate) {
		this.processingRate = processingRate;
	}


	public int getActiveProcessSize() {
		return groups.stream().mapToInt(SimCGroup::size).sum();
	}

}
