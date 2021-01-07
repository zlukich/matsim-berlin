/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.analysis.RunPersonTripAnalysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoreEventScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.extensions.pt.replanning.singleTripStrategies.ChangeSingleTripModeAndRoute;
import org.matsim.extensions.pt.replanning.singleTripStrategies.RandomSingleTripReRoute;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;
import org.matsim.vehicles.VehicleType;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.inject.Singleton;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.CapacityDependentInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyTracker;
import ch.sbb.matsim.routing.pt.raptor.RaptorInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

/**
* @author ikaddoura
* hmarciales
*/

public final class RunBerlinScenarioMIB {

	private static final Logger log = Logger.getLogger(RunBerlinScenarioMIB.class );

	public static void main(String[] args) {
		
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.config.xml"}  ;
		}
		
		var arguments = new InputArgs();
		JCommander.newBuilder().addObject(arguments).build().parse(args);

		Config config = prepareConfig( new String[] {"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"} ) ;
		
		config.controler().setOutputDirectory(arguments.outputFile);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(500);
		
		config.network().setInputFile(arguments.inputFile + "berlin-v5.5-network.xml.gz");
		config.getModules().get("transit").getParams().put("transitScheduleFile", arguments.inputFile + "/berlin-v5.5-transit-schedule.xml.gz");
		config.getModules().get("transit").getParams().put("vehiclesFile", arguments.inputFile + "/berlin-v5.5-transit-vehicles.xml.gz");
		
