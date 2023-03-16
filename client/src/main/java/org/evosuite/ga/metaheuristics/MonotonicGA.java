/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.ga.metaheuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.CoverageProgressGetter;
import org.evosuite.Properties;
import org.evosuite.StatisticChecker;
import org.evosuite.TimeController;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.FitnessReplacementFunction;
import org.evosuite.ga.ReplacementFunction;
import org.evosuite.result.BranchInfo;
import org.evosuite.result.seedexpr.BranchCoveringEvent;
import org.evosuite.result.seedexpr.EventSequence;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.factories.RandomLengthTestFactory;
import org.evosuite.testcase.synthesizer.TestCaseLegitimizer;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.DistributionUtil;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of steady state GA
 * 
 * @author Gordon Fraser
 */
public class MonotonicGA<T extends Chromosome> extends GeneticAlgorithm<T> implements Hybridable{

	private static final long serialVersionUID = 7846967347821123201L;

	protected ReplacementFunction replacementFunction;

	private final Logger logger = LoggerFactory.getLogger(MonotonicGA.class);

	/**
	 * Constructor
	 * 
	 * @param factory a {@link org.evosuite.ga.ChromosomeFactory} object.
	 */
	public MonotonicGA(ChromosomeFactory<T> factory) {
		super(factory);

		setReplacementFunction(new FitnessReplacementFunction());
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<TestChromosome> getSeeds(){
		return (List<TestChromosome>) this.getPopulation();
	}
	
	/**
	 * <p>
	 * keepOffspring
	 * </p>
	 * 
	 * @param parent1    a {@link org.evosuite.ga.Chromosome} object.
	 * @param parent2    a {@link org.evosuite.ga.Chromosome} object.
	 * @param offspring1 a {@link org.evosuite.ga.Chromosome} object.
	 * @param offspring2 a {@link org.evosuite.ga.Chromosome} object.
	 * @return a boolean.
	 */
	protected boolean keepOffspring(Chromosome parent1, Chromosome parent2, Chromosome offspring1,
			Chromosome offspring2) {
		return replacementFunction.keepOffspring(parent1, parent2, offspring1, offspring2);
	}

	private List<T> newGeneratedIndividuals = new ArrayList<>();

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	protected void evolve() {
		newGeneratedIndividuals.clear();
		List<T> newGeneration = new ArrayList<T>();

		T bestIndividual = getBestIndividual();
		BranchCoverageFactory branchFactory = new BranchCoverageFactory();
		List<BranchCoverageTestFitness> branchGoals = branchFactory.getCoverageGoals();
		Set<BranchCoverageGoal> uncoveredGoals = getUncoveredBranches(bestIndividual, branchGoals);
		
		RandomLengthTestFactory.workingBranch4ObjectGraph = null; 
		if(uncoveredGoals != null && !uncoveredGoals.isEmpty()) {
			RandomLengthTestFactory.workingBranch4ObjectGraph = Randomness.choice(uncoveredGoals).getBranch();
		}
		
		// Elitism
		logger.debug("Elitism");
		newGeneration.addAll(elitism());

		// Add random elements
		// new_generation.addAll(randomism());

		while (!isNextPopulationFull(newGeneration) && !isFinished()) {
			logger.debug("Generating offspring");

			T parent1 = selectionFunction.select(population);
			T parent2;
			if (Properties.HEADLESS_CHICKEN_TEST)
				parent2 = newRandomIndividual(); // crossover with new random
													// individual
			else
				parent2 = selectionFunction.select(population); // crossover
																// with existing
																// individual
//			t00 = System.currentTimeMillis();
			T offspring1 = (T) parent1.clone();
			T offspring2 = (T) parent2.clone();
//			t11 = System.currentTimeMillis();
//			logger.warn("- clone time 1" + (t11-t00));

//			try {
//				// Crossover
//				if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
//					crossoverFunction.crossOver(offspring1, offspring2);
//				}
//
//			} catch (ConstructionFailedException e) {
//				logger.info("CrossOver failed");
//				continue;
//			}

			// Mutation
//			t00 = System.currentTimeMillis();
			notifyMutation(offspring1);
			offspring1.mutate();
			notifyMutation(offspring2);
			offspring2.mutate();

			if (offspring1.isChanged()) {
				if(Properties.ENABLE_TRACEING_EVENT) {
					BranchCoveringEvent e = EventSequence.deriveCoveredBranch(offspring1, parent1, uncoveredGoals);
					EventSequence.addEvent(e);
				}
				
				offspring1.updateAge(currentIteration);
			}
			if (offspring2.isChanged()) {
				if(Properties.ENABLE_TRACEING_EVENT) {
					BranchCoveringEvent e = EventSequence.deriveCoveredBranch(offspring1, parent1, uncoveredGoals);
					EventSequence.addEvent(e);
				}
				
				offspring2.updateAge(currentIteration);
			}
//			t11 = System.currentTimeMillis();
//			logger.warn("- mutation time 1" + (t11-t00));

			newGeneratedIndividuals.add(offspring1);
			newGeneratedIndividuals.add(offspring2);

//			t00 = System.currentTimeMillis();
			// The two offspring replace the parents if and only if one of
			// the offspring is not worse than the best parent.
			for (FitnessFunction<T> fitnessFunction : fitnessFunctions) {
				// double d1_ = fitnessFunction.getFitness(parent1);
				double d1 = fitnessFunction.getFitness(offspring1);
				// if(d1<d1_ && d1==0){
				// System.currentTimeMillis();
				// }

				notifyEvaluation(offspring1);
				// double d2_ = fitnessFunction.getFitness(parent2);
				double d2 = fitnessFunction.getFitness(offspring2);
				// if(d2<d2_ && d2==0){
				// System.currentTimeMillis();
				// }

				notifyEvaluation(offspring2);
			}
//			t11 = System.currentTimeMillis();
//			logger.warn("- running time 1" + (t11-t00));

			if (keepOffspring(parent1, parent2, offspring1, offspring2)) {
				logger.debug("Keeping offspring");

				// Reject offspring straight away if it's too long
				int rejected = 0;
				if (isTooLong(offspring1) || offspring1.size() == 0) {
					rejected++;
				} else {
					// if(Properties.ADAPTIVE_LOCAL_SEARCH ==
					// AdaptiveLocalSearchTarget.ALL)
					// applyAdaptiveLocalSearch(offspring1);
					newGeneration.add(offspring1);
				}

				if (isTooLong(offspring2) || offspring2.size() == 0) {
					rejected++;
				} else {
					// if(Properties.ADAPTIVE_LOCAL_SEARCH ==
					// AdaptiveLocalSearchTarget.ALL)
					// applyAdaptiveLocalSearch(offspring2);
					newGeneration.add(offspring2);
				}

				if (rejected == 1)
					newGeneration.add(Randomness.choice(parent1, parent2));
				else if (rejected == 2) {
					newGeneration.add(parent1);
					newGeneration.add(parent2);
				}
			} else {
				logger.debug("Keeping parents");
				newGeneration.add(parent1);
				newGeneration.add(parent2);
			}

		}

		population = newGeneration;

		// archive
		updateFitnessFunctionsAndValues();

		currentIteration++;
	}

	private T newRandomIndividual() {
		T randomChromosome = chromosomeFactory.getChromosome();
		for (FitnessFunction<?> fitnessFunction : this.fitnessFunctions) {
			randomChromosome.addFitness(fitnessFunction);
		}
		return randomChromosome;
	}

	/** {@inheritDoc} */
	@Override
	public void initializePopulation() {
		notifySearchStarted();
		currentIteration = 0;

		// Set up initial population
		generateInitialPopulation(Properties.POPULATION);
		logger.debug("Calculating fitness of initial population");
		calculateFitnessAndSortPopulation(true);

		this.notifyIteration();
	}

	@SuppressWarnings("unused")
	private static final double DELTA = 0.000000001; // it seems there is some
														// rounding error in LS,
														// but hard to debug :(

	/** {@inheritDoc} */
	@Override
	public void generateSolution() {

		logger.warn("monotonic ga");
		if (Properties.ENABLE_SECONDARY_OBJECTIVE_AFTER > 0 || Properties.ENABLE_SECONDARY_OBJECTIVE_STARVATION) {
			disableFirstSecondaryCriterion();
		}

		long t1 = System.currentTimeMillis();
		TestCaseLegitimizer.startTime = t1;
		if (population.isEmpty()) {
			initializePopulation();
			assert !population.isEmpty() : "Could not create any test";
		}
		else {
			this.notifyIteration();
		}
		long t2 = System.currentTimeMillis();
		long initializationOverhead = t2 - t1;
		this.setIntializationOverhead(initializationOverhead);
		double coverage = getBestIndividual().getCoverage();
		this.setInitialCoverage(coverage);
		
		logger.warn("initial coverage: " + this.initialCoverage);
		logger.warn("initialization overhead: " + this.intializationOverhead);

		logger.debug("Starting evolution");
		int starvationCounter = 0;
		double bestFitness = Double.MAX_VALUE;
		double lastBestFitness = Double.MAX_VALUE;
		if (getFitnessFunction().isMaximizationFunction()) {
			bestFitness = 0.0;
			lastBestFitness = 0.0;
		}

		RuntimeRecord.methodCallAvailabilityMap.clear();

		BranchCoverageFactory branchFactory = new BranchCoverageFactory();
		List<BranchCoverageTestFitness> branchGoals = branchFactory.getCoverageGoals();
		Map<Integer, Integer> distributionMap = DistributionUtil.constructDistributionMap(branchGoals);
		updateDistribution(distributionMap, true);

		while (!isFinished()) {

			
			
			logger.info("Population size before: " + population.size());
			// related to Properties.ENABLE_SECONDARY_OBJECTIVE_AFTER;
			// check the budget progress and activate a secondary criterion
			// according to the property value.

			{
//				double bestFitnessBeforeEvolution = getBestFitness();
//				long t00 = System.currentTimeMillis();
				evolve();
//				long t11 = System.currentTimeMillis();
//				logger.warn("evolving time " + (t11-t00));
				sortPopulation();

//				double bestFitnessAfterEvolution = getBestFitness();
				bestFitness = getBestFitness();

//				if (getFitnessFunction().isMaximizationFunction())
//					assert (bestFitnessAfterEvolution >= (bestFitnessBeforeEvolution
//							- DELTA)) : "best fitness before evolve()/sortPopulation() was: "
//									+ bestFitnessBeforeEvolution + ", now best fitness is " + bestFitnessAfterEvolution;
//				else
//					assert (bestFitnessAfterEvolution <= (bestFitnessBeforeEvolution
//							+ DELTA)) : "best fitness before evolve()/sortPopulation() was: "
//									+ bestFitnessBeforeEvolution + ", now best fitness is " + bestFitnessAfterEvolution;
			}

			{
//				double bestFitnessBeforeLocalSearch = getBestFitness();
				applyLocalSearch();
//				double bestFitnessAfterLocalSearch = getBestFitness();

				// if (getFitnessFunction().isMaximizationFunction())
				// assert (bestFitnessAfterLocalSearch >= (bestFitnessBeforeLocalSearch
				// - DELTA)) : "best fitness before applyLocalSearch() was: " +
				// bestFitnessBeforeLocalSearch
				// + ", now best fitness is " + bestFitnessAfterLocalSearch;
				// else
				// assert (bestFitnessAfterLocalSearch <= (bestFitnessBeforeLocalSearch
				// + DELTA)) : "best fitness before applyLocalSearch() was: " +
				// bestFitnessBeforeLocalSearch
				// + ", now best fitness is " + bestFitnessAfterLocalSearch;
			}

			/*
			 * TODO: before explanation: due to static state handling, LS can worse
			 * individuals. so, need to re-sort.
			 * 
			 * now: the system tests that were failing have no static state... so re-sorting
			 * does just hide the problem away, and reduce performance (likely
			 * significantly). it is definitively a bug somewhere...
			 */
			// sortPopulation();

			// double newFitness = getBestFitness();
			// endtime = System.currentTimeMillis();
			// if (endtime - begintime >= interval) {
			// bestIndividual = getBestIndividual();
			// if (bestIndividual != null) {
			// double coverage = bestIndividual.getCoverage();
			// progress.add(coverage);
			// fList.add(bestIndividual.getFitness());
			// }
			// begintime = endtime;
			// }

			// if (getFitnessFunction().isMaximizationFunction())
			// assert (newFitness >= (bestFitness - DELTA)) : "best fitness was: " +
			// bestFitness
			// + ", now best fitness is " + newFitness;
			// else
			// assert (newFitness <= (bestFitness + DELTA)) : "best fitness was: " +
			// bestFitness
			// + ", now best fitness is " + newFitness;
			// bestFitness = newFitness;

			if (Double.compare(bestFitness, lastBestFitness) == 0) {
				starvationCounter++;
			} else {
				logger.info("reset starvationCounter after " + starvationCounter + " iterations");
				starvationCounter = 0;
				lastBestFitness = bestFitness;

			}

			updateSecondaryCriterion(starvationCounter);
			updateDistribution(distributionMap, false);

			// printUncoveredBranches(distributionMap, branchGoals);
			// printUncoveredBranches(getBestIndividual(), branchGoals);

			logger.error("Best fitness: " + bestFitness + ", Coverage: " + getBestIndividual().getCoverage());
			logger.info("Current iteration: " + currentIteration);
			this.notifyIteration();

			logger.info("Population size: " + population.size());
			logger.info("Best individual has fitness: " + population.get(0).getFitness());
			logger.info("Worst individual has fitness: " + population.get(population.size() - 1).getFitness());

		}
		// bestIndividual = getBestIndividual();
		// if (bestIndividual != null) {
		// double coverage = bestIndividual.getCoverage();
		// progress.add(coverage);
		// }
		// this.setProgressInformation(progress);
		// logger.error(fList.toString());

		int[] distribution = new int[distributionMap.keySet().size()];
		int count = 0;
		for (Integer key : distributionMap.keySet()) {
			distribution[count++] = distributionMap.get(key);
		}
		this.setDistributionMap(distributionMap);

		Map<Integer, Double> uncoveredBranchDistribution = DistributionUtil.computeBranchDistribution(distributionMap,
				branchGoals);
		this.setUncoveredBranchDistribution(uncoveredBranchDistribution);
		
		Set<BranchCoverageGoal> missingBranchesSet = getUncoveredBranches(getBestIndividual(),branchGoals);
		
		List<BranchInfo> missingBranches = getMissingBranches(population,branchGoals);
//				getBestIndividual().getMissingBranches();
		this.setMissingBranches(missingBranches);
		
		Map<BranchInfo, String> coveredBranchWithTest = getCoveredBranchWithTest(population,branchGoals);
		this.setCoveredBranchWithTest(coveredBranchWithTest);

		// this.setCallUninstrumentedMethod(true);

		// for (Integer branchID : distributionMap.keySet()) {
		// logger.error("branch ID: " + branchID + ": " +
		// distributionMap.get(branchID));
		// }

		double availabilityRatio = getAvailabilityRatio();

		this.setAvailabilityRatio(availabilityRatio);
		this.setAvailableCalls(getAvailableCalls());
		this.setUnavailableCalls(getUnavailableCalls());
		
		// archive
		TimeController.execute(this::updateBestIndividualFromArchive, "update from archive", 5_000);

		notifySearchFinished();
	}


	private List<BranchInfo> getMissingBranches(List<T> population,List<BranchCoverageTestFitness> branchGoals) {
		List<BranchInfo> missingBranches = new ArrayList<>();
		for (BranchCoverageTestFitness tf : branchGoals) {
			
			if(tf.getBranch()==null) continue;
			
			boolean value = tf.getValue();
			BranchInfo branchInfo = new BranchInfo(tf.getBranch(),value);
			missingBranches.add(branchInfo);
		}
		
		for(int i = 0;i < population.size();i++) {
			
			TestSuiteChromosome testsuite = (TestSuiteChromosome) population.get(i);
			
			for(TestChromosome test: testsuite.getTestChromosomes()) {
				ExecutionResult result = test.getLastExecutionResult();
				if (result != null) {
					for (BranchCoverageTestFitness tf : branchGoals) {
						
						if(tf.getBranch() == null) continue;
						
						int branchID = tf.getBranch().getActualBranchId();
						boolean value = tf.getValue();
						BranchInfo branchInfo = new BranchInfo(tf.getBranch(),value);

						if (value) {
							Double distance = result.getTrace().getTrueDistances().get(branchID);
							if (distance != null && distance == 0) {
								if(missingBranches.contains(branchInfo))
									missingBranches.remove(branchInfo);
							} else {
								continue;
							}
						} else {
							Double distance = result.getTrace().getFalseDistances().get(branchID);
							if (distance != null && distance == 0) {
								if(missingBranches.contains(branchInfo))
									missingBranches.remove(branchInfo);
							}else {
								continue;
							}
						}
					}
				}
			}
					
		}
		return missingBranches;
		}

	private Map<BranchInfo, String> getCoveredBranchWithTest(List<T> population,
			List<BranchCoverageTestFitness> branchGoals) {
		Map<BranchInfo, String> coveredBranchWithTest = new HashMap<BranchInfo, String>();
		for(int i = 0;i < population.size();i++) {
			TestSuiteChromosome testsuite = (TestSuiteChromosome) population.get(i);
			
			for(TestChromosome test: testsuite.getTestChromosomes()) {
				ExecutionResult result = test.getLastExecutionResult();
				if (result != null) {
					for (BranchCoverageTestFitness tf : branchGoals) {
						
						if(tf.getBranch()==null)continue;
						
						int branchID = tf.getBranch().getActualBranchId();
						boolean value = tf.getValue();

						if (value) {
							Double distance = result.getTrace().getTrueDistances().get(branchID);
							if (distance != null && distance == 0) {
								BranchInfo branch = new BranchInfo(tf.getBranch(),value);
								if(!coveredBranchWithTest.containsKey(branch))
									coveredBranchWithTest.put(branch, test.getTestCase().toString());
							} 
						} else {
							Double distance = result.getTrace().getFalseDistances().get(branchID);
							if (distance != null && distance == 0) {
								BranchInfo branch = new BranchInfo(tf.getBranch(),value);
								if(!coveredBranchWithTest.containsKey(branch))
									coveredBranchWithTest.put(branch, test.getTestCase().toString());
							}
						}
					}
				}
			}
			
			
		}
		return coveredBranchWithTest;
//		return null;
	}

	private Set<BranchCoverageGoal> getUncoveredBranches(T bestIndividual, List<BranchCoverageTestFitness> branchGoals) {
		if (bestIndividual instanceof TestSuiteChromosome) {

			Set<BranchCoverageGoal> uncoveredGoals = new HashSet<>();

			TestSuiteChromosome testsuite = (TestSuiteChromosome) bestIndividual;

			for (ExecutionResult result : testsuite.getLastExecutionResults()) {
				if (result != null) {
					for (BranchCoverageTestFitness tf : branchGoals) {
						
						if(tf.getBranch() == null) continue;
						
						int branchID = tf.getBranch().getActualBranchId();
						boolean value = tf.getValue();

						if (value) {
							Double distance = result.getTrace().getTrueDistances().get(branchID);
							if (distance != null && distance == 0) {
								uncoveredGoals.remove(tf.getBranchGoal());
							} else {
								uncoveredGoals.add(tf.getBranchGoal());
							}
						} else {
							Double distance = result.getTrace().getFalseDistances().get(branchID);
							if (distance != null && distance == 0) {
								uncoveredGoals.remove(tf.getBranchGoal());
							} else {
								uncoveredGoals.add(tf.getBranchGoal());
							}
						}
					}
				}
			}

//			for (BranchCoverageGoal goal : uncoveredGoals) {
//				logger.error(goal.toString());
//			}
			
			return uncoveredGoals;
		}
		
		return null;

	}

	public double getAvailabilityRatio() {
		int count = 0;
		for (String key : RuntimeRecord.methodCallAvailabilityMap.keySet()) {
			if (RuntimeRecord.methodCallAvailabilityMap.get(key)) {
				count++;
			} else {
				System.out.println("Missing analyzing call: " + key);
			}
		}
		int size = RuntimeRecord.methodCallAvailabilityMap.size();
		double ratio = -1;
		if (size != 0) {
			ratio = (double) count / size;
		}
		System.out.println("Method call availability: " + ratio);

		return ratio;
	}

	public List<String> getAvailableCalls() {
		List<String> calls = new ArrayList<>();
		for (String method : RuntimeRecord.methodCallAvailabilityMap.keySet()) {
			if (RuntimeRecord.methodCallAvailabilityMap.get(method)) {
				calls.add(method);
			}
		}
		return calls;
	}

	public List<String> getUnavailableCalls() {
		List<String> calls = new ArrayList<>();
		for (String method : RuntimeRecord.methodCallAvailabilityMap.keySet()) {
			if (!RuntimeRecord.methodCallAvailabilityMap.get(method)) {
				calls.add(method);
			}
		}
		return calls;
	}

	private void printUncoveredBranches(Map<Integer, Integer> distributionMap,
			List<BranchCoverageTestFitness> branchGoals) {
		for (BranchCoverageTestFitness goal : branchGoals) {
			int id = goal.getBranch().getActualBranchId();
			if (!goal.getBranchExpressionValue()) {
				id = -id;
			}
			int coverage = distributionMap.get(id);
			if (coverage == 0) {
				logger.error("uncovered:" + goal.getBranch());
			}
		}

	}

	private void updateDistribution(Map<Integer, Integer> distributionMap, boolean firstTime) {
		if (distributionMap.keySet().size() == 0) {
			return;
		}

		List<T> individuals = firstTime ? this.population : this.newGeneratedIndividuals;
		for (T individual : individuals) {
			if (individual instanceof TestSuiteChromosome) {
				TestSuiteChromosome testSuite = (TestSuiteChromosome) individual;
				for (ExecutionResult result : testSuite.getLastExecutionResults()) {
					if (result != null) {

						for (Integer branchID : result.getTrace().getCoveredTrue().keySet()) {
							if (distributionMap.get(branchID) != null) {
								int count = distributionMap.get(branchID) + 1;
								distributionMap.put(branchID, count);
							}
						}

						for (Integer branchID : result.getTrace().getCoveredFalse().keySet()) {
							if (distributionMap.get(-branchID) != null) {
								int count = distributionMap.get(-branchID) + 1;
								distributionMap.put(-branchID, count);
							}
						}
					}
				}
			} else if (individual instanceof TestChromosome) {
				TestChromosome test = (TestChromosome) individual;
				ExecutionResult result = test.getLastExecutionResult();

				if (result != null) {

					for (Integer branchID : result.getTrace().getCoveredTrue().keySet()) {
						if (distributionMap.get(branchID) != null) {
							int count = distributionMap.get(branchID) + 1;
							distributionMap.put(branchID, count);
						}
					}

					for (Integer branchID : result.getTrace().getCoveredFalse().keySet()) {
						if (distributionMap.get(-branchID) != null) {
							int count = distributionMap.get(-branchID) + 1;
							distributionMap.put(-branchID, count);
						}
					}
				}
			}

		}
	}

	private double getBestFitness() {
		T bestIndividual = getBestIndividual();
		for (FitnessFunction<T> ff : fitnessFunctions) {
			ff.getFitness(bestIndividual);
		}
		return bestIndividual.getFitness();
	}

	/**
	 * <p>
	 * setReplacementFunction
	 * </p>
	 * 
	 * @param replacement_function a {@link org.evosuite.ga.ReplacementFunction}
	 *                             object.
	 */
	public void setReplacementFunction(ReplacementFunction replacement_function) {
		this.replacementFunction = replacement_function;
	}

	/**
	 * <p>
	 * getReplacementFunction
	 * </p>
	 * 
	 * @return a {@link org.evosuite.ga.ReplacementFunction} object.
	 */
	public ReplacementFunction getReplacementFunction() {
		return replacementFunction;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void updatePopulation(TestSuiteChromosome previousSeeds) {
		List<TestChromosome> pop = (List<TestChromosome>) this.population;
		
		if(pop.size() + previousSeeds.getTestChromosomes().size() < Properties.POPULATION) {
			pop.addAll(previousSeeds.getTestChromosomes());			
		}
		else {
			List<TestChromosome> selected = new ArrayList<TestChromosome>();
			int eliteNum = previousSeeds.size() < 10 ? previousSeeds.size() : 10;
			for(int i=0; i<eliteNum; i++) {
				selected.add(previousSeeds.getTestChromosome(i));
			}
			
			pop.addAll(previousSeeds.getTestChromosomes());	
			while(pop.size() > Properties.POPULATION) {
				pop.remove(0);
			}
		}
		
	}

	@Override
	public void generateSolution(TestSuiteChromosome previousSeeds) {
		logger.warn("monotonic ga");
		if (Properties.ENABLE_SECONDARY_OBJECTIVE_AFTER > 0 || Properties.ENABLE_SECONDARY_OBJECTIVE_STARVATION) {
			disableFirstSecondaryCriterion();
		}

		if (population.isEmpty()) {
			initializePopulation();
			assert !population.isEmpty() : "Could not create any test";
		}
		else {
			this.notifyIteration();
		}
		
		this.updatePopulation(previousSeeds);

		logger.debug("Starting evolution");
		int starvationCounter = 0;
		double bestFitness = Double.MAX_VALUE;
		double lastBestFitness = Double.MAX_VALUE;
		if (getFitnessFunction().isMaximizationFunction()) {
			bestFitness = 0.0;
			lastBestFitness = 0.0;
		}

		RuntimeRecord.methodCallAvailabilityMap.clear();

//		StatisticChecker timer = new StatisticChecker(getProgressInformation(), new CoverageProgressGetter() {
//			@Override
//			public double getCoverage() {
//				T bestIndividual = getBestIndividual();
//				return bestIndividual.getCoverage();
//			}
//		});
//		Thread timerThread = new Thread(timer);
//		timerThread.start();

		BranchCoverageFactory branchFactory = new BranchCoverageFactory();
		List<BranchCoverageTestFitness> branchGoals = branchFactory.getCoverageGoals();
		Map<Integer, Integer> distributionMap = DistributionUtil.constructDistributionMap(branchGoals);
		updateDistribution(distributionMap, true);

		while (!isFinished()) {

			logger.info("Population size before: " + population.size());
			// related to Properties.ENABLE_SECONDARY_OBJECTIVE_AFTER;
			// check the budget progress and activate a secondary criterion
			// according to the property value.

			{
//				double bestFitnessBeforeEvolution = getBestFitness();
				evolve();
				sortPopulation();

//				double bestFitnessAfterEvolution = getBestFitness();
				bestFitness = getBestFitness();

//				if (getFitnessFunction().isMaximizationFunction())
//					assert (bestFitnessAfterEvolution >= (bestFitnessBeforeEvolution
//							- DELTA)) : "best fitness before evolve()/sortPopulation() was: "
//									+ bestFitnessBeforeEvolution + ", now best fitness is " + bestFitnessAfterEvolution;
//				else
//					assert (bestFitnessAfterEvolution <= (bestFitnessBeforeEvolution
//							+ DELTA)) : "best fitness before evolve()/sortPopulation() was: "
//									+ bestFitnessBeforeEvolution + ", now best fitness is " + bestFitnessAfterEvolution;
			}

			{
//				double bestFitnessBeforeLocalSearch = getBestFitness();
				applyLocalSearch();
//				double bestFitnessAfterLocalSearch = getBestFitness();

				// if (getFitnessFunction().isMaximizationFunction())
				// assert (bestFitnessAfterLocalSearch >= (bestFitnessBeforeLocalSearch
				// - DELTA)) : "best fitness before applyLocalSearch() was: " +
				// bestFitnessBeforeLocalSearch
				// + ", now best fitness is " + bestFitnessAfterLocalSearch;
				// else
				// assert (bestFitnessAfterLocalSearch <= (bestFitnessBeforeLocalSearch
				// + DELTA)) : "best fitness before applyLocalSearch() was: " +
				// bestFitnessBeforeLocalSearch
				// + ", now best fitness is " + bestFitnessAfterLocalSearch;
			}

			/*
			 * TODO: before explanation: due to static state handling, LS can worse
			 * individuals. so, need to re-sort.
			 * 
			 * now: the system tests that were failing have no static state... so re-sorting
			 * does just hide the problem away, and reduce performance (likely
			 * significantly). it is definitively a bug somewhere...
			 */
			// sortPopulation();

			// double newFitness = getBestFitness();
			// endtime = System.currentTimeMillis();
			// if (endtime - begintime >= interval) {
			// bestIndividual = getBestIndividual();
			// if (bestIndividual != null) {
			// double coverage = bestIndividual.getCoverage();
			// progress.add(coverage);
			// fList.add(bestIndividual.getFitness());
			// }
			// begintime = endtime;
			// }

			// if (getFitnessFunction().isMaximizationFunction())
			// assert (newFitness >= (bestFitness - DELTA)) : "best fitness was: " +
			// bestFitness
			// + ", now best fitness is " + newFitness;
			// else
			// assert (newFitness <= (bestFitness + DELTA)) : "best fitness was: " +
			// bestFitness
			// + ", now best fitness is " + newFitness;
			// bestFitness = newFitness;

			if (Double.compare(bestFitness, lastBestFitness) == 0) {
				starvationCounter++;
			} else {
				logger.info("reset starvationCounter after " + starvationCounter + " iterations");
				starvationCounter = 0;
				lastBestFitness = bestFitness;

			}

			updateSecondaryCriterion(starvationCounter);
			updateDistribution(distributionMap, false);

			// printUncoveredBranches(distributionMap, branchGoals);
			// printUncoveredBranches(getBestIndividual(), branchGoals);

//			logger.error("Best fitness: " + bestFitness + ", Coverage: " + getBestIndividual().getCoverage());
			logger.info("Current iteration: " + currentIteration);
			this.notifyIteration();

			logger.info("Population size: " + population.size());
			logger.info("Best individual has fitness: " + population.get(0).getFitness());
			logger.info("Worst individual has fitness: " + population.get(population.size() - 1).getFitness());

		}
		// bestIndividual = getBestIndividual();
		// if (bestIndividual != null) {
		// double coverage = bestIndividual.getCoverage();
		// progress.add(coverage);
		// }
		// this.setProgressInformation(progress);
		// logger.error(fList.toString());

		int[] distribution = new int[distributionMap.keySet().size()];
		int count = 0;
		for (Integer key : distributionMap.keySet()) {
			distribution[count++] = distributionMap.get(key);
		}
		this.setDistributionMap(distributionMap);

		Map<Integer, Double> uncoveredBranchDistribution = DistributionUtil.computeBranchDistribution(distributionMap,
				branchGoals);
		this.setUncoveredBranchDistribution(uncoveredBranchDistribution);

		// this.setCallUninstrumentedMethod(true);

		// for (Integer branchID : distributionMap.keySet()) {
		// logger.error("branch ID: " + branchID + ": " +
		// distributionMap.get(branchID));
		// }

		double availabilityRatio = getAvailabilityRatio();

		this.setAvailabilityRatio(availabilityRatio);
		this.setAvailableCalls(getAvailableCalls());
		this.setUnavailableCalls(getUnavailableCalls());

		// archive
		TimeController.execute(this::updateBestIndividualFromArchive, "update from archive", 5_000);

		notifySearchFinished();
		
	}

}
