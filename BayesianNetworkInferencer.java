import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.io.*;

public class BayesianNetworkInferencer
{	
	public static final String nodeOrder[] = {"W", "S", "R", "WG", "SR"};
	
	public static void main(String[] args)
	{	
		List<NetworkNode> networkTree = new ArrayList<NetworkNode>();
		//define nodes with names
		NetworkNode W = new NetworkNode("W");
		
		NetworkNode S = new NetworkNode("S");
		NetworkNode R = new NetworkNode("R");
		
		NetworkNode WG = new NetworkNode("WG");
		NetworkNode SR = new NetworkNode("SR");
		
		
		//set parents and children
		W.setChildren(new NetworkNode[] {W, S});
		
		S.setParents(new NetworkNode[] {W});
		R.setParents(new NetworkNode[] {W});
		S.setChildren(new NetworkNode[] {WG});
		R.setChildren(new NetworkNode[] {WG});
		
		WG.setParents(new NetworkNode[] {S, R});
		SR.setParents(new NetworkNode[] {R});
		
		//assign probability tables
		W.setProbabilities(new float[] {0.6f});
		
		S.setProbabilities(new float[] {0.75f, 0.2f});
		R.setProbabilities(new float[] {0.1f, 0.8f});
		
		WG.setProbabilities(new float[] {0f, 0.8f, 0.9f, 0.95f});
		SR.setProbabilities(new float[] {0f, 0.7f});
		
		//add the nodes to the networkTree
		networkTree.add(W);
		networkTree.add(S);
		networkTree.add(R);
		networkTree.add(WG);
		networkTree.add(SR);
		
		//query for input
		System.out.println("Welcome to the Bayesian Network Inferencer."
				+ "\nStart by inputting a query in the form of S=?|W=?, R=?"
				+ "\nand fill in the ?'s with T or F."
				+ "\nEnter 'STOP' when you are done."
				+ "\nInput Here: ");
		
		Scanner scanner = new Scanner(System.in);
		String query = scanner.nextLine();
		
		while (!query.toUpperCase().equals("STOP"))
		{
			float result = exactInference(query, networkTree); 
			System.out.println("\n\nThat probability is " + result
					+ "\nInput Here: ");
			query = scanner.nextLine();
		}
		
		System.out.println("\n\nExit");
		
	}
	
	public static class Variable
	{
		String name;
		//-1 for unassigned, 0 for false, 1 for true
		int val;
		NetworkNode correspondingNode;
		
		Variable(String name, int val)
		{
			this.name = name;
			this.val = val;
		}
		
		Variable(String name, int val, NetworkNode correspondingNode)
		{
			this.name = name;
			this.val = val;
			this.correspondingNode = correspondingNode;
		}
		
		Variable(Variable other)
		{
			this.name = new String(other.name);
			this.val = other.val;
			this.correspondingNode = other.correspondingNode;
		}
	}
	
	public static class NetworkNode
	{
		String name;
		NetworkNode parents[];
		NetworkNode children[];
		
		//What are the probabilities that this NetworkNode is true? the indexes are the binary values of the parents being true or false
		float trueProbabilities[];
		
		public NetworkNode(String name)
		{
			this.name = name;
		}
		
		public void setChildren(NetworkNode children[])
		{
			this.children = children;
		}
		
		public void setParents(NetworkNode parents[])
		{
			this.parents = parents;
		}
		
		public void setProbabilities(float trueProbabilities[])
		{
			this.trueProbabilities = trueProbabilities;
		}
		
		public float calcProbability(boolean nodeIsTrue, int rowIndex)
		{
			if (nodeIsTrue)
			{
				return trueProbabilities[rowIndex];
			}
			else
			{
				return  1 - trueProbabilities[rowIndex];
			}
		}
	}
	
