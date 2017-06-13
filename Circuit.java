
// Circuit.java
// by David Cline

import java.io.*;
import java.util.*;

class ArrayIndex
{
	int[] arr;
	int idx;
}

public class Circuit
{
	//---------------------------------------------------------------------//
	// Parsing Stuff
	//---------------------------------------------------------------------//
	
	static HashMap<String, String> circuitFiles = new HashMap<>();
	static ArrayList<String> testCaseDescription = new ArrayList<>();

	static final int MAX_TRUTH_TABLE_INPUTS = 16;
	static final int MAX_TRUTH_TABLE_OUTPUTS = 32;
	static String keywords[] = { 
		"inputNames", "outputNames", "outputs", "truthTable", 
		"circuit", "circuitInputs", "propagationDelay"
	};
	transient Scanner scan;
	transient String extraToken;
	transient String repeatHead, repeatTail;
	transient int repeatStart, repeatEnd, repeatVal;
	transient int repeatTimes;
	
	//---------------------------------------------------------------------//
	// Circuit Definition
	//---------------------------------------------------------------------//
	
	// Circuit type with inputs and outputs
	String type;
	ArrayList<String> inputNames = new ArrayList<String>();
	ArrayList<String> outputNames = new ArrayList<String>();
	
	// Internal representation of circuit (either truth table or subcircuits)
	boolean isCombinatorial;
	int truthTable[];
	ArrayList<Circuit> internalCircuits = new ArrayList<Circuit>();
	
	//---------------------------------------------------------------------//
	// Circuit Instance
	//---------------------------------------------------------------------//
	
	// Input and Output Setup
	String name;
	int inputArrays[][];      // arrays of input from other circuit instances
	int inputArrayIndices[];  // indices into the input arrays
	int inputs[];             // input values
	//
	int outputArrays[][];     // arrays of input from other circuit instances
	int outputArrayIndices[]; // indices into the input arrays
	int outputs[];            // output values
	
	int propagationDelay[];   // propagationDelay for each output
	
	//---------------------------------------------------------------------//
	// Basic Functions
	//---------------------------------------------------------------------//
	
	Circuit() { isCombinatorial = true; }
	int getInputIndex(String s) { return inputNames.indexOf(s); }
	int getOutputIndex(String s) { return outputNames.indexOf(s); }
	
	//---------------------------------------------------------------------//
	// SIMULATION FUNCTIONS
	//---------------------------------------------------------------------//
	
	void calculateTruthTable()
	{
		for (int i=0; i<internalCircuits.size(); i++) {
			if (!internalCircuits.get(i).isCombinatorial) return;
		}
	
		//System.out.println("Calculating truth table for " + type);
		int numInputs = inputNames.size();
		int numOutputs = outputNames.size();
		int numRows = (int)(Math.pow(2, numInputs));
		int TT[] = new int[numRows];
		
		for (int r=0; r<numRows; r++) {
			for (int i=0; i<numInputs; i++) {
				inputs[i] = (r>>i) & 0x1;
			}
			simulate();
			int val=0;
			for (int i=0; i<numOutputs; i++) {
				val |= outputs[i] << i;
			}
			//System.out.println("  " + r + " " + val);
			TT[r] = val;
		}
		truthTable = TT;
	}
	
	void gatherInputs()
	{
		for (int i=0; i<inputs.length; i++) {
			if (inputArrays[i] != null) {
				inputs[i] = inputArrays[i][inputArrayIndices[i]];
			}
		}
	}
	
	void simulate()
	{
		// Use truth table if we have one
		if (truthTable != null) {
			int row = 0;
			for (int i=0; i<inputs.length; i++) {
				row |= inputs[i] << i;
			}
			int out = truthTable[row];
			for (int i=0; i<outputs.length; i++) {
				outputs[i] = (out >> i) & 0x1;
			}
		}
		// Otherwise simulate the internal circuits
		else {
			for (int i=0; i<internalCircuits.size(); i++) {
				Circuit c = internalCircuits.get(i);
				c.gatherInputs();
				c.simulate();
			}
			for (int i=0; i<outputs.length; i++) {
				outputs[i] = outputArrays[i][outputArrayIndices[i]];
			}
		}
	}
	
