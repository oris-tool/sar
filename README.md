# Modeling and evaluation of Software Aging and Rejuvenation (SAR) models beyond the enabling restriction

This repository provides a Java library for modeling and evaluation of Software Aging and Rejuvenation (SAR) models beyond the enabling restriction. The approach is presented in the paper titled "Cost-Effective Software Rejuvenation Combining Time-Based and Condition-Based Policies", authored by Laura Carnevali, Marco Paolieri, Riccardo Reali, Leonardo Scommegna, and Enrico Vicario, submitted to the IEEE Transactions on Emerging Topics in Computing.

## Experimental reproducibility
To reproduce experiments illustrated in Sect.3 of the paper:
   - Run the main class: "/sar/src/main/java/org/oristool/timebased/results/BestPeriod.java", which evaluates the best period to use for models illustrated in Fig.3
   - Run the main class: "/sar/src/main/java/org/oristool/timebased/results/BestPeriodTransient.java", which evaluates the transients illustrated in Fig.5
   - Run the python script: "/sar/plots/python_scripts/time_based_plots.py", which will output Fig.4 and Figs.5 in the folder "/sar/plots/figures/timebased"


To reproduce experiments illustrated in Sect.4 of the paper:
   - Run the main class: "/sar/src/main/java/org/oristool/conditionbased/ConditionBasedSteadyState.java", which evaluates the steady-state of the interested rewards.
   - Run the python script: "/sar/plots/python_scripts/condition_based_plots.py", which will output Figs.7,8,9,10 in the folder "/sar/plots/figures/conditionbased"
   Note that evaluation of the transient of Fig.7 is reported as a vector in the script and not read from an external file. The vector values can be evaluated by running "/sar/src/main/java/org/oristool/conditionbased/Rej_n_samples.java"

## Installation

This repository provides a ready-to-use Maven project that you can easily import into an Eclipse workspace to start working with the [this library](https://github.com/oris-tool/sar/) (the version `2.0.0-SNAPSHOT` of the [Sirio library](https://github.com/oris-tool/sirio) is included as a Maven dependency). Just follow these steps:

1. **Install Java >= 11.** For Windows, you can download a [package from Oracle](https://www.oracle.com/java/technologies/downloads/#java11); for Linux, you can run `apt-get install openjdk-11-jdk`; for macOS, you can run `brew install --cask java`. 

2. **Download Eclipse.** The [Eclipse IDE for Java Developers](http://www.eclipse.org/downloads/eclipse-packages/) package is sufficient.

3. **Clone this project.** Inside Eclipse:
   - Select `File > Import > Maven > Check out Maven Projects from SCM` and click `Next`.
   - If the `SCM URL` dropbox is grayed out, click on `m2e Marketplace` and install `m2e-egit`. You will have to restart Eclipse.
   - As `SCM URL`, type: `git@github.com:oris-tool/sar.git` and click `Next` and then `Finish`.

## License

This library is released under the [GNU Affero General Public License v3.0](https://choosealicense.com/licenses/agpl-3.0).
