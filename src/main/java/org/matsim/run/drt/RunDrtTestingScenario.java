package org.matsim.run.drt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * This class starts a testing simulation run with DRT in the Mielec scenario.
 * 
 * @author Chengqi Lu
 */
public class RunDrtTestingScenario {
	public static void main(String[] args) {
		if (args.length == 0) {
			args = new String[] { "C:\\Users\\cluac\\MATSimScenarios\\Mielec\\mielec_drt_config.xml" };
		}
		
		Config config = ConfigUtils.loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

		// Setting the rebalancing strategy (if this part is enabled, the corresponding part in the config file has to be removed)
		for (DrtConfigGroup drtConfigGroup : multiModeDrtConfig.getModalElements()) {
			RebalancingParams rebalancingParams = drtConfigGroup.getRebalancingParams()
					.orElse(new RebalancingParams());
			rebalancingParams.setInterval(300);
			rebalancingParams.setMinServiceTime(3600);
			rebalancingParams.setMaxTimeBeforeIdle(900);
			
			FeedforwardRebalancingStrategyParams feedforwardRebalancingStrategyParams = new FeedforwardRebalancingStrategyParams();
			feedforwardRebalancingStrategyParams.setFeedbackSwitch(true);
			feedforwardRebalancingStrategyParams.setFeedforwardSignalLead(300);
			rebalancingParams.addParameterSet(feedforwardRebalancingStrategyParams);
		} 
		
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation()
				.getFactory()
				.getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));
		
		controler.run();
		
	}
}
