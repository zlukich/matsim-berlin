package org.matsim.run;

import jdk.jfr.Event;
import org.apache.commons.csv.CSVFormat;
import org.matsim.analysis.LinkEventHandler;
import org.matsim.analysis.WriterEventHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LinkUsageAnalysis {
    private static final String baseFile = "C:\\Users\\zlukich\\Desktop\\base.xml.gz";
    private static final String policyFile = "C:\\Users\\zlukich\\Desktop\\policy_500.xml.gz";
    private static final String outFile = "C:\\Users\\zlukich\\Desktop\\smth.csv";

    public static void main(String[] args) {

        var manager = EventsUtils.createEventsManager();
        var linkHandler = new LinkEventHandler();

        manager.addHandler(linkHandler);

//        EventsUtils.readEvents(manager, baseFile);
//        var base_counter = linkHandler.GetCounter();
//        System.out.println(base_counter);

        linkHandler.ResetCounter();

        EventsUtils.readEvents(manager,policyFile);


        var policy_counter = linkHandler.GetCounter();
//

//        System.out.println(linkHandler.GetCounter());
        var personIds = linkHandler.getPersons().keySet();
        try (var writer = Files.newBufferedWriter(Paths.get(outFile)); var printer = CSVFormat.DEFAULT.withDelimiter(';').withHeader("AgentId").print(writer)) {

            for (Id<Person> id :personIds
            ) {
                printer.printRecord(id.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        var vehicleIds = linkHandler.getVehicles().keySet();
        System.out.format("Insgesamt %d persons that enters the link \n",personIds.size());
        for (Id<Person> id:personIds) {
            System.out.println(id);
        }
        System.out.format("Insgesamt %d vehicles that enters the link \n",vehicleIds.size());
        for (Id<Vehicle> id:vehicleIds) {
            System.out.println(id);
        }

        var writerManager = EventsUtils.createEventsManager();
        var writerHandler = new WriterEventHandler(linkHandler.getPersons());
        writerManager.addHandler(writerHandler);
        EventsUtils.readEvents(writerManager,policyFile);
        writerHandler.saveToFile();





    }
}
