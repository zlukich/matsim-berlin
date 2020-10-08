package org.matsim.run.drt;

import org.matsim.contrib.drt.run.RunDrtScenario;

import com.google.common.base.Preconditions;

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
		Preconditions.checkArgument(args.length == 1,
				"RunDrtScenario needs one argument: path to the configuration file");
		RunDrtScenario.run(args[0], false);
	}
}
