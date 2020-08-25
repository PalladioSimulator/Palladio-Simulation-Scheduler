package de.uka.ipd.sdq.scheduler.entities;

import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.simulation.abstractsimengine.AbstractSimEntityDelegator;

/**
 * 
 * @author Philipp Merkle
 *
 */
public class SchedulerEntity extends AbstractSimEntityDelegator {

    protected SchedulerEntity(SchedulerModel model, String name) {
        super(model, name);
    }

    @Override
    public SchedulerModel getModel() {
        return (SchedulerModel) super.getModel();
    }

}
