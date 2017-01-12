
// CircuitSim.java
// by David Cline

// Make jar file: jar cvfe CircuitSim.jar CircuitSim *.class

public class CircuitSim
{
	public static void main(String args[])
	{
		// Check to see if we have enough args
		if (args.length < 1) {
			System.out.println("Usage: circuitName [testFile]");
			System.exit(0);
		}
		
		// Massage the circuit name to get rid of .txt if necessary
		String circuitName = args[0];
		if (circuitName.endsWith(".txt")) {
			circuitName = circuitName.substring(0,circuitName.length()-4);
		}
		
		// Load the circuit
		Circuit c = new Circuit();
		c.loadFromFile(circuitName, "");
		System.out.println("");
		
		// Propagation delay 
		for (int i=0; i<c.inputs.length; i++) c.inputs[i]=0;
		c.simulatePropagationDelay();
		System.out.print("Propagation Delays: ");
		for (int i=0; i<c.outputs.length; i++) System.out.print(c.outputs[i] + " ");
		System.out.println();
		
		// If there are no more arguments, print truth table lines.
		if (args.length == 1) {
			c.printTruthTable(1024);
		}
		
		// Otherwise we have an input, run the circuit on it
		else if (args[1].charAt(0) == '0' || args[1].charAt(0) == '1') {
			int inputNum = 0;
			for (int argNum=1; argNum<args.length; argNum++) {
				String arg = args[argNum];
				for (int i=0; i<arg.length(); i++) {
					if (inputNum >= c.inputs.length) break;
					char d = arg.charAt(i);
					if (d=='0' || d=='1') {
						c.inputs[inputNum] = d - '0';
						inputNum++;
					}
				}
			}
			c.simulate();
			System.out.println("");
			c.printInputOutputNames();
			c.printInputOutput();
		}
		
		// Otherwise we have a test file
		else {
			String testFile = args[1];
			if (testFile.endsWith(".txt")) {
				testFile = testFile.substring(0,testFile.length()-4);
			}
			c.runTestCase(testFile);
		}
	}
}