	void simulatePropagationDelay()
	{
		// If we have a truth table, use tabular propagation delay from input file
		if (internalCircuits.size() == 0) {
			int maxInputProp = 0;
			for (int i=0; i<inputs.length; i++) {
				if (inputs[i] > maxInputProp) maxInputProp = inputs[i];
			}
			for (int i=0; i<outputs.length; i++) {
				if (propagationDelay != null) {
					outputs[i] = propagationDelay[i] + maxInputProp;
				}
				else {
					outputs[i] = 1 + maxInputProp;
				}
			}
		}
		// Otherwise simulate the delay of the internal circuits
		else {
			for (int i=0; i<internalCircuits.size(); i++) {
				Circuit c = internalCircuits.get(i);
				c.gatherInputs();
				c.simulatePropagationDelay();
			}
			for (int i=0; i<outputs.length; i++) {
				outputs[i] = outputArrays[i][outputArrayIndices[i]];
			}
		}
	}
	
	//---------------------------------------------------------------------//
	// TEST FUNCTIONS
	//---------------------------------------------------------------------//
	
	void runTestCase(String fileName)
	{
		openCircuitFile(fileName); // Open the test file
		String token;
		int numInputs = inputs.length;
		int numOutputs = outputs.length;
		int desiredOutputs[] = new int[numOutputs];
		int totalErrors = 0;
		
		System.out.println("\nRunning test cases from " + fileName + ".txt\n");
		printInputOutputNames();
		testCaseDescription.clear();
		
		while ((token=getToken()) != null) {
			if (token.equals("testCase")) {
				int inCount = 0;
				String s;
				while (inCount < numInputs && (s=getToken()) != null) {
					for (int i=0; i<s.length(); i++) {
						char c = s.charAt(i);
						if (c == '0' || c == '1') {
							inputs[inCount] = c - '0';
							inCount++;
						}
					}
				}
				int outCount = 0;
				while (outCount < numOutputs && ((s=getToken()) != null)) {
					for (int i=0; i<s.length(); i++) {
						char c = s.charAt(i);
						if (c == '0' || c == '1') {
							desiredOutputs[outCount] = c - '0';
							outCount++;
						}
					}
				}
				if (inCount != numInputs || outCount != numOutputs) {
					error("Incomplete test case in test file " + fileName + ".txt");
				}
				simulate();
				
				System.out.println();
				for (int i=0; i<testCaseDescription.size(); i++) {
					System.out.println("##" + testCaseDescription.get(i));
				}
				testCaseDescription.clear();

				System.out.print("Input  : ");
				for (int i=0; i<numInputs; i++) {
					if (i%4==0) System.out.print(" ");
					if (i%32==0 && i>0) System.out.print("\n         ");
					System.out.print(inputs[i]);
				}
				System.out.print("\nDesired: ");
				for (int i=0; i<numOutputs; i++) {
					if (i%4==0) System.out.print(" ");
					System.out.print(desiredOutputs[i]);
				}
				System.out.print("\nActual : ");
				for (int i=0; i<numOutputs; i++) {
					if (i%4==0) System.out.print(" ");
					System.out.print(outputs[i]);
				}
				int numErrors = 0;
				for (int i=0; i<numOutputs; i++) {
					if (outputs[i] != desiredOutputs[i]) numErrors++;
				}
				totalErrors += numErrors;
				if (numErrors > 0) {
					System.out.print("\nErrors : ");
					for (int i=0; i<numOutputs; i++) {
						if (i%4==0) System.out.print(" ");
						if (outputs[i] != desiredOutputs[i]) System.out.print("^");
						else System.out.print(" ");
					}
					System.out.print("\nError Names: ");
					for (int i=0; i<numOutputs; i++) {
						if (outputs[i] != desiredOutputs[i]) {
							System.out.print(outputNames.get(i) + " ");
						}
					}
				}
				System.out.println("");
			}
			else if (token.equals("}")) {
				continue;
			}
			else {
				System.out.println("\nError in test file '" + fileName +
					".txt': Unknown command '" + token + "'.\n");
				System.exit(0);
			}
		}
		if (totalErrors > 0) {
			System.out.println("\n***** " + totalErrors + " ERRORS FOUND. *****");
		}
		else {
			System.out.println("\n***** ALL TEST CASES PASSED. *****");
		}
	}
	
