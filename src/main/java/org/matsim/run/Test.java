package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

public class Test {

    public static void main(String[] args){
        var population = PopulationUtils.readPopulation("C:\\Users\\zlukich\\Desktop\\matsim-berlin\\scenarios\\berlin-v5.5-1pct\\output-berlin-v5.5-1pct\\berlin-v5.5-1pct.output_plans.xml.gz");
        var persons = population.getPersons();
        int  id = 0;
    }
}
