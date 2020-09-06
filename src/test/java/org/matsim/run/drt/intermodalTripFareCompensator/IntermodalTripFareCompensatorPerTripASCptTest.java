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

/**
 *
 */
package org.matsim.run.drt.intermodalTripFareCompensator;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura based on vsp-gleich
 */
public class IntermodalTripFareCompensatorPerTripASCptTest {

    /**
     * Test method for {@link IntermodalTripFareCompensatorPerTrip}.
     */
    @Test
    public void testIntermodalTripFareCompensatorPerTrip() {
    	
        Id<Person> personId1 = Id.createPersonId("p1");
        Id<Person> personId2 = Id.createPersonId("p2");

        Config config = ConfigUtils.createConfig();
		ModeParams modeParams = new ModeParams("pt");
		modeParams.setConstant(-1234);
		
		{
			ScoringParameterSet params = config.planCalcScore().getOrCreateScoringParameters("hobbits");
			params.addModeParams(modeParams);
			params.setMarginalUtilityOfMoney(0.5);
			
			config.planCalcScore().getScoringParametersPerSubpopulation().put("hobbits", params);
		}
		
		{
			ScoringParameterSet params = config.planCalcScore().getOrCreateScoringParameters("orks");
			params.addModeParams(modeParams);
			params.setMarginalUtilityOfMoney(0.25);
			
			config.planCalcScore().getScoringParametersPerSubpopulation().put("orks", params);
		}
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        Person person1 = scenario.getPopulation().getFactory().createPerson(personId1);
        person1.getAttributes().putAttribute("subpopulation", "hobbits");
        scenario.getPopulation().addPerson(person1);
        
        Person person2 = scenario.getPopulation().getFactory().createPerson(personId2);
        person2.getAttributes().putAttribute("subpopulation", "orks");
        scenario.getPopulation().addPerson(person2);
        
        IntermodalTripFareCompensatorConfigGroup compensatorConfig = new IntermodalTripFareCompensatorConfigGroup();
        compensatorConfig.setDrtModesAsString(TransportMode.drt + ",drt2");
        compensatorConfig.setPtModesAsString(TransportMode.pt);
        double compensationPerTrip = 1.0;
        compensatorConfig.setCompensationPerTrip(compensationPerTrip);
        
        config.addModule(compensatorConfig);

        EventsManager events = EventsUtils.createEventsManager();
        IntermodalTripFareCompensatorPerTripASCpt tfh = new IntermodalTripFareCompensatorPerTripASCpt(compensatorConfig, events, scenario);
        events.addHandler(tfh);
        
        Map<Id<Person>, Double> person2Fare = new HashMap<>();
        events.addHandler(new PersonMoneyEventHandler() {
            @Override
            public void handleEvent(PersonMoneyEvent event) {
            	if (!person2Fare.containsKey(event.getPersonId())) {
            		person2Fare.put(event.getPersonId(), event.getAmount());
            	} else {
            		person2Fare.put(event.getPersonId(), person2Fare.get(event.getPersonId()) + event.getAmount());
            	}
            }

            @Override
            public void reset(int iteration) {
            }
        });

        // test trip with drt mode but not intermodal
        events.processEvent(new PersonDepartureEvent(0.0, personId1, Id.createLinkId("12"), TransportMode.drt));
        events.processEvent(new ActivityStartEvent(1.0, personId1, Id.createLinkId("23"), Id.create("dummy", ActivityFacility.class), "work"));
        Assert.assertTrue("Compensation should be 0, but is not!", person2Fare.get(personId1) == null);
        
        // test intermodal trip without drt mode (only unrelated other mode)
        events.processEvent(new PersonDepartureEvent(2.0, personId1, Id.createLinkId("23"), TransportMode.car));
        events.processEvent(new PersonDepartureEvent(3.0, personId1, Id.createLinkId("34"), TransportMode.pt));
        
        // there should be no compensation so far
        Assert.assertTrue("Compensation should be 0, but is not!", person2Fare.get(personId1) == null);

        // test drt after pt leg
        events.processEvent(new PersonDepartureEvent(4.0, personId1, Id.createLinkId("45"), TransportMode.drt));
        
        // compensation paid once
		Assert.assertEquals("After a pt and a drt leg compensation is wrong.", 1234/0.5, person2Fare.get(personId1), MatsimTestUtils.EPSILON);
		
		// some distraction, nothing should change
        events.processEvent(new PersonDepartureEvent(4.0, personId1, Id.createLinkId("45"), TransportMode.pt));
		
	    // compensation paid once
		Assert.assertEquals("After a pt and a drt leg compensation is wrong.", 1234/0.5, person2Fare.get(personId1), MatsimTestUtils.EPSILON);

		
		// end trip
        events.processEvent(new ActivityStartEvent(5.0, personId1, Id.createLinkId("23"), Id.create("dummy", ActivityFacility.class), "blub"));
        
        // test drt2 before pt with interaction activity in between
        events.processEvent(new PersonDepartureEvent(6.0, personId1, Id.createLinkId("45"), "drt2"));
        events.processEvent(new ActivityStartEvent(7.0, personId1, Id.createLinkId("56"), Id.create("dummy", ActivityFacility.class), "drt interaction"));
        events.processEvent(new PersonDepartureEvent(8.0, personId1, Id.createLinkId("56"), TransportMode.pt));
        
        // compensation paid second time (second trip)
		Assert.assertEquals("After a drt2 and a pt leg compensation is wrong", 2 * 1234/0.5, person2Fare.get(personId1), MatsimTestUtils.EPSILON);
		
		// some distraction, nothing should change
        events.processEvent(new PersonDepartureEvent(4.0, personId1, Id.createLinkId("45"), TransportMode.pt));
		Assert.assertEquals("After a drt2 and a pt leg compensation is wrong.", 2 * 1234/0.5, person2Fare.get(personId1), MatsimTestUtils.EPSILON);
        
        events.processEvent(new PersonDepartureEvent(4.0, personId1, Id.createLinkId("67"), TransportMode.drt));
        
        // compensation paid third time (second trip)
		Assert.assertEquals("After another drt leg compensation should be paid a 3nd time, but is not", 3 * 1234/0.5, person2Fare.get(personId1), MatsimTestUtils.EPSILON);

        // test drt before pt with interaction activity in between at other agent who did not use pt before
        events.processEvent(new PersonDepartureEvent(6.0, personId2, Id.createLinkId("45"), "drt"));
        events.processEvent(new ActivityStartEvent(7.0, personId2, Id.createLinkId("56"), Id.create("dummy", ActivityFacility.class), "drt interaction"));
        events.processEvent(new PersonDepartureEvent(8.0, personId2, Id.createLinkId("56"), TransportMode.pt));
		Assert.assertEquals("After a pt and a drt leg compensation is wrong.", 1 * 1234/0.25, person2Fare.get(personId2), MatsimTestUtils.EPSILON);
		
    }


}
