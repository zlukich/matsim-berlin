/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.run.drt.intermodalTripFareCompensator;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.StageActivityTypeIdentifier;

import com.google.inject.Inject;

/**
 * 
 * This class assumes there can be at most 1 drt leg before a pt leg in a trip and at most 1 drt leg after. Further drt
 * legs will not be compensated! This is for performance reasons (set faster than Map)
 * 
 * @author ikaddoura based on vsp-gleich
 *
 */
public class IntermodalTripFareCompensatorPerTripASCpt implements PersonDepartureEventHandler, ActivityStartEventHandler {
	private static final Logger log = Logger.getLogger(IntermodalTripFareCompensatorPerTripASCpt.class);
	private int logCnt = 0;
	
	@Inject private EventsManager events;
	@Inject private Scenario scenario;
	private Set<Id<Person>> personsOnPtTrip = new HashSet<>();
	private Set<Id<Person>> personsOnDrtTrip = new HashSet<>();
	private Set<String> drtModes;
	private Set<String> ptModes;
	
	IntermodalTripFareCompensatorPerTripASCpt(IntermodalTripFareCompensatorConfigGroup intermodalFareConfigGroup) {
		this.drtModes = intermodalFareConfigGroup.getDrtModes();
		this.ptModes = intermodalFareConfigGroup.getPtModes();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		
		Person person = scenario.getPopulation().getPersons().get(event.getPersonId());
		
		if (person == null) {
			// person not in population, probably a transit driver
		} else {
			// compute the compensation for each agent depending on subpopulation-specific parameters
			
			if (person.getAttributes().getAttribute("subpopulation") == null) {
				if (logCnt <= 5) {
					log.warn("Person doesn't have the subpopulation attribute. Can't compensate the intermodal trip for this agent: " + person.toString());
					logCnt++;
					if (logCnt == 5) log.warn("Further warnings of this type will not be printed.");
				}
				
			} else {
				String subpopulation = (String) person.getAttributes().getAttribute("subpopulation");
				double utilityConstant = scenario.getConfig().planCalcScore().getScoringParameters(subpopulation).getModes().get(TransportMode.pt).getConstant();
				double marginalUtilityOfMoney = scenario.getConfig().planCalcScore().getScoringParameters(subpopulation).getMarginalUtilityOfMoney();
				double compensation = -1. * utilityConstant / marginalUtilityOfMoney;
				
				if (ptModes.contains(event.getLegMode())) {
					personsOnPtTrip.add(event.getPersonId());
					
		 			if (personsOnDrtTrip.contains(event.getPersonId())) {
						// drt before pt case: compensate here when pt leg follows
						compensate(event.getTime(), event.getPersonId(), compensation);
						personsOnDrtTrip.remove(event.getPersonId());
					}
				}
				if (drtModes.contains(event.getLegMode())) {
					if (personsOnPtTrip.contains(event.getPersonId())) {
						// drt after pt case: compensate immediately
						compensate(event.getTime(), event.getPersonId(), compensation);
					} else {
						// drt before pt case: compensate later _if_ pt leg follows
						personsOnDrtTrip.add(event.getPersonId());
					}
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!StageActivityTypeIdentifier.isStageActivity(event.getActType())) {
			// reset after trip has finished
			personsOnPtTrip.remove(event.getPersonId());
			personsOnDrtTrip.remove(event.getPersonId());
		}
		
	}
	
	private void compensate(double time, Id<Person> agentId, double amount) {
		events.processEvent(new PersonMoneyEvent(time, agentId, amount));
	}

    @Override
    public void reset(int iteration) {
		// reset for stuck agents after mobsim
    	personsOnPtTrip.clear();
    	personsOnDrtTrip.clear();
    }
	
}
