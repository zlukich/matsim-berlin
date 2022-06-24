package org.matsim.run;

import org.apache.commons.csv.CSVFormat;
import org.matsim.analysis.LinkEventHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.EventsUtils;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LinkUsageAnalysis {
    private static final String baseFile = "C:\\Users\\zlukich\\Desktop\\base.xml.gz";
    private static final String policyFile = "C:\\Users\\zlukich\\Desktop\\policy_100.xml.gz";
    private static final String outFile = "C:\\Users\\zlukich\\Desktop\\smth.csv";

    public static void main(String[] args) {

        var manager = EventsUtils.createEventsManager();
        var linkHandler = new LinkEventHandler();
       // var simpleHandler = new LinkEventHandler();
        //manager.addHandler(simpleHandler);
        manager.addHandler(linkHandler);

        //EventsUtils.readEvents(manager, baseFile);

        //var base_counter = linkHandler.GetCounter();

        linkHandler.ResetCounter();

        EventsUtils.readEvents(manager,policyFile);


        var policy_counter = linkHandler.GetCounter();
//        try (var writer = Files.newBufferedWriter(Paths.get(outFile)); var printer = CSVFormat.DEFAULT.withDelimiter(';').withHeader("Base","Policy").print(writer)) {
//
//            printer.printRecord(base_counter,policy_counter);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        System.out.println(linkHandler.GetCounter());
        var personIds = linkHandler.getPersons().keySet();
        var vehicleIds = linkHandler.getVehicles().keySet();
        System.out.format("Insgesamt %d persons that enters the link \n",personIds.size());
        for (Id<Person> id:personIds) {
            //System.out.println(id);
        }
        System.out.format("Insgesamt %d vehicles that enters the link \n",vehicleIds.size());
        for (Id<Vehicle> id:vehicleIds) {
            //System.out.println(id);
        }


    }
}
