package inputOutput;

import general.General;
import general.Combinations;
import general.RandomPartition;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Random;
import java.util.TreeSet;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.BetaDistributionImpl;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.apache.commons.math.distribution.WeibullDistributionImpl;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.distribution.WeibullDistributionImpl;
import org.apache.commons.math.distribution.FDistributionImpl;
import org.apache.commons.math.distribution.CauchyDistributionImpl;
import org.apache.commons.math.distribution.TDistributionImpl;
import org.apache.commons.math.distribution.ZipfDistributionImpl;
import org.apache.commons.math.distribution.PascalDistributionImpl;

public class Input
{
	//The parameters that can only be set here :
	public final boolean printPercentageOf_v_equals_f = false; // only relevant when using dynamic programming. The percentage of coalitions whose value equals their best partition
	public final boolean printDistributionOfThefTable = false; // only relevant when using dynamic programming. The distribution of the f table
	public final boolean printCoalitionValueDistribution = false; // the distribution of the coalition values
	public final boolean printCoalitionStructureValueDistribution = false; // the distribution of the coalition structure values (based on a sample of coalition structures)
	public final boolean printTheSubspaceThatIsCurrentlyBeingSearched = false; // only relevant when running IP or ODP-IP
	public final boolean printTheCoalitionsWhenPrintingTheirValues = true;
	public final boolean pruneSubspaces = true; // only relevant when running IP or ODP-IP. Determines whether IP is allowed to prune entire subspaces based on their bounds.
	public final boolean useBranchAndBound = true; // only relevant when running IP or ODP-IP
	public final boolean useSamplingWhenSearchingSubspaces = false; // only relevant when running IP or ODP-IP.
	public final boolean samplingDoneInGreedyFashion = false;
	private final double sigma = 0.1; // relevant to the following distributions: "Normal", "Modified Normal", and "Agent-based Normal"
	public final boolean useEfficientImplementationOfIDP = true; //if "true", IDP evaluates more splits, but this allows for a more efficient implementation and a shorter runtime.
	
	public SolverNames solverName;
	public boolean readCoalitionValuesFromFile;
	public boolean storeCoalitionValuesInFile;
	
	//The IP parameters
	public boolean orderIntegerPartitionsAscendingly; // setting this to false means integers will be ordered descendingly
	public double  acceptableRatio; // only relevant when running IP or ODP-IP
	
	//The printing parameters
	public boolean printDetailsOfSubspaces; // only relevant when running IP or ODP-IP
	public boolean printNumOfIntegerPartitionsWithRepeatedParts;
	public boolean printInterimResultsOfIPToFiles; // only relevant when running IP or ODP-IP
	public boolean printTimeTakenByIPForEachSubspace; // only relevant when running IP or ODP-IP

	//General parameters
	public TreeSet<Integer> feasibleCoalitions;
	public int numOfAgents;
	public long numOfRunningTimes;	
	public double[] coalitionValues;
	public String outputFolder;
	public ValueDistribution valueDistribution;
	public String folderInWhichCoalitionValuesAreStored = "../../CSGtemp"; //this is the default setting, unless it is modified by another method

	public String problemID = ""; //This is the ID of the problem instance (useful when taking average over multiple instances)

	//******************************************************************************************************

	/**
	 * Sets some parameters to default values. These values can be changed later on of course.
	 */
	public void initInput()
	{
		feasibleCoalitions = null; // this is only needed for a constraint solver. Just ignore it.
		numOfRunningTimes = 1; // Setting this to 1 means we are not taking an average over multiple run with different problem instaces.
		storeCoalitionValuesInFile = false; // Setting this to false means we won't be storing the coalition values after solving the problem
		printDetailsOfSubspaces = false;
		valueDistribution = ValueDistribution.UNKNOWN;
	}

	//******************************************************************************************************

