package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;

import java.util.HashSet;

public class PrepareNetwork {

    public static void main(String[] args) {

        Network network = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");


        Integer ids_of_links_to_be_changed[] = {
                122077,
                32393,
                152714,
                46981,
                122763,
                128554,
                10911,
                //
                //85556,
                //123234,
                150027,
                68877,
                68882,
                68875,
                128376,
                //
                //104400,
                92405,
                61167,
                //
                        //68028,
                63665,
                15713,
                76115,
                122445,
                27939,
                153398,
                154994,
                //
                        //5670,
                82261,
                //
                        //63748,
                104386,
                69152,
        };
        for(int i: ids_of_links_to_be_changed)
        {
            Link l = network.getLinks().get(Id.createLinkId(i));
            HashSet<String> newModes = new HashSet<>();
            newModes.add("freight");
            l.setAllowedModes(newModes);
        }
        //MultimodalNetworkCleaner networkCleaner = new MultimodalNetworkCleaner(network);
        //NetworkCleaner cleaner = new NetworkCleaner();
        NetworkUtils.writeNetwork(network,"scenarios/berlin-v5.5-1pct/input/result.xml");
        //cleaner.run("temp.xml","result.xml");

        //Network resultNetwork = NetworkUtils.readNetwork("result.xml");
    }
}
