package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.vehicles.Vehicle;
import scala.Int;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LinkEventHandler  implements LinkEnterEventHandler, PersonEntersVehicleEventHandler, ActivityStartEventHandler {

    private int counter = 0;
    public String output_file = "smth.xml";
    public java.io.FileWriter fw;

    private final Map<Id<Vehicle>,Id<Person>> personsEnteringVehicle = new HashMap<>();
    private final Map<Id<Person>, Integer> personsEnteringLink = new HashMap<>();
    private final Map<Id<Vehicle>, Integer> vehicleEnteringLink = new HashMap<>();



    public int GetCounter(){
        return counter;
    }
    public void ResetCounter(){counter = 0;}


    public int ids_of_links_to_be_changed[] = {122077,
            32393,
            152714,
            46981,
            122763,
            128554,
            10911,
            //85556,
            //123234,s
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

    public Map<Id<Person>,Integer> getPersons(){
        return personsEnteringLink;
    }
    public Map<Id<Vehicle>,Integer> getVehicles(){
        return vehicleEnteringLink;
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent personEntersVehicleEvent) {

        var personId = personEntersVehicleEvent.getPersonId();
        personsEnteringVehicle.put(personEntersVehicleEvent.getVehicleId(),personId);
    }



    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        //System.out.println(linkEnterEvent.getLinkId());
        int link_id = linkEnterEvent.getLinkId().index();
        var str = linkEnterEvent.toString();
        for(int i: ids_of_links_to_be_changed){
            if(linkEnterEvent.getLinkId().equals(Id.createLinkId(i))){
                var personId = personsEnteringVehicle.get(linkEnterEvent.getVehicleId());
                personsEnteringLink.put(personId,personId.index());
                vehicleEnteringLink.put(linkEnterEvent.getVehicleId(),linkEnterEvent.getVehicleId().index());
                counter++;
            }

        }
    }

    @Override
    public void reset(int iteration) {
        //LinkEnterEventHandler.super.reset(iteration);
    }


    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {
        //activityStartEvent.
    }

}
