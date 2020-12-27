/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.run;

import org.apache.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.testcases.MatsimTestUtils;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.CapacityDependentInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyTracker;
import ch.sbb.matsim.routing.pt.raptor.RaptorInVehicleCostCalculator;



/**
 *
 * @author vsp-gleich
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunBerlinScenarioMIBTest {
	private static final Logger log = Logger.getLogger( RunBerlinScenarioMIBTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	// 1pct, testing the scores in iteration 0 and 1
	@Test
	public final void testCapacityDependentPtScoringAndRouting() {
		try {
			final String[] args = {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"};

			/*
			 * Idea: Find two parallel bus lines. Create agents taking taht bus from stop a to stop b which all depart
			 * at time 10:00, so the next bus would be line 1. Create so many agents that the bus is congested (not
			 * overfull, there should be no denied boarding) and check the score in iteration 0. In the second
			 * iteration (iteration 1), have strategy weight 100% on ReRoute (can also be set over all iterations).
			 * Some agents should now switch to the less congested bus of line 2 following the line 1 bus. Then ensure
			 * the scores are better at the end of iteration 1 than at the end of iteration 0, because agents switched
			 * to the less congested bus. This should ensure that a) occupancy dependent scoring is working and b)
			 * the router takes the occupancy into account.
			 */

			Config config =  RunBerlinScenarioMIBerlin.prepareConfig( args );
			config.controler().setLastIteration(5);

			config.strategy().setFractionOfIterationsToDisableInnovation(5);
			config.strategy().clearStrategySettings();
			
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
			stratSets.setStrategyName("ReRoute");
			stratSets.setWeight(1.0);
			stratSets.setSubpopulation("person");
			config.strategy().addStrategySettings(stratSets);
			
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.plans().setInputFile("../../../test/input/PopulationforMiBTest.xml");
			
			RunBerlinScenarioMIBerlin.setPTScoringParameter(config);
			
			Scenario scenario = RunBerlinScenarioMIBerlin.prepareScenario( config );
			
			RunBerlinScenarioMIBerlin.reduceVehicleCapacityPt(scenario, 10.0);
			
			Controler controler = RunBerlinScenarioMIBerlin.prepareControler( scenario ) ;
			
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(RunBerlinScenarioMIBerlin.occtracker(scenario));	
					bind(OccupancyTracker.class).toInstance(RunBerlinScenarioMIBerlin.occtracker(scenario));
					bind(CapacityDependentInVehicleCostCalculator.class).toInstance(new CapacityDependentInVehicleCostCalculator(0.4, 0.3, 0.6, 1.8));
					bind(RaptorInVehicleCostCalculator.class).to(CapacityDependentInVehicleCostCalculator.class);
				}
			});;
			
			
			controler.run() ;
			

			log.info( "Done with testCapacityDependentPtScoringAndRouting"  );
			log.info("") ;
			
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	

}
