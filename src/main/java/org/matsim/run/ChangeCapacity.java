package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;

public class ChangeCapacity {
    public static void main(String[] args){
        var network = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz");

        var link = network.getLinks().get(Id.createLinkId(122077));
        link.setCapacity(0.0001);
        link.setFreespeed(1);
        NetworkUtils.writeNetwork(network,"C:\\Users\\zlukich\\Desktop\\altered_network.xml.gz");

    }
}