		Scenario scenario = prepareScenario( config ) ;
		RunBerlinScenarioMIB.reduceVehicleCapacityPt(scenario, 10.0);
		Controler controler = prepareControler( scenario ) ;
		
			
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(occtracker(scenario));	
				bind(OccupancyTracker.class).toInstance(occtracker(scenario));
				bind(CapacityDependentInVehicleCostCalculator.class).toInstance(new CapacityDependentInVehicleCostCalculator(0.4, 0.3, 0.6, 1.8));
				bind(RaptorInVehicleCostCalculator.class).to(CapacityDependentInVehicleCostCalculator.class);
			}
		});;

		controler.run() ;

	}

	public static Controler prepareControler( Scenario scenario ) {
		// note that for something like signals, and presumably drt, one needs the controler object
		
		Gbl.assertNotNull(scenario);
		
		final Controler controler = new Controler( scenario );
		
		if (controler.getConfig().transit().isUseTransit()) {
			// use the sbb pt raptor router
			
			ConfigUtils.addOrGetModule(scenario.getConfig(), SwissRailRaptorConfigGroup.class).setUseCapacityConstraints(true);
			
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					install( new SwissRailRaptorModule() );
				}
			} );
		} else {
			log.warn("Public transit will be teleported and not simulated in the mobsim! "
					+ "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
					+ "Should only be used for testing or car-focused studies with a fixed modal split.  ");
		}
		
		
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
				bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);
				
				addPlanStrategyBinding("RandomSingleTripReRoute").toProvider(RandomSingleTripReRoute.class);
				addPlanStrategyBinding("ChangeSingleTripModeAndRoute").toProvider(ChangeSingleTripModeAndRoute.class);

				bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);

				//use income-dependent marginal utility of money for scoring
				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).in(Singleton.class);
			}
		} );

		return controler;
	}
	
	public static Scenario prepareScenario( Config config ) {
		Gbl.assertNotNull( config );
		
		// note that the path for this is different when run from GUI (path of original config) vs.
		// when run from command line/IDE (java root).  :-(    See comment in method.  kai, jul'18
		// yy Does this comment still apply?  kai, jul'19

		/*
		 * We need to set the DrtRouteFactory before loading the scenario. Otherwise DrtRoutes in input plans are loaded
		 * as GenericRouteImpls and will later cause exceptions in DrtRequestCreator. So we do this here, although this
		 * class is also used for runs without drt.
		 */
		final Scenario scenario = ScenarioUtils.createScenario( config );

		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		
		ScenarioUtils.loadScenario(scenario);

		BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
		if (berlinCfg.getPopulationDownsampleFactor() != 1.0) {
			downsample(scenario.getPopulation().getPersons(), berlinCfg.getPopulationDownsampleFactor());
		}
		
		return scenario;
	}

	public static Config prepareConfig( String [] args, ConfigGroup... customModules ){
		return prepareConfig( RunDrtOpenBerlinScenario.AdditionalInformation.none, args, customModules ) ;
	}
	public static Config prepareConfig( RunDrtOpenBerlinScenario.AdditionalInformation additionalInformation, String [] args,
					    ConfigGroup... customModules ) {
		OutputDirectoryLogging.catchLogEntries();
		
		String[] typedArgs = Arrays.copyOfRange( args, 1, args.length );
		
		ConfigGroup[] customModulesToAdd = null ;
		if ( additionalInformation== RunDrtOpenBerlinScenario.AdditionalInformation.acceptUnknownParamsBerlinConfig ) {
			customModulesToAdd = new ConfigGroup[]{ new BerlinExperimentalConfigGroup(true) };
		} else {
			customModulesToAdd = new ConfigGroup[]{ new BerlinExperimentalConfigGroup(false) };
		}
		ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];
		
		int counter = 0;
		for (ConfigGroup customModule : customModules) {
			customModulesAll[counter] = customModule;
			counter++;
		}
		
		for (ConfigGroup customModule : customModulesToAdd) {
			customModulesAll[counter] = customModule;
			counter++;
		}
		
		final Config config = ConfigUtils.loadConfig( args[ 0 ], customModulesAll );
		
		config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );
		
		config.plansCalcRoute().setRoutingRandomness( 3. );
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
		config.plansCalcRoute().removeModeRoutingParams("undefined");
		
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );
				
		// vsp defaults
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info );
		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
				
		// activities:
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			config.planCalcScore().addActivityParams( new ActivityParams( "home_" + ii + ".0" ).setTypicalDuration( ii ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "work_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "leisure_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "shopping_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(8. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "other_" + ii + ".0" ).setTypicalDuration( ii ) );
		}
		config.planCalcScore().addActivityParams( new ActivityParams( "freight" ).setTypicalDuration( 12.*3600. ) );
		
		

		ConfigUtils.applyCommandline( config, typedArgs ) ;

		return config ;
	}
	
	public static void runAnalysis(Controler controler) {
		Config config = controler.getConfig();
		
		String modesString = "";
		for (String mode: config.planCalcScore().getAllModes()) {
			modesString = modesString + mode + ",";
		}
		// remove last ","
		if (modesString.length() < 2) {
			log.error("no valid mode found");
			modesString = null;
		} else {
			modesString = modesString.substring(0, modesString.length() - 1);
		}
		
		String[] args = new String[] {
				config.controler().getOutputDirectory(),
				config.controler().getRunId(),
				"null", // TODO: reference run, hard to automate
				"null", // TODO: reference run, hard to automate
				config.global().getCoordinateSystem(),
				"https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-bezirke/bezirke_berlin.shp",
				TransformationFactory.DHDN_GK4,
				"SCHLUESSEL",
				"home",
				"10", // TODO: scaling factor, should be 10 for 10pct scenario and 100 for 1pct scenario
				"null", // visualizationScriptInputDirectory
				modesString
		};
		
		try {
			RunPersonTripAnalysis.main(args);
		} catch (IOException e) {
			log.error(e.getStackTrace());
			throw new RuntimeException(e.getMessage());
		}
	}
	
	private static void downsample( final Map<Id<Person>, ? extends Person> map, final double sample ) {
		final Random rnd = MatsimRandom.getLocalInstance();
		log.warn( "Population downsampled from " + map.size() + " agents." ) ;
		map.values().removeIf( person -> rnd.nextDouble() > sample ) ;
		log.warn( "Population downsampled to " + map.size() + " agents." ) ;
	}
	
	public static OccupancyTracker occtracker (Scenario scenario){
		ScoringParametersForPerson parameters = new SubpopulationScoringParameters(scenario);

		EventsManager events = EventsUtils.createEventsManager();
		RaptorInVehicleCostCalculator inVehicleCostCalculator = new CapacityDependentInVehicleCostCalculator(0.4, 0.3, 0.6, 5.0);
		OccupancyData occData = new OccupancyData();
		return new OccupancyTracker(occData, scenario, inVehicleCostCalculator, events, parameters);
		
	}
	
	public static ScoringFunctionFactory testSFF(Scenario scenario, Config config) {
		ScoringFunctionFactory testSFF = new ScoringFunctionFactory() {
			ScoringParametersForPerson parameters = new SubpopulationScoringParameters(scenario);

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork(), config.transit().getTransitModes()));
				scoringFunctionAccumulator.addScoringFunction(new ScoreEventScoring());

				return scoringFunctionAccumulator;
			}
		};
		return testSFF;
		
	}
	
	public static void increaseVehicleTypePassengerCarEquivalents (Scenario scenario, Double equivalency ){
		Set<Id<VehicleType>> vehicleTypeIds = scenario.getTransitVehicles().getVehicleTypes().keySet();
		for (Id<VehicleType> id: vehicleTypeIds) 
		scenario.getTransitVehicles().getVehicleTypes().get(id).setPcuEquivalents(equivalency);
		
	}
	
	public static void reduceVehicleCapacityPt(Scenario scenario, Double equivalency){
		Set<Id<VehicleType>> vehicleTypeIds = scenario.getTransitVehicles().getVehicleTypes().keySet();
		for (Id<VehicleType> id: vehicleTypeIds) { 
		Double seats = scenario.getTransitVehicles().getVehicleTypes().get(id).getCapacity().getSeats() / equivalency;	
		Double  standingRoom = scenario.getTransitVehicles().getVehicleTypes().get(id).getCapacity().getStandingRoom() / equivalency;
		scenario.getTransitVehicles().getVehicleTypes().get(id).getCapacity().setSeats(seats.intValue()+1);
		scenario.getTransitVehicles().getVehicleTypes().get(id).getCapacity().setStandingRoom(standingRoom.intValue());
		}
	}
	
	public static void setPTScoringParameter (Config config){
		config.planCalcScore().getModes().get("pt").setMarginalUtilityOfTraveling(-0.18);
	}
	
	private static class InputArgs {

		@Parameter(names = {"-input"}, required = true)
		String inputFile = "Input/europe-latest.osm.pbf";

		@Parameter(names = {"-output"}, required = true)
		String outputFile = "Output/Network.xml.gz";
	}

}