	/** Return the value of a coalition where every member is represented by an bit
	 */
	public double getCoalitionValue( int coalitionInBitFormat ){
		return( coalitionValues[ coalitionInBitFormat ]);
	}
	/** Return the value of a coalition where every member is represented by an integer
	 */ 
	public double getCoalitionValue( int[] coalitionInByteFormat ){
		int coalitionInBitFormat = Combinations.convertCombinationFromByteToBitFormat( coalitionInByteFormat );
		return( getCoalitionValue( coalitionInBitFormat ) );
	}
	/** Return the value of a given coalition structure CS, where each coalition member is represented using an integer
	 */
	public double getCoalitionStructureValue( int[][] coalitionStructure ){
		double valueOfCS = 0;
		for(int i=0; i<coalitionStructure.length; i++)
				valueOfCS += getCoalitionValue( coalitionStructure[i] );
		return( valueOfCS );
	}	
	/** Return the value of a given coalition structure CS, where each coalition member is represented using a bit
	 */
	public double getCoalitionStructureValue( int[] coalitionStructure ){
		double valueOfCS = 0;
		for(int i=0; i<coalitionStructure.length; i++)
			valueOfCS += getCoalitionValue( coalitionStructure[i] );
		return( valueOfCS );
	}	

	//*****************************************************************************************************

	/**
	 * This method sets the values of the coalitions of a particular size.
	 */ 
	public void generateCoalitionValues()
	{
		//initialization
		Random random = new Random();
		long time = System.currentTimeMillis();
		coalitionValues = new double[ (int)Math.pow(2,numOfAgents) ];
		coalitionValues[0] = 0; //just in case, set the value of the empty set to 0

		//Give random strengths to the agents. This is only used in "agent-Based" distributions
		double[] agentStrength_normal  = new double[numOfAgents];
		double[] agentStrength_uniform = new double[numOfAgents];
		for( int agent=1; agent<=numOfAgents; agent++){
			agentStrength_normal [agent-1] = Math.max( 0, General.getRandomNumberFromNormalDistribution( 10,sigma,random) );
			agentStrength_uniform[agent-1] = random.nextDouble() * 10;
		}
		//Uniform distribution		
		if( valueDistribution == ValueDistribution.UNIFORM )
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--)
				coalitionValues[ coalition ] = Math.round( (Integer.bitCount(coalition) * random.nextDouble()) * 100000000 );

