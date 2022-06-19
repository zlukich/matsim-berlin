package org.matsim.run;

import org.matsim.analysis.LinkEventHandler;
import org.matsim.core.events.EventsUtils;

public class LinkUsageAnalysis {
    private static final String eventsFile = "C:\\Users\\zlukich\\Desktop\\policy_5_iteration.xml.gz";
    private static final String outFile = "C:\\Users\\Janekdererste\\Projects\\matsim-serengeti-park-hodenhagen\\scenarios\\serengeti-park-v1.0\\output\\output-serengeti-park-v1.0-run1\\serengeti-park-v1.0.run1.output_link_count.csv";

    public static void main(String[] args) {

        var manager = EventsUtils.createEventsManager();
        var linkHandler = new LinkEventHandler();
        var simpleHandler = new LinkEventHandler();
        manager.addHandler(simpleHandler);
        manager.addHandler(linkHandler);

        EventsUtils.readEvents(manager, eventsFile);

        System.out.println(linkHandler.GetCounter());


    }
}
