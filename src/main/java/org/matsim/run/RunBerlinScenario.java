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

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.analysis.RunPersonTripAnalysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.prepare.population.AssignIncome;
import org.matsim.run.drt.OpenBerlinIntermodalPtDrtRouterModeIdentifier;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

/**
* @author ikaddoura
*/

public final class RunBerlinScenario {

	private static final Logger log = Logger.getLogger(RunBerlinScenario.class );

	public static void main(String[] args)throws Exception {
		
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.config.xml"}  ;
		}

		Config config = prepareConfig( args );
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = prepareScenario( config ) ;
		Network network =  scenario.getNetwork();

		var links = network.getLinks();
		Integer ids_of_links_to_be_changed[] = {
				122077,
				32393,
				152714,
				46981,
				122763,
				128554,
				10911,
				//85556,
				123234,
				150027,
				68877,
				68882,
				68875,
				128376,
				//104400,
				92405,
				61167,
				//68028,
				63665,
				15713,
				76115,
				122445,
				27939,
				153398,
				154994,
				//5670,
				82261,
				//63748,
				104386,
				69152,
				};






		Controler controler = prepareControler( scenario ) ;
		controler.run();
	}


	public static void removeAll(Node node, short nodeType, String name) {
		if (node.getNodeType() == nodeType && (name == null || node.getNodeName().equals(name))) {
			node.getParentNode().removeChild(node);
		} else {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				removeAll(list.item(i), nodeType, name);
			}
		}
	}





	public static Controler prepareControler( Scenario scenario ) {
		// note that for something like signals, and presumably drt, one needs the controler object
		
		Gbl.assertNotNull(scenario);
		
		final Controler controler = new Controler( scenario );

			// use the sbb pt raptor router
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				install( new SwissRailRaptorModule() );
			}
		} );


		
		
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
				bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);
				bind(MultimodalLinkChooser.class).to(NetworkFixMultimodalLinkChooser.class);
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

		AssignIncome.assignIncomeToPersonSubpopulationAccordingToGermanyAverage(scenario.getPopulation());
		return scenario;
	}

	public static Config prepareConfig( String [] args, ConfigGroup... customModules ){

		return prepareConfig( RunDrtOpenBerlinScenario.AdditionalInformation.none, args, customModules ) ;
	}
	public static Config prepareConfig( RunDrtOpenBerlinScenario.AdditionalInformation additionalInformation, String [] args,
					    ConfigGroup... customModules ) {
		OutputDirectoryLogging.catchLogEntries();
		
		String[] typedArgs = Arrays.copyOfRange( args, 1, args.length );
		
		ConfigGroup[] customModulesToAdd;
		if (additionalInformation == RunDrtOpenBerlinScenario.AdditionalInformation.acceptUnknownParamsBerlinConfig) {
			customModulesToAdd = new ConfigGroup[]{new BerlinExperimentalConfigGroup(true)};
		} else {
			customModulesToAdd = new ConfigGroup[]{new BerlinExperimentalConfigGroup(false)};
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

		//Intermodal example
		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

		SwissRailRaptorConfigGroup srrConfig = new SwissRailRaptorConfigGroup();
		srrConfig.setUseIntermodalAccessEgress(true);
		srrConfig.setIntermodalAccessEgressModeSelection(SwissRailRaptorConfigGroup.IntermodalAccessEgressModeSelection.RandomSelectOneModePerRoutingRequestAndDirection);

		SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetCar = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
		paramSetCar.setMode(TransportMode.car);
		paramSetCar.setInitialSearchRadius(1000);
		paramSetCar.setMaxRadius(6000);
		paramSetCar.setSearchExtensionRadius(0.1);

		srrConfig.addIntermodalAccessEgress(paramSetCar);

		SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetBike = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
		paramSetBike.setMode("bicycle");
		paramSetBike.setInitialSearchRadius(1000);
		paramSetBike.setMaxRadius(3000);
		paramSetBike.setSearchExtensionRadius(0.1);
		srrConfig.addIntermodalAccessEgress(paramSetBike);

		SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetWalk = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
		paramSetWalk.setMode(TransportMode.walk);
		paramSetWalk.setInitialSearchRadius(1000);
		paramSetWalk.setMaxRadius(1000);
		paramSetWalk.setSearchExtensionRadius(0.1);
		srrConfig.addIntermodalAccessEgress(paramSetWalk);

		config.addModule(srrConfig);

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

}