	void printTruthTable(int maxRows)
	{
		int numInputs = inputNames.size();
		int numOutputs = outputNames.size();
		int rowsToPrint = (int)(Math.pow(2, numInputs));
		if (rowsToPrint > maxRows || numInputs >= 32) rowsToPrint = maxRows;
		
		System.out.println("\nTRUTH TABLE:");
		printInputOutputNames();
		
		for (int r=0; r<rowsToPrint; r++) {
			for (int i=0; i<numInputs; i++) {
				int sh = (numInputs-1-i);
				inputs[i] = (r>>sh) & 0x1;
			}
			simulate();
			if (r%8 == 0) System.out.println();
			printInputOutput();
		}
	}
	
	void printInputOutputNames() 
	{
		int numInputs = inputNames.size();
		int numOutputs = outputNames.size();
		
		for (int i=0; i<numInputs; i++) {
			System.out.print(inputNames.get(i) + " ");
		}
		System.out.print("| ");
		for (int i=0; i<numOutputs; i++) {
			System.out.print(outputNames.get(i) + " ");
		}
		System.out.println("");
	}
	
	void printInputOutput()
	{
		int numInputs = inputNames.size();
		int numOutputs = outputNames.size();
		
		for (int i=0; i<numInputs; i++) {
			System.out.print(inputs[i]);
			if (i%4==3) System.out.print(" ");
		}
		System.out.print(" | ");
		for (int i=0; i<numOutputs; i++) {
			System.out.print(outputs[i]);
			if (i%4==3) System.out.print(" ");
		}
		System.out.println("");
	}
	
	//---------------------------------------------------------------------//
	// PARSING FUNCTIONS
	//---------------------------------------------------------------------//
	
	void error(String s)
	{
		System.out.println("\nError in file '" + type + ".txt'!\n  " + s + "\n");
		System.exit(0);
	}
	
	void openCircuitFile(String circuitType)
	{
		String circuitFileName = "";
		try {
			circuitFileName = circuitType + ".txt";

			if (!circuitFiles.containsKey(circuitFileName))
			{
				scan = new Scanner(new File(circuitFileName));
				//System.out.println(circuitType);
				scan.useDelimiter("\\Z"); // end of file marker
				String circuitFileText = scan.next();
				circuitFiles.put(circuitFileName, circuitFileText);
			}

			scan = new Scanner(circuitFiles.get(circuitFileName));
			scan.useDelimiter("[ ,{;=\t\n\r]"); // : and [] treated separately

		} catch (Exception ex) {
			System.out.println("\nError opening file '" + circuitFileName + "'.\n");
			System.exit(0);
		}
	}
	
	String getRepeatToken()
	{
		// Handle simple repeats case 0[4] for example
		if (repeatTimes > 0) {
			String token = repeatHead;
			repeatTimes--;
			if (repeatTimes == 0) {
				repeatHead = null;
				repeatTail = null;
			}
			return token;
		}

		// Handle changing value cases a[1:7] for example 
		String token = repeatHead + repeatVal + repeatTail;
		if (repeatVal > repeatEnd) {
			repeatVal--;
		} 
		else if (repeatVal < repeatEnd) {
			repeatVal++;
		} 
		else {
			repeatHead = null;
			repeatTail = null;
		}
		//System.out.println(token);
		return token;
	}
	
