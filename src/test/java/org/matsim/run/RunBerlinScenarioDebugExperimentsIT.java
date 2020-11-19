package org.matsim.run;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PersonRemoveLinkAndRoute;
import org.matsim.testcases.MatsimTestUtils;

public class RunBerlinScenarioDebugExperimentsIT{
	private static final Logger log = Logger.getLogger( RunBerlinScenarioDebugExperimentsIT.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public final void test10pct() {
		try {
			final String[] args = {"scenarios/berlin-v5.4-10pct/input/berlin-v5.4-10pct.config.xml"};

			Config config =  RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(0);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
			
			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;

			controler.run() ;

		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void test10pctRemoveRideModeOnSomeLinks() {
		try {
			final String[] args = {"scenarios/berlin-v5.4-10pct/input/berlin-v5.4-10pct.config.xml"};

			Config config =  RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(0);
			config.strategy().setFractionOfIterationsToDisableInnovation(1);
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
			
			Set<String> modes = new HashSet<>();
			modes.add("car");
			modes.add("freight");
			
			// remove ride as allowed mode somewhere in the network
			scenario.getNetwork().getLinks().get(Id.createLinkId("34414")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("34425")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("39205")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("39251")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("5408")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("5951")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("66294")).setAllowedModes(modes);

			// remove initial routes 
			for (Person person : scenario.getPopulation().getPersons().values()) {
				if (person.getPlans().size() > 1) {
					throw new RuntimeException("More than just one plan! Aborting...");
				}
				PersonRemoveLinkAndRoute removeLinkAndRoute = new PersonRemoveLinkAndRoute();
				removeLinkAndRoute.run(person);	
			}
			
			
			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;

			controler.run() ;

		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
	}

	@Test
	public final void test1pctRemoveRideModeOnSomeLinks() {
	
		final int iteration = 0;
		try {
			final String[] args = {"scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml"};

			Config config = RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(iteration);

			config.qsim().setNumberOfThreads( 1 );
			config.global().setNumberOfThreads( 1 );
			// small number of threads in hope to consume less memory.  kai, jul'18

			config.strategy().setFractionOfIterationsToDisableInnovation( 1.0 );

			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );

			config.controler().setWriteEventsInterval( 1 );
			config.controler().setWritePlansUntilIteration( 1 );
			config.controler().setWritePlansInterval( 1 );
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
			
			final double sample = 1.0;
			downsample( scenario.getPopulation().getPersons(), sample ) ;
			config.qsim().setFlowCapFactor( config.qsim().getFlowCapFactor()*sample );
			config.qsim().setStorageCapFactor( config.qsim().getStorageCapFactor()*sample );
			
			Set<String> modes = new HashSet<>();
			modes.add("car");
			modes.add("freight");
			
			// remove ride as allowed mode somewhere in the network
			scenario.getNetwork().getLinks().get(Id.createLinkId("34414")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("34425")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("39205")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("39251")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("5408")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("5951")).setAllowedModes(modes);
			scenario.getNetwork().getLinks().get(Id.createLinkId("66294")).setAllowedModes(modes);

			// remove initial routes 
			for (Person person : scenario.getPopulation().getPersons().values()) {
				if (person.getPlans().size() > 1) {
					throw new RuntimeException("More than just one plan! Aborting...");
				}
				PersonRemoveLinkAndRoute removeLinkAndRoute = new PersonRemoveLinkAndRoute();
				removeLinkAndRoute.run(person);	
			}

			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;

			Gbl.assertNotNull( controler.getScoreStats() );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory() );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ) );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ).get(0) );

			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ).get(iteration) );

		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void test1pct() {

		final int iteration = 0;
		try {
			final String[] args = {"scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml"};

			Config config = RunBerlinScenario.prepareConfig( args ) ;
			config.controler().setLastIteration(iteration);

			config.qsim().setNumberOfThreads( 1 );
			config.global().setNumberOfThreads( 1 );
			// small number of threads in hope to consume less memory.  kai, jul'18

			config.strategy().setFractionOfIterationsToDisableInnovation( 1.0 );

			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );

			config.controler().setWriteEventsInterval( 1 );
			config.controler().setWritePlansUntilIteration( 1 );
			config.controler().setWritePlansInterval( 1 );
			
			Scenario scenario = RunBerlinScenario.prepareScenario( config ) ;
			
			final double sample = 1.0;
			downsample( scenario.getPopulation().getPersons(), sample ) ;
			config.qsim().setFlowCapFactor( config.qsim().getFlowCapFactor()*sample );
			config.qsim().setStorageCapFactor( config.qsim().getStorageCapFactor()*sample );

			Controler controler = RunBerlinScenario.prepareControler( scenario ) ;
			
			controler.run() ;

			Gbl.assertNotNull( controler.getScoreStats() );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory() );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ) );
			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ).get(0) );

			Gbl.assertNotNull( controler.getScoreStats().getScoreHistory().get( ScoreStatsControlerListener.ScoreItem.average ).get(iteration) );

		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	private static void downsample( final Map<Id<Person>, ? extends Person> map, final double sample ) {
		final Random rnd = MatsimRandom.getLocalInstance();
		log.warn( "map size before=" + map.size() ) ;
		map.values().removeIf( person -> rnd.nextDouble()>sample ) ;
		log.warn( "map size after=" + map.size() ) ;
	}

}
