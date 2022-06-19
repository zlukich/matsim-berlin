package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

import java.util.Arrays;

public class LinkEventHandler implements LinkEnterEventHandler {

    private int counter = 0;
    private static final Id<Link> linkOfInterest = Id.createLinkId("122077");

    public int GetCounter(){
        return counter;
    }

    public int ids_of_links_to_be_changed[] = {122077,
            32393,
            152714,46981,122763,128554,10911,85556,123234,150027,68877,68882,68875,128376,104400,92405,61167,68028,
            63665,
            15713,
            76115,
            122445,
            27939,
            153398,

            154994,
            5670,
            82261,

            63748,
            104386,
            69152,
    };

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        //System.out.println(linkEnterEvent.getLinkId());
        int link_id = linkEnterEvent.getLinkId().index();
        if(linkEnterEvent.getLinkId().equals(linkOfInterest)){
            System.out.println(linkEnterEvent.getLinkId());
            counter++;
        }
    }

    @Override
    public void reset(int iteration) {
        LinkEnterEventHandler.super.reset(iteration);
    }
}