	void initRepeatToken(String s)
	{
		try {
			if (!s.contains(":")) { // 0[7] case
				String[] tokens = s.split("[\\[\\]:]+");
				repeatHead = tokens[0];
				repeatTail = "";
				repeatTimes = Integer.parseInt(tokens[1]);
				return;
			}
			else if (s.charAt(0) == '[') { // [3:4]X case
				String[] tokens = s.substring(1).split("[\\[\\]:]+"); // cut off initial [
				repeatHead = "";
				repeatStart = Integer.parseInt(tokens[0]);
				repeatEnd = Integer.parseInt(tokens[1]);
				repeatTail = tokens[2];
				repeatVal = repeatStart;
				return;
			} else { // X[3:4], X[3:4]Y cases
				String[] tokens = s.split("[\\[\\]:]+");
				repeatHead = tokens[0];
				repeatStart = Integer.parseInt(tokens[1]);
				repeatEnd = Integer.parseInt(tokens[2]);
				if (tokens.length > 3) repeatTail = tokens[3];
				else repeatTail = "";
				repeatVal = repeatStart;
				return;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		error("Illegal repeater: '" + s + "'.");	
	}

	String getToken()
	{
		String t = getToken2();
		//System.out.println(t);
		return t;
	}
	
	String getToken2()
	{
		if (repeatHead != null) {
			return getRepeatToken();
		}
		else if (extraToken != null) {
			String t = extraToken;
			extraToken = null;
			return t;
		}
		while (scan.hasNext() || extraToken != null) {
			String token = scan.next();
			if (token.length() == 0) {
				continue;
			}
			else if (token.startsWith("##")) {
				testCaseDescription.add(scan.nextLine());
			}
			else if (token.startsWith("#")) {
				scan.nextLine();
			}
			else if (token.equals("}")) {
				return token;
			}
			else if (token.endsWith("}")) {
				extraToken = "}";
				String t = token.substring(0, token.length()-1);
				if (t.contains("[")) {
					initRepeatToken(t);
					return getRepeatToken();
				} else {
					return t;
				}
			}
			else if (token.contains("[")) {
				initRepeatToken(token);
				return getRepeatToken();
			}
			else {
				return token;
			}
		}
		return null;
	}
	
	void ungetToken(String token) 
	{
		extraToken = token; // currently can only unget 1 token
	}
	
	boolean isKeyword(String s)
	{
		for (int i=0; i<keywords.length; i++) {
			if (keywords[i].equals(s)) return true;
		}
		return false;
	}
	
	void loadStringList(ArrayList<String> stringList)
	{
		String token;
		while ((token=getToken()) != null) {
			if (isKeyword(token)) {
				ungetToken(token);
				break;
			}
			if (token.equals("}")) break;
			stringList.add(token);
		}
	}
	
	boolean getArrayAndIndex(ArrayIndex ai, String str)
	{
		ai.arr = null;
		ai.idx = -1;
		
		// See if we are an input
		int index = inputNames.indexOf(str);
		if (index >= 0) {
			ai.arr = inputs;
			ai.idx = index;
			return true;
		}
		
		// Try to find an output of a subunit
		int dotIndex = str.indexOf('.');
		String strSuffix = null; // Whatever is past the dot
		if (dotIndex>0) strSuffix = str.substring(dotIndex+1);
		
		for (int j=0; j<internalCircuits.size(); j++) {
			Circuit d = internalCircuits.get(j);
			if (str.equals(d.name) || str.startsWith(d.name+".")) {
				ai.arr = d.outputs;
				if (strSuffix == null) { // no suffix, so use index 0
					if (d.outputNames.size() > 1) {
						error("Unqualified circuit output for circuit "
							+ "with multiple outputs: '" + str + "'");
					}
					ai.idx = 0; 
					return true;
				}
				else {
					// First check for a name
					ai.idx = d.outputNames.indexOf(strSuffix);
					if (ai.idx >= 0) {
						return true;
					}
					try {
						ai.idx = Integer.parseInt(strSuffix);
						return true;
					} 
					catch (Exception ex) {
						return false;
					}
				}
			}
			//
		}
		return false;
	}
	
	void loadSubCircuit(boolean inputsOnly, String indent)
	{
		ArrayIndex ai = new ArrayIndex();
		String subType = getToken();
		String subName = getToken();
		Circuit c = null;
		for (int i=0; i<inputNames.size(); i++) {
			String inputName = inputNames.get(i);
			if (inputName.equals(subName)) {
				error("Subcircuit has same name as input: " + subName);
			}
		}
		for (int i=0; i<internalCircuits.size(); i++) {
			Circuit ci = internalCircuits.get(i);
			if (ci.name.equals(subName)) {
				if (!inputsOnly) {
					error("Duplicate subcircuit name: " + subName);
				}
				c = ci;
				break;
			}
		}
		if (c == null) {
			c = new Circuit();
			c.loadFromFile(subType, indent+"  ");
			c.name = subName;
			internalCircuits.add(c);
		}
		
		// connect up inputs to internal circuits
		for (int i=0; i<c.inputNames.size(); i++) {
			String inName = getToken();
			if (inName.equals("}")) {
				error("Not enough inputs for " + subType + " " + subName);
			}
			//
			if (inName.equals("...")) {
				isCombinatorial = false;
				break;
			}
			else if (getArrayAndIndex(ai, inName)) {
				c.inputArrays[i] = ai.arr;
				c.inputArrayIndices[i] = ai.idx;
			}
			else if (inName.equals("0") || inName.equals("1")) {
				c.inputs[i] = Integer.parseInt(inName);
			}
			else {
				error("Could not find input '" + 
					inName + "' for " + subType + " " + subName);
			}
		}

		String t = getToken();
		if (!t.equals("}")) {
			error("Too many inputs for " + subType + " " + subName);
		}
	}
	
	void loadOutputs()
	{
		ArrayIndex ai = new ArrayIndex();
		
		for (int i=0; i<outputNames.size(); i++) {
			String outputName = getToken();
			if (outputName==null || outputName.equals("}")) {
				error("Incomplete circuit outputs.");
			}
			
			if (getArrayAndIndex(ai, outputName)) {
				outputArrays[i] = ai.arr;
				outputArrayIndices[i] = ai.idx;
			}
			else {
				error("Could not find output '" + outputName);
			}
		}

		String t = getToken();
		if (!t.equals("}")) {
			error("too many outputs for circuit.");
		}
	}
	
	void loadTruthTable()
	{
		String token;
		int numInputs = inputNames.size();
		int numOutputs = outputNames.size();
		int numRows = (int)(Math.pow(2, numInputs));
		truthTable = new int[numRows];
		
		for (int i=0; i<numRows; i++) {
			int index=0;
			int j=0;
			while (j<numInputs) {
				token = getToken();
				if (token == null) {
					error("Could not load truth table.");
				}
				for (int k=0; k<token.length(); k++) {
					if (token.charAt(k) == '1') {
						index |= (1<<j);
						j++;
					}
					else if (token.charAt(k) == '0') {
						j++;
					}
				}
			}
			//
			int value=0;
			j=0;
			while (j<numOutputs) {
				token = getToken();
				if (token == null) {
					error("Could not load truth table");
				}
				for (int k=0; k<token.length(); k++) {
					if (token.charAt(k) == '1') {
						value |= (1<<j);
						j++;
					}
					else if (token.charAt(k) == '0') {
						j++;
					}
				}
			}
			//
			truthTable[index] = value;
			//System.out.println(value);
		}
		while (true) {
			token = getToken();
			if (token == null) 
				error("Missing } after truth table.");
			if (token.equals("}")) break;
		}
	}
	
	static int loadCount = 0;
	boolean loadFromFile(String circuitType, String indent)
	{
		//System.out.println(indent + circuitType);
		//System.out.print(".");
		//System.out.print("\rLoading: " + circuitType + "        ");
		System.out.print("\rCircuits loaded: " + (loadCount++) + "  ");
		
		type = circuitType;
		openCircuitFile(circuitType);
		String token;
		boolean outputsLoaded = false;

		while ((token=getToken()) != null) {
			//System.out.println(token);
			if (token.equals("inputNames")) {
				loadStringList(inputNames);
				inputArrays = new int[inputNames.size()][];
				inputArrayIndices = new int[inputNames.size()];
				inputs = new int[inputNames.size()];
			}
			else if (token.equals("outputNames")) {
				loadStringList(outputNames);
				outputArrays = new int[outputNames.size()][];
				outputArrayIndices = new int[outputNames.size()];
				outputs = new int[outputNames.size()];
			}
			else if (token.equals("circuit")) {
				if (inputNames.size()==0 || outputNames.size()==0) {
					error("inputNames and outputNames must come before circuits."); 
				}
				loadSubCircuit(false, indent);
			}
			else if (token.equals("circuitInputs")) {
				loadSubCircuit(true, indent);
			}
			else if (token.equals("outputs")) {
				outputsLoaded = true;
				loadOutputs();
			}
			else if (token.equals("truthTable")) {
				outputsLoaded = true;
				loadTruthTable();
			}
			else if (token.equals("propagationDelay")) {
				ArrayList<String> props = new ArrayList<String>();
				loadStringList(props);
				propagationDelay = new int[outputNames.size()];
				for (int i=0; i<propagationDelay.length; i++) {
					propagationDelay[i] = Integer.parseInt(props.get(i));
				}
			}
			else {
				error("Unkown command: '" + token + "'");
			}
		}

		if (!outputsLoaded) error("'outputs' or 'truthTable' required.");

		scan.close();
		scan = null;
		extraToken = null;
		
		if (isCombinatorial && truthTable == null 
				&& inputs.length <= MAX_TRUTH_TABLE_INPUTS 
				&& outputs.length <= MAX_TRUTH_TABLE_OUTPUTS) {
			calculateTruthTable();
		}
		
		return false;
	}
}
