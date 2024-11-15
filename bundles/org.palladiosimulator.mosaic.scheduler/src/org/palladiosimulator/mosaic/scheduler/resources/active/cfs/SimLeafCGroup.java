package org.palladiosimulator.mosaic.scheduler.resources.active.cfs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.palladiosimulator.mosaic.scheduler.resources.TaskObserver;

import java.util.OptionalDouble;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.simucomframework.Context;

public class SimLeafCGroup implements ISimCGroup {
	
	private final TaskObserver callback;
	private Double processingRate = 1.0;
	//defined in microseconds
	private long quota_period = 1000000;//µs
	//defined as portion of cores
	private double quota_cores = 1.0;
	
	public List<ISchedulableProcess> processes = new LinkedList<>();
	public Map<ISchedulableProcess, Double> scheduledDemands = new HashMap<>();
	
	
	public SimLeafCGroup(TaskObserver callback) {
		this.callback = callback;
	}
	

	public double getRemainingDemand() {
		return scheduledDemands.values().stream().reduce(0.0, Double::sum);
	}
	
	
	public double grantDemand(double grantedDemand, long timePassed) {
		var periods = timePassed / quota_period;
		
		var allowance = (quota_cores * quota_period * periods) / 1000;
		var overQuota = 0.0;
		if (grantedDemand > allowance) {
			grantedDemand = allowance;
			overQuota = grantedDemand - allowance;
		}
		
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
		
		
		return grantedDemand + overQuota;
	}
	
	public OptionalDouble getMinDemand() {
		var min = scheduledDemands.entrySet().stream().mapToDouble(e -> e.getValue()).min();
		return min;
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


	public long getQuota_period() {
		return quota_period;
	}


	public void setQuota_period(long quota_period) {
		this.quota_period = quota_period;
	}


	public double getQuota_cores() {
		return quota_cores;
	}


	public void setQuota_cores(double quota_cores) {
		this.quota_cores = quota_cores;
	}

	


}
