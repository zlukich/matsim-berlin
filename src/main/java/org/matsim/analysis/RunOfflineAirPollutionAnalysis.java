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

package org.matsim.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
* @author ikaddoura
*/

public class RunOfflineAirPollutionAnalysis {
	private static final Logger log = Logger.getLogger(RunOfflineAirPollutionAnalysis.class);
	
	public static void main(String[] args) {
		
		if ( args.length==0 ) {
			args = new String[] {"input/berlin-v5.5-1pct.config_2.xml"};
		}
		
		double electricVehicleShare = 0.01;
		
		Config config = ConfigUtils.loadConfig(args, new AnalysisConfigGroup(), new EmissionsConfigGroup());
		
		AnalysisConfigGroup aConfigGroup = (AnalysisConfigGroup) config.getModules().get(AnalysisConfigGroup.GROUP_NAME);
		
		config.vehicles().setVehiclesFile(aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_vehicles.xml.gz");
		config.network().setInputFile(aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_network.xml.gz");
		config.transit().setTransitScheduleFile(aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile(aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_transitVehicles.xml.gz");
		config.global().setCoordinateSystem("GK4");
		config.plans().setInputFile(null);
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);
		
		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.directlyTryAverageTable);
		eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
		eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);
		
		String analysisOutputDirectory = config.controler().getOutputDirectory();
		if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
		
		File folder = new File(analysisOutputDirectory);			
		folder.mkdirs();
		
		final String emissionEventOutputFile = analysisOutputDirectory + aConfigGroup.getRunId() + ".emission.events.offline.xml.gz";
		final String eventsFile = aConfigGroup.getRunDirectory() + aConfigGroup.getRunId() + ".output_events.xml.gz";
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// network
		for (Link link : scenario.getNetwork().getLinks().values()) {

			double freespeed = Double.NaN;

			if (link.getFreespeed() <= 13.888889) {
				freespeed = link.getFreespeed() * 2;
				// for non motorway roads, the free speed level was reduced
			} else {
				freespeed = link.getFreespeed();
				// for motorways, the original speed levels seems ok.
			}
			
			if(freespeed <= 8.333333333){ //30kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
			} else if(freespeed <= 11.111111111){ //40kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
			} else if(freespeed <= 13.888888889){ //50kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 16.666666667){ //60kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 19.444444444){ //70kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
			} else if(freespeed <= 22.222222222){ //80kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
			} else if(freespeed > 22.222222222){ //faster
				link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
			} else{
				throw new RuntimeException("Link not considered...");
			}			
		}
		
		// vehicle types
		
		// car vehicles
		
		Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
		VehicleType carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
		
		EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory( carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleUtils.setHbefaTechnology( carEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( carEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( carEngineInformation, "average" );
		
		// freight vehicles

		Id<VehicleType> freightVehicleTypeId = Id.create("freight", VehicleType.class);
		VehicleType freightVehicleType = scenario.getVehicles().getVehicleTypes().get(freightVehicleTypeId);
		
		EngineInformation freightEngineInformation = freightVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory( freightEngineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
//		VehicleUtils.setHbefaVehicleCategory( freightEngineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
		VehicleUtils.setHbefaTechnology( freightEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( freightEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( freightEngineInformation, "average" );

		// electric vehicles
		Id<VehicleType> electricVehicleTypeId = Id.create("ev", VehicleType.class);
		VehicleType electricVehicleType = scenario.getVehicles().getFactory().createVehicleType(electricVehicleTypeId);
		scenario.getVehicles().addVehicleType(electricVehicleType);
		
		EngineInformation electricEngineInformation = electricVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory( electricEngineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
		VehicleUtils.setHbefaTechnology( electricEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( electricEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( electricEngineInformation, "average" );
		
		// public transit vehicles should be considered as non-hbefa vehicles
		for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();
			// TODO: Check! Is this a zero emission vehicle?!
			VehicleUtils.setHbefaVehicleCategory( engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
			VehicleUtils.setHbefaTechnology( engineInformation, "average" );
			VehicleUtils.setHbefaSizeClass( engineInformation, "average" );
			VehicleUtils.setHbefaEmissionsConcept( engineInformation, "average" );			
		}
		
		List<Id<Vehicle>> vehiclesToChangeToElectric = new ArrayList<>();

		final Random rnd = MatsimRandom.getLocalInstance();

		int totalVehiclesCounter = 0;
		int carCounter = 0;
		int evCounter = 0;
		// randomly change some vehicle types
		for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
			totalVehiclesCounter++;
			if (vehicle.getId().toString().contains("freight")) {
				// some freight vehicles have the type "car"
			} else if (vehicle.getType().getId().toString().equals(carVehicleTypeId.toString())) {
				carCounter++;
				if (rnd.nextDouble() < electricVehicleShare) {
					evCounter++;
					vehiclesToChangeToElectric.add(vehicle.getId());
				}
			} else {
				// ignore all other vehicles
			}
		}
				
		for (Id<Vehicle> id : vehiclesToChangeToElectric) {
			scenario.getVehicles().removeVehicle(id);
			Vehicle vehicleNew = scenario.getVehicles().getFactory().createVehicle(id, electricVehicleType);
			scenario.getVehicles().addVehicle(vehicleNew);
			log.info("Type for vehicle " + id + " changed to electric.");
		}
		
		// the following is copy paste from the example...
		
        EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector(config, module);

        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        EmissionsOnLinkHandler emissionsEventHandler = new EmissionsOnLinkHandler();
		eventsManager.addHandler(emissionsEventHandler);
        
        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);
        eventsManager.finishProcessing();

        emissionEventWriter.closeFile();
        
        log.info("Total number of vehicles: " + totalVehiclesCounter);
		log.info("Number of car vehicles: " + carCounter);
		log.info("Number of electric vehicles: " + evCounter);

        log.info("Emission analysis completed.");
        
        log.info("Writing output...");   
        
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(analysisOutputDirectory + aConfigGroup.getRunId() + ".emissionsPerLink"), CSVFormat.DEFAULT)) {

            // write header
            printer.printRecord("LinkID", Pollutant.CO2_TOTAL, Pollutant.NOx, Pollutant.PM, Pollutant.SO2);

            // write values
            for (Id<Link> linkId : emissionsEventHandler.getLink2pollutants().keySet()) {
            	Map<Pollutant, Double> pollutants = emissionsEventHandler.getLink2pollutants().get(linkId);
                printer.printRecord(linkId, pollutants.get(Pollutant.CO2_TOTAL), pollutants.get(Pollutant.NOx), pollutants.get(Pollutant.PM), pollutants.get(Pollutant.SO2));
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
	}

}