	public static float exactInference(String expression, List<NetworkNode> networkTree)
	{
		//remove whitespace characters
		String noWhiteSpace = expression.replaceAll("\\s+","");
		
		//parse the expression into two sides
		String splitByGiven[] = noWhiteSpace.split("\\|");
		String probabilityToCalc = splitByGiven[0];
		String given = splitByGiven[1];
		
		//split the left hand side by "," and assign to lHandVariables
		String lHandStrings[] = probabilityToCalc.split(",");
		Variable lHandVariables[] = new Variable[lHandStrings.length];
		for (int i = 0; i < lHandStrings.length; i++)
		{
			String splitByEquals[] = lHandStrings[i].split("=");
			String variableName = splitByEquals[0].toUpperCase();
			int variableValue = -1;
			switch (splitByEquals[1].toUpperCase())
			{
				case "T":
					variableValue = 1;
					break;
				case "F":
					variableValue = 0;
					break;
			}
			lHandVariables[i] = new Variable(variableName, variableValue);
		}
		
		//split the right hand side by "," and assign to rHandVariables
		String rHandStrings[] = given.split(",");
		Variable rHandVariables[] = new Variable[rHandStrings.length];
		for (int i = 0; i < rHandStrings.length; i++)
		{
			String splitByEquals[] = rHandStrings[i].split("=");
			String variableName = splitByEquals[0].toUpperCase();
			int variableValue = -1;
			switch (splitByEquals[1].toUpperCase())
			{
				case "T":
					variableValue = 1;
					break;
				case "F":
					variableValue = 0;
					break;
			}
			rHandVariables[i] = new Variable(variableName, variableValue);
		}
		
		//find the top part of the equation
		Variable topPart[] = new Variable[rHandVariables.length + lHandVariables.length];
		for (int i = 0; i < lHandVariables.length; i++)
		{
			topPart[i] = new Variable(lHandVariables[i]);
		}
		int newStartingIdx = lHandVariables.length;
		for (int i = 0; i < rHandVariables.length; i++)
		{
			topPart[i + newStartingIdx] = new Variable(rHandVariables[i]);
		}
		
		//find the bottom part of the equation
		Variable bottomPart[] = new Variable[rHandVariables.length];
		for (int i = 0; i < rHandVariables.length; i++)
		{
			bottomPart[i] = new Variable(rHandVariables[i]);
		}
		
		//find the corresponding NetworkNodes for the top part of the equation variables
		for (int i = 0; i < topPart.length; i++)
		{
			//search the networkTree for the corresponding NetWorkNode for this variable at index i
			boolean found = false;
			Iterator<NetworkNode> it = networkTree.iterator();
			while (it.hasNext() && !found)
			{
				NetworkNode currentNode = it.next();
				if(currentNode.name.equals(topPart[i].name))
				{
					//Add the corresponding NetworkNode (shallow copy)
					topPart[i].correspondingNode = currentNode;
					found = true;
				}
			}
		}
		
		//find the corresponding NetworkNodes for the bottom part of the equation variables
		for (int i = 0; i < bottomPart.length; i++)
		{
			//search the networkTree for the corresponding NetWorkNode for this variable at index i
			boolean found = false;
			Iterator<NetworkNode> it = networkTree.iterator();
			while (it.hasNext() && !found)
			{
				NetworkNode currentNode = it.next();
				if(currentNode.name.equals(bottomPart[i].name))
				{
					//Add the corresponding NetworkNode (shallow copy)
					bottomPart[i].correspondingNode = currentNode;
					found = true;
				}
			}
		}
		
		
		//calculate the top part
		float topPartSolution = solveForVariables(topPart);
		
		//calculate the bottom part
		float bottomPartSolution = solveForVariables(bottomPart);
		
		//calculate result
		return topPartSolution / bottomPartSolution;
	}
	
	public static float solveForVariables(Variable varsToSolve[])
	{
		List<Variable> equationVariables = new ArrayList<Variable>();
		
		//initialize the equationVariables with the varsToSolve
		for (int i = 0; i < varsToSolve.length; i++)
		{
			equationVariables.add(varsToSolve[i]);
		}
		
		//find the parents of the varsToSolve
		//add a single copy of each of the parents
		for (int i = 0; i < varsToSolve.length; i++)
		{
			addParents(equationVariables, varsToSolve[i]);
		}
		
		
		
		//call recursive solve with first index of nodeOrder
		return solveForVariable(0, equationVariables);
	}
	
	public static void addParents(List<Variable> variableListToModify, Variable varToSearch)
	{
		NetworkNode parents[] = varToSearch.correspondingNode.parents;
		//if there are parents, then add them
		if (parents != null)
		{
			for (int i = 0; i < parents.length; i++)
			{
				//search for the current parent's name in the variableListToModify
				boolean found = false;
				Iterator<Variable> it = variableListToModify.iterator();
				while (it.hasNext() && !found)
				{
					String currentParentName = it.next().name;
					if (currentParentName.equals(parents[i].name))
					{
						found = true;
					}
				}
				
				//if we didn't find it, then add this parent as a variable
				if (!found) 
				{
					//add the parent
					Variable newVarToSearch = new Variable(parents[i].name, -1, parents[i]);
					variableListToModify.add(newVarToSearch);
					//add the parents of this parent
					addParents(variableListToModify, newVarToSearch);
				}
			}
		}
	}
	
