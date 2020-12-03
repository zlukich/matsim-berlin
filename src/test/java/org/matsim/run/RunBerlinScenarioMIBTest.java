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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;



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
			
			Config config =  RunBerlinScenarioMiB.prepareConfig( args );
			config.controler().setLastIteration(1);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.plans().setInputFile("../../../test/input/test-agents.xml");
			
			Scenario scenario = RunBerlinScenarioMiB.prepareScenario( config );
			
			Controler controler = RunBerlinScenarioMiB.prepareControler( scenario ) ;
			
			controler.run() ;
			

			log.info( "Done with testCapacityDependentPtScoringAndRouting"  );
			log.info("") ;
			
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
}
