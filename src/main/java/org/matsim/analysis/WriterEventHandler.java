package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.matsim.api.core.v01.population.Person;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
public class WriterEventHandler implements LinkEnterEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler {
    public String output_file = "smth.xml";
    public String result_to_file = "";
    public java.io.FileWriter fw;
    public Map<Id<Person>,Integer> Map;
    public WriterEventHandler(Map<Id<Person>,Integer> map){
        result_to_file+="<events version='1.0'>";
        Map = map;
        try {
            fw = new FileWriter(output_file);

            fw.write("<events version='1.0'>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void saveToFile(){
        result_to_file+="</events>";
        try{
            fw.write("</events>");
            stringToDom(result_to_file);
        }
        catch (IOException e){

        }
    }
    public void stringToDom(String xmlSource)
            throws IOException {
        fw.close();
    }


    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {
        if(Map.containsKey(activityEndEvent.getPersonId())){
            try {

                fw.write("\n");
                activityEndEvent.setTime(0);
                fw.write(activityEndEvent.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {
        if(Map.containsKey(activityStartEvent.getPersonId())){
            try {

                fw.write("\n");
                activityStartEvent.setTime(0);
                fw.write(activityStartEvent.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        if(Map.containsKey(linkEnterEvent.getVehicleId())){
            try {

                fw.write("\n");

                fw.write(linkEnterEvent.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent personEntersVehicleEvent) {
        if(Map.containsKey(personEntersVehicleEvent.getPersonId())){
            try {

                fw.write("\n");
                fw.write(personEntersVehicleEvent.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