	public static float solveForVariable(int nodeOrderIndex, List<Variable> equationVariables)
	{
		//Recursive(equationVariable):
		//if (equationVariable is empty string)
		//	return 1
		//
		//if (equationVariable is not used)
		//	return recursive(next in line)
		//
		//if (equationVariable is assigned)
		//	multiplicand = the calculated probability for the assigned values
		//	return multiplicand * recursive(next in line)
		//else
		//	assign it to true
		//	multiplicand = the calculated probability for the assigned values
		//	result1 = multiplicand * recursive(next in line)
		//
		//	assign it to false
		//	multiplicand = the calculated probability for the assigned values
		//	result2 = multiplicand * recursive(next in line)
		//
		//	return result1 + result 2
		
		//BASE CASE
		//if there is no more in the nodeOrder array, return 1
		if(nodeOrderIndex == nodeOrder.length)
		{
			return 1;
		}
		
		//search for the name at nodeOrderIndex in equationVariables
		Variable equationVar = null;
		boolean found = false;
		Iterator<Variable> it = equationVariables.iterator();
		while (it.hasNext() && !found)
		{
			Variable currentVar = it.next();
			if (nodeOrder[nodeOrderIndex].equals(currentVar.name))
			{
				found = true;
				equationVar = currentVar;
			}
		}
		
		//if we didn't find the name at nameOrderIndex, return 1 * solveForVariable(nextIndex)
		if (!found)
		{
			return solveForVariable(nodeOrderIndex + 1, equationVariables);
		}
		else
		{
			//if the value of this variable is known, go ahead and cacluate
			if (equationVar.val != -1)
			{
				return findProbability(equationVar, equationVariables) * solveForVariable(nodeOrderIndex + 1, equationVariables);
			}
			//otherwise, do a summation
			else
			{
				//make a deep copy of the equationVariables
				List<Variable>equationVariablesCopy = new ArrayList<Variable>();
				Variable equationVarCopy = null;
				for (Variable v : equationVariables)
				{
					Variable variableToAdd = new Variable(v);
					equationVariablesCopy.add(variableToAdd);
					if (v.name.equals(equationVar.name))
					{
						equationVarCopy = variableToAdd;
					}
				}
				
				//do the summation for the variable's true and false values, use the deep copy as the new equationVariables
				equationVarCopy.val = 1;
				if (equationVarCopy.name.equals("S"))
					System.out.println();
				float result1 = findProbability(equationVarCopy, equationVariablesCopy) * solveForVariable(nodeOrderIndex + 1, equationVariablesCopy);
				
				equationVarCopy.val = 0;
				float result2 = findProbability(equationVarCopy, equationVariablesCopy) * solveForVariable(nodeOrderIndex + 1, equationVariablesCopy);
				
				return result1 + result2;
			}
		}
	}
	
	public static float findProbability(Variable equationVar, List<Variable> equationVariables)
	{
		
		//find the parents of the variable
		NetworkNode varParentNodes[] = equationVar.correspondingNode.parents;
		int probabilityIndex = 0;
		
		//if the variable has parents, assign the probability index
		if (varParentNodes != null)
		{
			Variable parentVariables[] = new Variable[varParentNodes.length];
			
			//search for the corresponding variables for the parents
			for (int i = 0; i < varParentNodes.length; i++)
			{
				boolean found = false;
				Iterator<Variable> it = equationVariables.iterator();
				while (it.hasNext() && !found)
				{
					Variable currentVar = it.next();
					if (varParentNodes[i].name.equals(currentVar.name))
					{
						parentVariables[i] = currentVar;
					}
				}
			}
			
			//assign the probability index that will be used to calculate probability
			int parentCounter = 0;
			for (Variable v : parentVariables)
			{
				probabilityIndex += v.val * (int)Math.pow(2, parentVariables.length - 1 - parentCounter);
				parentCounter++;
			}
		}
		
		//get the probability at the probability index
		if (equationVar.val == 1)
		{
			return equationVar.correspondingNode.trueProbabilities[probabilityIndex];
		}
		else
		{
			//subtract from 1 if probability of false
			return 1 - equationVar.correspondingNode.trueProbabilities[probabilityIndex];
		}
		
	}

}
