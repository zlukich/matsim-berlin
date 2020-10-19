package org.matsim.run.drt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams.RebalancingTargetCalculatorType;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams.ZonalDemandEstimatorType;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class RunDrtRebalancingStrategyComparison {
	private static final Logger log = Logger.getLogger(RunDrtRebalancingStrategyComparison.class);

	private static final String[] REBLANCE_STRATEGIES = { "Feedforward", "PlusOne", "AdaptiveRealTime", "NoRebalance" };
	private static final String[] FLEET_SIZES = { "2000", "3000", "4000", "5000" };
	private static final String DEFAULT_CONFIG = "./input/berlin-drt-v5.5-10pct-BQS.config.xml";

	private static final String OUTPUT_DIRECTORY_HEADING = "./output/RebalanceStrategyComparison/";
	private static final String OUTPUT_DIRECTORY_ENDING = "vehicles";

	private static final String VEHICLE_FILE_HEADING = " https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries"
			+ "/de/berlin/projects/avoev/berlin-drt-v5.5-10pct/input/berlin-drt-v5.5.drt-";
	private static final String VEHICLE_FILE_ENDING = "vehicles-4seats.xml.gz";

	private static final int REBALANCE_INTERVAL = 300;

	public static void main(String[] args) {
		if (args.length == 0) {
			args = new String[] { DEFAULT_CONFIG };
		}

		for (int i = 0; i < REBLANCE_STRATEGIES.length; i++) {
			for (int j = 0; j < FLEET_SIZES.length; j++) {
				String RebalancdStrategy = REBLANCE_STRATEGIES[i];
				String fleetSize = FLEET_SIZES[j];

				Config config = RunDrtOpenBerlinScenario.prepareConfig(args);

				// Modify config
				// Update output directory
				config.controler().setOutputDirectory(
						OUTPUT_DIRECTORY_HEADING + RebalancdStrategy + "-" + fleetSize + OUTPUT_DIRECTORY_ENDING);
				for (DrtConfigGroup drtConfigGroup : MultiModeDrtConfigGroup.get(config).getModalElements()) {
					// Set Fleet size (i.e. set vehicles file)
					drtConfigGroup.setVehiclesFile(VEHICLE_FILE_HEADING + fleetSize + VEHICLE_FILE_ENDING);

					RebalancingParams rebalancingParams = drtConfigGroup.getRebalancingParams()
							.orElse(new RebalancingParams());
					rebalancingParams.setInterval(REBALANCE_INTERVAL);
					rebalancingParams.setMinServiceTime(3600);
					rebalancingParams.setMaxTimeBeforeIdle(900);

					// Set rebalancing Strategy
					switch (RebalancdStrategy) {
					case "Feedforward":
						log.info("Feedforward rebalancing strategy is used");
						prepareFeedforwardStrategy(rebalancingParams);
						break;
					case "PlusOne":
						log.info("Plus One rebalancing strategy is used");
						preparePlusOneStrategy(rebalancingParams);
						break;
					case "AdaptiveRealTime":
						log.info("Adaptive Real Time rebalancing strategy is used");
						prepareAdaptiveRealTimeStrategy(rebalancingParams);
						break;
					default:
						log.info("No rebalancing strategy is used");
						drtConfigGroup.removeParameterSet(rebalancingParams);
						break;
					}
				}

				Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config);
				Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario);
				controler.run();
			}

		}
	}

	private static void prepareAdaptiveRealTimeStrategy(RebalancingParams rebalancingParams) {
		MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
		minCostFlowRebalancingStrategyParams.setRebalancingTargetCalculatorType(
				RebalancingTargetCalculatorType.EqualRebalancableVehicleDistribution);
		minCostFlowRebalancingStrategyParams
				.setZonalDemandEstimatorType(ZonalDemandEstimatorType.PreviousIterationDemand);
		minCostFlowRebalancingStrategyParams.setTargetAlpha(1);
		minCostFlowRebalancingStrategyParams.setTargetBeta(0);
		minCostFlowRebalancingStrategyParams.setDemandEstimationPeriod(3600);
		rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);
	}

	private static void preparePlusOneStrategy(RebalancingParams rebalancingParams) {
		rebalancingParams.addParameterSet(new PlusOneRebalancingStrategyParams());
	}

	private static void prepareFeedforwardStrategy(RebalancingParams rebalancingParams) {
		FeedforwardRebalancingStrategyParams feedforwardRebalancingStrategyParams = new FeedforwardRebalancingStrategyParams();
		feedforwardRebalancingStrategyParams.setFeedbackSwitch(true);
		feedforwardRebalancingStrategyParams.setFeedforwardSignalLead(300);
		rebalancingParams.addParameterSet(feedforwardRebalancingStrategyParams);
	}
}
