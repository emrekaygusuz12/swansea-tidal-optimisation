ReadME

The tuning results are in results folder.


You can change the configuration parameters in NSGA2Config under getAnnualConfig()
You can run DatasetComparator to compare 2011 and 2012 data
You can also run ManualTuningRunner but you're going to have to change the testing configurations and the result will be displayed in the terminal and also saved in results folder as a csv file

COMPILATION INSTRUCTIONS
========================

To compile the Java project, run the following command from the project root directory:

javac -d out src\NSGA2Main.java src\utils\TideDataReader.java src\optimisation\NSGA2Algorithm.java src\optimisation\NSGA2Config.java src\optimisation\Individual.java src\optimisation\Population.java src\optimisation\ObjectiveFunction.java src\optimisation\GeneticOperators.java src\optimisation\ParetoDominance.java src\optimisation\CrowdingDistance.java src\optimisation\NextGenerationSelection.java src\model\SimulationConfig.java src\analysis\DatasetComparator.java src\tuning\ManualTuningRunner.java

After compilation, run the program with:

java -cp out src.NSGA2Main annual

or 

java -cp out src.tuning.ManualTuningRunner

or 

java -cp out src.analysis.DatasetComparator