		//Normal distribution
		if( valueDistribution == ValueDistribution.NORMAL )
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				coalitionValues[ coalition ] = Math.max( 0, Integer.bitCount(coalition) * General.getRandomNumberFromNormalDistribution( 10,sigma,random) );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );
			}
		//Modified Uniform distribution
		if( valueDistribution == ValueDistribution.MODIFIEDUNIFORM )
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				coalitionValues[ coalition ] = random.nextDouble()*10*Integer.bitCount(coalition);
				int probability = random.nextInt(100);
				if(probability <=20) coalitionValues[ coalition ] += random.nextDouble() * 50;
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );
			}
		//Modified Normal distribution
		if( valueDistribution == ValueDistribution.MODIFIEDNORMAL )
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				coalitionValues[ coalition ] = Math.max( 0, Integer.bitCount(coalition) * General.getRandomNumberFromNormalDistribution( 10,sigma,random) );
				int probability = random.nextInt(100);
				//if(probability <=20) valuesOfCurSize[index] += r.nextDouble() * 50;
				if(probability <=20) coalitionValues[ coalition ] += General.getRandomNumberFromNormalDistribution( 25,sigma,random);
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );
			}
		//Exponential distribution
		if( valueDistribution == ValueDistribution.EXPONENTIAL ){
			ExponentialDistributionImpl exponentialDistributionImpl = new ExponentialDistributionImpl(1); //the mean of an exponential distribution is 1/lambda.
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, Integer.bitCount(coalition) * exponentialDistributionImpl.sample() );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );
			}
		}
		//Beta distribution
		if( valueDistribution == ValueDistribution.BETA ){
			BetaDistributionImpl betaDistributionImpl =new BetaDistributionImpl(0.5, 0.5);
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, Integer.bitCount(coalition) * betaDistributionImpl.sample() );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );				
			}
		}
		//Gamma distribution
		if( valueDistribution == ValueDistribution.GAMMA ){
			GammaDistributionImpl gammaDistributionImpl = new GammaDistributionImpl(2, 2);
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, Integer.bitCount(coalition) * gammaDistributionImpl.sample() );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );
			}
		}
		//Chi-square distribution
		if( valueDistribution == ValueDistribution.CHISQUARE ){
			ChiSquaredDistributionImpl[] chiSquaredDistributionImpl = new ChiSquaredDistributionImpl[numOfAgents+1];
			for(int i=1; i<=numOfAgents; i++){
				chiSquaredDistributionImpl[i] = new ChiSquaredDistributionImpl(i);
			}
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, chiSquaredDistributionImpl[Integer.bitCount(coalition)].sample() );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );
			}
		}
		//Weibull distribution
		if( valueDistribution == ValueDistribution.WEIBULL ){
			WeibullDistributionImpl[] weibullDistributionImpl = new WeibullDistributionImpl[numOfAgents+1];
			for(int i=1; i<=numOfAgents; i++){
				weibullDistributionImpl[i] = new WeibullDistributionImpl(i, 0.9);
			}
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, Integer.bitCount(coalition) * weibullDistributionImpl[Integer.bitCount(coalition)].sample() );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );				
			}
		}
		//F distribution
		if( valueDistribution == ValueDistribution.F ){
			FDistributionImpl[] fDistributionImpl = new FDistributionImpl[numOfAgents+1];
			for(int i=1; i<=numOfAgents; i++){
				fDistributionImpl[i] = new FDistributionImpl(1, i+1);
			}
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, fDistributionImpl[Integer.bitCount(coalition)].sample() );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );				
			}
		}
		//Cauchy distribution
		if( valueDistribution == ValueDistribution.CAUCHY ){
			CauchyDistributionImpl[] cauchyDistributionImpl = new CauchyDistributionImpl[numOfAgents+1];
			//FDistributionImpl[] fDistributionImpl = new FDistributionImpl[numOfAgents+1];
			for(int i=1; i<=numOfAgents; i++){
				cauchyDistributionImpl[i] = new CauchyDistributionImpl(10*i, 0.1);
			}
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, cauchyDistributionImpl[Integer.bitCount(coalition)].sample() );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );				
			}
		}
		//T distribution
		if( valueDistribution == ValueDistribution.T ){
			TDistributionImpl[] tDistributionImpl = new TDistributionImpl[numOfAgents+1];
			for(int i=1; i<=numOfAgents; i++){
				tDistributionImpl[i] = new TDistributionImpl(i);
			}
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, Integer.bitCount(coalition) * tDistributionImpl[Integer.bitCount(coalition)].sample() );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );				
			}
		}
		//Zipf distribution
		if( valueDistribution == ValueDistribution.Zipf ){
			ZipfDistributionImpl[] zipfDistributionImpl = new ZipfDistributionImpl[numOfAgents+1];
			for(int i=1; i<=numOfAgents; i++){
				zipfDistributionImpl[i] = new ZipfDistributionImpl(i,0.1);
			}
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, Integer.bitCount(coalition) * zipfDistributionImpl[Integer.bitCount(coalition)].sample() + (random.nextDouble()*50) );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );				
			}
		}
		//Pascal distribution
		if( valueDistribution == ValueDistribution.Pascal ){
			PascalDistributionImpl[] pascalDistributionImpl = new PascalDistributionImpl[numOfAgents+1];
			for(int i=1; i<=numOfAgents; i++){
				pascalDistributionImpl[i] = new PascalDistributionImpl(i, 0.1);
			}
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				boolean repeat = false;
				do{
					try { coalitionValues[ coalition ] = Math.max( 0, Integer.bitCount(coalition) * pascalDistributionImpl[Integer.bitCount(coalition)].sample() + (random.nextDouble()*50) );
					} catch (MathException e) { repeat = true; }
				}while( repeat == true );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );				
			}
		}
		//Agent-based Uniform distribution
		if( valueDistribution == ValueDistribution.AGENTBASEDUNIFORM )
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				int[] members = Combinations.convertCombinationFromBitToByteFormat(coalition, numOfAgents);

				//if percentage is, say 60%, then an agent's value can go up by at most 60%, and down by at most 60%
				double percentage = 100;
				coalitionValues[ coalition ] = 0;
				for( int m=0; m<Integer.bitCount(coalition); m++){
					double rangeSize = (percentage/(double)100) * agentStrength_uniform[members[m]-1] * 2;
					double startOfRange = ((100 - percentage)/(double)100) * agentStrength_uniform[members[m]-1];
					coalitionValues[ coalition ] += startOfRange + (random.nextDouble()*rangeSize);
				}
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );
			}		
		//Agent-based Normal distribution
		if( valueDistribution == ValueDistribution.AGENTBASEDNORMAL || valueDistribution == ValueDistribution.AGENTBASEDNORMAL)
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				int[] members = Combinations.convertCombinationFromBitToByteFormat(coalition, numOfAgents);
				coalitionValues[ coalition ] = 0;
				for( int m=0; m<Integer.bitCount(coalition); m++){
					double newValue;
					newValue = General.getRandomNumberFromNormalDistribution(agentStrength_normal[members[m]-1], sigma, random);
					coalitionValues[ coalition ] += Math.max( 0, newValue );
				}
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );	
			}
		//NDCS distribution
		if( valueDistribution == ValueDistribution.NDCS )
			for(int coalition = coalitionValues.length-1; coalition>0; coalition--){
				do{ 
					coalitionValues[ coalition ] = (General.getRandomNumberFromNormalDistribution(Integer.bitCount(coalition),Math.sqrt(Integer.bitCount(coalition)),random));
				}while( coalitionValues[ coalition ] <= 0 );
				coalitionValues[ coalition ] = Math.round( coalitionValues[ coalition ] * 100000000 );	
			}

		System.out.println(numOfAgents+" agents, "+ValueDistribution.toString(valueDistribution)+" distribution. The time required to generate the coalition values (in milli second): "+(System.currentTimeMillis()-time));

		//get the coalition value distribution
		if( printCoalitionValueDistribution ) printCoalitionValueDistribution();

		//get the coalition structure value distribution
		if( printCoalitionStructureValueDistribution ) printCoalitionStructureValueDistribution( numOfAgents );
	}	

	//******************************************************************************************************

	/**
	 * Store the coalition values in a file
	 */
	public void storeCoalitionValuesInFile( int problemID )
	{
		//Create the output folder 
		General.createFolder( "../../CSGtemp" );
		
		//Set the name of the output file, and empty it
		String filePathAndName = "../../CSGtemp";
		filePathAndName += "/"+"Agents_"+".txt";
		General.clearFile( filePathAndName );

		//Print the coalition values to a string buffer
		StringBuffer tempStringBuffer = new StringBuffer();
		int numberOfValuesInEachPart = ((int)Math.pow(2,numOfAgents)-1)/64;
		int coalition = 0;
		for(int i = 0; i<64; i++){
			tempStringBuffer = new StringBuffer();
			for(int j = 0; j< numberOfValuesInEachPart; j++){
				tempStringBuffer.append( coalitionValues[ coalition ]+"\n");
				coalition++;
			}
			General.printToFile( filePathAndName, tempStringBuffer.toString(), false);
		}
		tempStringBuffer = new StringBuffer();
		while(coalition < coalitionValues.length){
			tempStringBuffer.append( coalitionValues[ coalition ]+"\n");
			coalition++;
		}

		//empty the contents of the string buffer into the output file
		//General.printToFile( filePathAndName, tempStringBuffer.toString(), false);
		tempStringBuffer.setLength(0);
	}
	
	//*****************************************************************************************************
	
	/**
	 * Read the coalition values from a file
	 */
	public void readCoalitionValuesFromFile( int problemID )
	{
		coalitionValues = new double[ (int)Math.pow(2,numOfAgents) ];
		
		//Set the name and path of the file from which we will read the coalition values
		String filePathAndName = folderInWhichCoalitionValuesAreStored;
		filePathAndName += "/"+numOfAgents+"Agents_"+ValueDistribution.toString(valueDistribution)+"_"+problemID+".txt";
		try{
			BufferedReader bufferedReader = new BufferedReader( new FileReader(filePathAndName) );
			for(int coalition = 0; coalition<coalitionValues.length; coalition++)
				coalitionValues[ coalition ] = (new Double(bufferedReader.readLine())).doubleValue();			
			
			bufferedReader.close();
		}
		catch (Exception e){
			System.out.println(e);
		}		
	}	

	//******************************************************************************************************

	/**
	 * Read the coalition values from a file
	 */
	public void readCoalitionValuesFromFile( String fileName )
	{
		coalitionValues = new double[ (int)Math.pow(2,numOfAgents) ];
		
		//Set the name and path of the file from which we will read the coalition values
		String filePathAndName = folderInWhichCoalitionValuesAreStored;
		filePathAndName += "/"+fileName;
		try{
			BufferedReader bufferedReader = new BufferedReader( new FileReader(filePathAndName) );
			for(int coalition = 0; coalition<coalitionValues.length; coalition++)
				coalitionValues[ coalition ] = (new Double(bufferedReader.readLine())).doubleValue();			
			
			bufferedReader.close();
		}
		catch (Exception e){
			System.out.println(e);
		}		
	}	

	//******************************************************************************************************

	/**
	 * Given the coalition values, print the "weighted" distribution (i.e., where every coalition value is divided
	 * by the size of that coalition).
	 */
	public void printCoalitionValueDistribution()
	{
		//Initialization
		int[] counter = new int[40];
		for(int i=0; i<counter.length; i++){
			counter[i]=0;
		}		
		//get the minimum and maximum values
		long min = Integer.MAX_VALUE;
		long max = Integer.MIN_VALUE;
		for(int coalition=1; coalition<coalitionValues.length; coalition++){
			long currentWeightedValue = (long)Math.round( coalitionValues[coalition] / Integer.bitCount(coalition) );
			if( min > currentWeightedValue )
				min = currentWeightedValue ;
			if( max < currentWeightedValue )
				max = currentWeightedValue ;
		}
		System.out.println("The maximum weighted coalition value (i.e., value of coalition divided by size of that coalition) is  "+max+"  and the minimum one is  "+min);
		
		//get the distribution		       
		for(int coalition=1; coalition<coalitionValues.length; coalition++){
			long currentWeightedValue = (long)Math.round( coalitionValues[coalition] / Integer.bitCount(coalition) );
			int percentageOfMax = (int)Math.round( (currentWeightedValue-min) * (counter.length-1) / (max-min) );
			counter[percentageOfMax]++;
		}
		//Printing
		System.out.println("The distribution of the weighted coalition values (i.e., every value of coalition is divided by size of that coalition) is:");
		System.out.print(ValueDistribution.toString(valueDistribution)+"_coalition = [");
		for(int i=0; i<counter.length; i++)
			System.out.print(counter[i]+" ");
		System.out.println("]");
	}
	
	//******************************************************************************************************
	
	/**
	 * Given the coalition values, print the "weighted" distribution (i.e., where every coalition value is divided
	 * by the size of that coalition).
	 */
	public void printCoalitionStructureValueDistribution( int n )
	{
		RandomPartition randomPartition = new RandomPartition(n);
		int numOfSamples = 10000000;		
		long[] sampleValues = new long[numOfSamples];
		for(int i=0; i<numOfSamples; i++){
			int[] currentCoalitionStructure = randomPartition.get();
			long value = 0;
			for(int j=0; j<currentCoalitionStructure.length; j++)
				value += coalitionValues[ currentCoalitionStructure[j] ];
			//value /= currentCoalitionStructure.length;
			sampleValues[i] = value;
		}
		//Initialization
		int[] counter = new int[200];
		for(int i=0; i<counter.length; i++){
			counter[i]=0;
		}		
		//get the minimum and maximum values
		long min = Integer.MAX_VALUE;
		long max = Integer.MIN_VALUE;
		for(int i=1; i<sampleValues.length; i++){
			long currentValue = sampleValues[i];
			if( min > currentValue )
				min = currentValue ;
			if( max < currentValue )
				max = currentValue ;
		}
		System.out.println("The maximum weighted coalition value (i.e., value of coalition divided by size of that coalition) is  "+max+"  and the minimum one is  "+min);
		
		//get the distribution		       
		for(int i=1; i<sampleValues.length; i++){
			long currentValue = sampleValues[i];
			int percentageOfMax = (int)Math.round( (currentValue-min) * (counter.length-1) / (max-min) );
			counter[percentageOfMax]++;
		}
		//Printing
		System.out.println("The distribution of the weighted coalition STRUCTURE values  (i.e., every value of coalition is divided by size of that coalition) is:");
		System.out.print(ValueDistribution.toString(valueDistribution)+"_structure = [");
		for(int i=0; i<counter.length; i++)
			System.out.print(counter[i]+" ");
		System.out.println("]");
	}
}