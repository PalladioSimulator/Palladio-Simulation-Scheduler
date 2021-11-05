package org.palladiosimulator.mosaic.scheduler.resources.active.cfs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.palladiosimulator.mosaic.scheduler.resources.TaskObserver;

import java.util.OptionalDouble;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.simucomframework.Context;

public class SimCGroup {
	
	private final TaskObserver callback;
	private Double processingRate;
	private double quota_period;
	private double quota_cores;
	
	public List<ISchedulableProcess> processes = new LinkedList<>();
	public Map<ISchedulableProcess, Double> scheduledDemands = new HashMap<>();
	
	
	public SimCGroup(TaskObserver callback) {
		this.callback = callback;
	}
	

	public double getRemainingDemand() {
		return scheduledDemands.values().stream().reduce(0.0, Double::sum);
	}
	
	
	public double grantDemand(double grantedDemand) {
		
		while(grantedDemand > 0.0 && scheduledDemands.size() > 0) {
			double fairShare = grantedDemand / processes.size();
			double unusedDemand = 0.0;
			for(var p : scheduledDemands.keySet())
			{
//				scheduledDemands.computeIfPresent(p, (key, d) -> 
//					 d - fairShare
//				);
				var newDemand = scheduledDemands.get(p) - fairShare;
				scheduledDemands.put(p, newDemand);
				var remainingDemand = scheduledDemands.get(p);
				if(remainingDemand <= 0.0) {
					p.activate();
					processes.remove(p);
					callback.fireTaskFinished(p);
					//collect unusedShares
					unusedDemand += remainingDemand * -1.0;
				}
				
			};
			grantedDemand = unusedDemand;
			//remove dangling demands for removed processes
			scheduledDemands.keySet().retainAll(processes);
		}
		
		
		return grantedDemand;
	}
	
	public OptionalDouble getMinDemand() {
		return scheduledDemands.entrySet().stream().mapToDouble(e -> e.getValue()).min();
	}
	
	
	public double addTask(ISchedulableProcess process, double demand) {
		
		this.processes.add(process);
		this.scheduledDemands.put(process, demand * this.getRate());
		
		return getRemainingDemand();
	}
	
	public int size() {
		return processes.size();
	}


	
	public double getRate() {
		return processingRate;
	}


	public void setRate(String rate) {
		this.processingRate = Context.evaluateStatic(rate, Double.class);
	}


	public double getQuota_period() {
		return quota_period;
	}


	public void setQuota_period(double quota_period) {
		this.quota_period = quota_period;
	}


	public double getQuota_cores() {
		return quota_cores;
	}


	public void setQuota_cores(double quota_cores) {
		this.quota_cores = quota_cores;
	}



}
