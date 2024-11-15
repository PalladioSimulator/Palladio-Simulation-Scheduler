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
	

	private List<ISimCGroup> rootGroups = new LinkedList<>();
	private Map<String, ISimCGroup> groupIds = new HashMap<>();
	private Map<ISimCGroup, Double> requestedRates = new HashMap<>();
	private Map<ISimCGroup, Double> quotas = new HashMap<>();
	
	
	
	
	public double getNextSchedule() {
		// calculate quotient by summing up the rates
		final double grantedRateQuotient = requestedRates.entrySet().stream()
				.filter(e -> e.getKey().size() > 0).mapToDouble(e -> e.getValue()).sum();
		
		// calculate minimal time span until a cgroup finishes its minimum demand process using the current ratio
		OptionalDouble minFinishTime =  requestedRates.entrySet().stream()
		.filter(e -> e.getKey().size() > 0).mapToDouble(e -> {
			var g = e.getKey();
			// the overall min time is the smallest remaining slot time the number of waiting process
			var minDouble = g.getMinDemand().orElse(-1.0);
			var minGroupDemand = minDouble * g.size();
			// the groupSlowDown is the portion of the overall demand that is assigned to this group
			var groupSlowDown = e.getValue() / grantedRateQuotient;
			// divide the demand by the slowdown to get the minimalTime for this group to finish
			return minGroupDemand / groupSlowDown;
			}).filter(d -> d > 0).min(); //filter out negativ = empty
		
		if(minFinishTime.isEmpty() || minFinishTime.getAsDouble() <= 0.0)
			return -1.0;
		
		return Math.max( minFinishTime.getAsDouble() / processingRate, JIFFY);
	}
	
	
	public void grantDemand(double grantedDemand, long timePassed) {
		
		var unconsumedDemands = splitDemandsPerRequest(grantedDemand, requestedRates, timePassed);
		//should not happen under normal circumstances as it indicates that we have granted to much demand to groups
		if(unconsumedDemands > 0 && getNextSchedule() > JIFFY)
			System.out.println("encountered unused demands" + unconsumedDemands); //TODO: remove after testing
			//LoggingWrapper.log("encountered unused demands" + unconsumedDemands); this is doing nothing currently
			//grantDemand(unconsumedDemands, timePassed); //TODO: if really needed unroll to while loop
		
	}


	public static double splitDemandsPerRequest(double grantedDemand, Map<ISimCGroup, Double> requestedGroupRates, long timePassed) {

		
		if(grantedDemand <= 0.0)
			return 0.0;
		
		// calculate quotient by summing up the rates
		final double grantedRateQuotient = requestedGroupRates.entrySet().stream()
				.filter(e -> e.getKey().size() > 0).mapToDouble(e -> e.getValue()).sum();
		
		// grant every group the demand according to its fair share
		var unconsumedDemands = requestedGroupRates.entrySet().stream()
		.filter(e -> e.getKey().size() > 0).mapToDouble( e -> {
			var group = e.getKey();
			var groupSlowDown = e.getValue() / grantedRateQuotient;
			return group.grantDemand(grantedDemand * groupSlowDown, timePassed);
		}).sum();
		
		return unconsumedDemands;
	}
	
	public void enqueueProcessDemand(String groupId, ISchedulableProcess process, double demand) {
		
//		TODO:if(!groupIds.containsKey(groupId))
			
		
		var group = groupIds.get(groupId);
		enqueueProcessDemand(group, process, demand);
		
	}
	
	
	public void enqueueProcessDemand(ISimCGroup group, ISchedulableProcess process, double demand) {
		
		if(!groupIds.containsValue(group)) {
			//groups.add(group);
			// TODO: do we want to update more frequently?
			
		}
		
		if(group instanceof SimLeafCGroup)
			((SimLeafCGroup) group).addTask(process, demand);
		
		
	}
	
	public void addGroup(ISimCGroup group, String groupId, boolean isRoot) {
		groupIds.put(groupId, group);
		if(isRoot) {
			rootGroups.add(group);
			updateGroup(group);
		}
	}
	
	

	private void updateGroup(ISimCGroup group) {
		requestedRates.put(group, group.getRate());
	}
	
	
	public double getProcessingRate() {
		return processingRate;
	}
	
	
	public void setProcessingRate(double processingRate) {
		this.processingRate = processingRate;
	}


	public int getActiveProcessSize() {
		return groupIds.values().stream().mapToInt(ISimCGroup::size).sum();
	}


	public boolean hasGroup(String groupId) {
		return groupIds.containsKey(groupId);
	}
	
	public ISimCGroup getGroup(String groupId) {
		return groupIds.get(groupId);
	}

}
