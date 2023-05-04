package org.krasnow.cng.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.krasnow.cng.domain.Asymmetry;
import org.krasnow.cng.domain.BinaryTreeNode;
import org.krasnow.cng.domain.LinkedQueue;
import org.krasnow.cng.domain.LinkedStack;
import org.krasnow.cng.domain.ParentedBinaryTreeNode;
import org.krasnow.cng.domain.SwcDataNode;

/**
 * @author gillette
 *
 */
public class BinaryTreeUtils {


	// Tree must already have caulescence calculated
	public static void findMainPathBifurcations(BinaryTreeNode tree)
	throws Exception{
		findMainPathBifurcations(tree,Asymmetry.METRIC_NODES);
	}
	public static void findMainPathBifurcations(BinaryTreeNode tree, int metric)
	throws Exception{
		if (tree.getAsymmetry(metric) == null){
			throw new Exception("Caulescence has not yet been found for this tree");
		}
		BinaryTreeNode node = tree, smallerChild, tmp;
		List considerList = new ArrayList();
		// holdQueue contains smaller side nodes that have been added to main branch, await traversal
		LinkedQueue holdQueue = new LinkedQueue();
		Asymmetry treeCaul = tree.getAsymmetry(metric), nodeCaul, smallCaul;
		int numMainPathes = 1, i, insertIndex;
		// Goes down main branch looking for smaller sides with higher C than entire tree
		while (!holdQueue.isEmpty() || node != null){
			// If node is null (reached end of the line) go to other tree with highest C
			if (node == null || !node.hasChildren()){
				node = (BinaryTreeNode)holdQueue.dequeue();
			}
			nodeCaul = node.getAsymmetry(metric);
			// determine smaller child branch
			smallerChild = node.getSmallerSubtree();
			smallCaul = smallerChild.getAsymmetry(metric);

			// If smaller side node's caulescence is higher than tree's, consider it
			if (smallerChild.hasChildren() &&
					getMergedCaulescence(treeCaul, nodeCaul, smallCaul) > treeCaul.getCaulescence()){
				// Add to consider list in correct order (highest caul to lowest)
				insertIndex = 0;
				for (i = 0; i < considerList.size(); i++){
					// Gets considerable branch point 
					tmp = (BinaryTreeNode)considerList.get(i);
					// Gets smaller subtree for caulescence value testing
					tmp = tmp.getSmallerSubtree();
					// If smaller tree caulescence higher than that in list, add in front of it
					if (smallCaul.getCaulescence() > tmp.getAsymmetry().getCaulescence()){
						insertIndex = i;
						i = considerList.size(); // Stops loop
					}
				}
				considerList.add(insertIndex,node);

				// If no other nodes on hold, add highest caulescence sub-tree to entire tree
				if (holdQueue.isEmpty()){
					// Highest caul to add is first
					node = (BinaryTreeNode)considerList.remove(0);
					smallCaul = node.getSmallerSubtree().getAsymmetry(metric);
					treeCaul.setLargerSummedSize(
						treeCaul.getLargerSummedSize() 
						- node.getAsymmetry().getLargerSize()
						+ smallCaul.getLargerSummedSize());
					treeCaul.setAllSummedSize(
						treeCaul.getAllSummedSize() 
						- node.getAsymmetry().getTotalSize()
						+ smallCaul.getAllSummedSize());
					// increment tree main branches
					numMainPathes++;
					node.getAsymmetry().setMainPath(Asymmetry.MAIN_BRANCH_BOTH);
					// recheck consider list, make sure they're still above treeCaul
					for (i = 0; i < considerList.size(); i++){
						tmp = (BinaryTreeNode)considerList.get(i);
						nodeCaul = tmp.getAsymmetry(metric);
						smallCaul = tmp.getSmallerSubtree().getAsymmetry(metric);
						if (treeCaul.getCaulescence() > 
							getMergedCaulescence(treeCaul, nodeCaul, smallCaul)){
							considerList.remove(i);
							i--;
						}
					}
					// then add smaller side to holdQueue and traverse larger side
					if (node.getSmallerSubtree().hasChildren()){
						holdQueue.enqueue(node.getSmallerSubtree());
					}
					node = node.getLargerSubtree();
				}
				// go to next held node
				else{
					node = (BinaryTreeNode)holdQueue.dequeue();
				}
			}
			// Not worth considering, keep going down main branch
			else{
				node = node.getLargerSubtree();
				if (!node.hasChildren()){
					node = null;
				}
			}
		}
		treeCaul.setNumMainPathes(numMainPathes);
	}
	
	private static double getMergedCaulescence(
			Asymmetry treeCaul, Asymmetry parentCaul, Asymmetry smallCaul){
		Asymmetry trialCaul = new Asymmetry();
		trialCaul.setLargerSummedSize(
			treeCaul.getLargerSummedSize() 
			- parentCaul.getLargerSize()
			+ smallCaul.getLargerSummedSize());
		trialCaul.setAllSummedSize(
			treeCaul.getAllSummedSize() 
			- parentCaul.getTotalSize()
			+ smallCaul.getAllSummedSize());
		return trialCaul.getCaulescence();
	}
	
	// Tree must already have caulescence calculated
	public static void findMainPathBifurcationsViaNodes(ParentedBinaryTreeNode tree) throws Exception{
		findMainPathBifurcationsViaNodes(tree, Asymmetry.METRIC_NODES);
	}
	public static void findMainPathBifurcationsViaNodes(ParentedBinaryTreeNode tree, int metric) throws Exception{
		if (tree.getAsymmetry(metric) == null){
			throw new Exception("Caulescence has not yet been found for this tree");
		}
		LinkedStack nodeStack = getLeavesToRootStack(tree);
		Set checkedNodes = getMainPathNodes(tree);
		
		List topNodeList = new LinkedList();
		double treeCaul = tree.getAsymmetry().getCaulescence();
		ParentedBinaryTreeNode node, parent;
		int i;
		boolean notDone = true;
		Asymmetry testCaul;
		//System.out.println("FindMainPathBifurcationsViaNodes: "+tree+"; caul: "+treeCaul);
		
		while (!nodeStack.isEmpty()){
			node = (ParentedBinaryTreeNode)nodeStack.pop();
			if (false && node.hasChildren()){
				System.out.println("Node: "+node+"; checked? "+checkedNodes.contains(node)
					+"; partitionCaulescence: "+node.getAsymmetry().getPartitionAsymmetry());
			}
			/* Only add node if it's not already a main branch (or determined it won't be), 
			 * has children, and has a higher partision caulescence than the whole tree caulescence */
			if (!checkedNodes.contains(node)
					&& node.hasChildren() 
					&& node.getAsymmetry().getPartitionAsymmetry() > treeCaul){
				for (i = 0; i < topNodeList.size(); i++){
					if (node.getAsymmetry().getPartitionAsymmetry() >
						((BinaryTreeNode)topNodeList.get(i)).getAsymmetry().getPartitionAsymmetry()){
						topNodeList.add(i, node);
						i = topNodeList.size()+1;
					}
				}
				if (i == 0 || i == topNodeList.size()+1){
					topNodeList.add(node);
				}
			}
		}
		
		boolean isLeft;
		while (!topNodeList.isEmpty() && notDone){
			//System.out.println("Not done; "+topNodeList.size());
			node = (ParentedBinaryTreeNode)topNodeList.remove(0);
			// Node may have been added already
			if (!checkedNodes.contains(node)
					&& node.getAsymmetry().getPartitionAsymmetry() > treeCaul){
				parent = node.getParent();
				isLeft = node.isLeft();
				node.detach(false);
				findMainPathBifurcationsViaNodes(node, metric);
				if (isLeft){
					parent.setLeft(node, false);
				}
				else{
					parent.setRight(node, false);
				}
				testCaul = testMergeUp(node, metric);
				//System.out.println("TestCaul: "+testCaul.getCaulescence());
				
				// Add this tree to main branch
				if (testCaul.getCaulescence() > treeCaul){
					checkedNodes.addAll(mergeUp(node, metric));
					treeCaul = testCaul.getCaulescence();
				}
				// Add this node and all nodes below to checked list, plus mergeUp nodes
				checkedNodes.addAll(getAllNodesSet(node));
			}
			else{
				notDone = false;
			}
		}
	}
	
	private static ParentedBinaryTreeNode trySelectedPaths(
			ParentedBinaryTreeNode tree, List onOffList, Set mainBranchNodes, int metric)
	throws Exception{
		BigInteger onOffBits = BigInteger.ZERO;
		int i;
		//System.out.println("Toggle Nodes: "+onOffList.size());
		// Create bit sequence of 1s, length of the onOffList
		for (i = 0; i < onOffList.size(); i++){
			onOffBits = onOffBits.shiftLeft(1).or(BigInteger.ONE);
		}

		// keeping track of best tree caulescence
		double bestCaul = tree.getAsymmetry().getCaulescence();
		ParentedBinaryTreeNode treeCopy, bestTree = tree;
		ExhaustiveBean bean;
		boolean skipCase;
		//System.out.println("Initial caul: "+bestCaul);
		//BigInteger divBy = new BigInteger("100000");
		int numOn;
		// while still nodes/branches to toggle
		while (onOffBits.compareTo(BigInteger.ZERO) > 0){
			skipCase = false;
			numOn = 1;
			// check dependencies - don't run if dependencies fail
			for (i = 0; i < onOffList.size(); i++){
				// Only check if this node is toggled on
				/*if (onOffBits.remainder(divBy).compareTo(BigInteger.ZERO) == 0){
					System.out.println(isNodeOn(i,onOffBits));
				}*/
				if (isNodeOn(i,onOffBits)){
					bean = (ExhaustiveBean)onOffList.get(i);
					if (bean.getRequiredOnIndex() != -1 &&
							!isNodeOn(bean.getRequiredOnIndex(),onOffBits)){
						skipCase = true;
						i = onOffList.size();
					}
					numOn++;
				}
			}
			if (!skipCase){
				treeCopy = copyParentedTree(tree);
				if (treeCopy.getAsymmetry().getCaulescence() != tree.getAsymmetry().getCaulescence()){
					System.out.println("Caulescences of orig & copy do not match: "
							+tree.getAsymmetry().getCaulescence()+" " 
							+treeCopy.getAsymmetry().getCaulescence());
					System.exit(0);
				}
				mainBranchNodes = getMainPathNodes(treeCopy);
				mergeTerminalParentNodes(treeCopy, mainBranchNodes, onOffBits, metric);
				//System.out.println(treeCopy.getAsymmetry().getCaulescence());
				if (treeCopy.getAsymmetry().getCaulescence() > bestCaul){
					bestTree = treeCopy;
					bestCaul = treeCopy.getAsymmetry().getCaulescence();
				}
			}
			
			onOffBits = onOffBits.subtract(BigInteger.ONE);
		}
		return bestTree;
	}
	
	public static ParentedBinaryTreeNode findMainPathBifurcationsSelective(
			ParentedBinaryTreeNode tree, int maxTNodes) throws Exception{
		return findMainPathBifurcationsSelective(tree, maxTNodes, Asymmetry.METRIC_NODES);
	}
	public static ParentedBinaryTreeNode findMainPathBifurcationsSelective(
			ParentedBinaryTreeNode tree, int maxTNodes, int metric) throws Exception{
		// Get nodes already in the main branch (can't toggle off)
		Set mainBranchNodes = getMainPathNodes(tree);
		// Get terminal nodes that need to be toggled on/off as part of the main branch
		List onOffList = pickTerminalNodesWorthJoining(tree, mainBranchNodes, metric);
		System.out.println("Selected T-Nodes: "+onOffList.size());
		if (onOffList.size() > maxTNodes){
			return null;
		}
		
		return trySelectedPaths(tree, onOffList, mainBranchNodes, metric);
	}
	
	public static ParentedBinaryTreeNode findMainPathBifurcationsExhaustive(
			ParentedBinaryTreeNode tree) throws Exception{
		return findMainPathBifurcationsExhaustive(tree, Asymmetry.METRIC_NODES);
	}
	public static ParentedBinaryTreeNode findMainPathBifurcationsExhaustive(
			ParentedBinaryTreeNode tree, int metric) throws Exception{
		tree.calculateAsymmetry(metric);
		// Get nodes already in the main branch (can't toggle off)
		Set mainBranchNodes = getMainPathNodes(tree);
		// Get terminal nodes that need to be toggled on/off as part of the main branch
		List onOffList = getTerminalParentNodes(tree, mainBranchNodes);
		return trySelectedPaths(tree, onOffList, mainBranchNodes, metric);
	}
/*
	private static void resetCaulescence(ParentedBinaryTreeNode tree){
		
	}
*/	
	private static void mergeTerminalParentNodes(
			ParentedBinaryTreeNode tree, Set mainBranchNodes, BigInteger onOffBits, int metric)
	throws Exception{
		LinkedStack stack = new LinkedStack();
		stack.push(tree);
		ParentedBinaryTreeNode node;
		int i = 0;
		while (!stack.isEmpty()){
			node = (ParentedBinaryTreeNode)stack.pop();
			if (node.hasChildren()){
				if (!mainBranchNodes.contains(node)){
					// Add node to list If terminal node
					if ((node.getLeft().isLeaf() && node.getRight().isLeaf())
						// Or if larger child (main branch) is a leaf
							|| node.getLargerSubtree().isLeaf()){
						if (isNodeOn(i,onOffBits)){
							mergeUp(node, metric);
						}
						i++;
					}
				}
				// Adding in same order as sister function
				if (!node.getLeft().isLeaf()){
					stack.push(node.getLeft());
				}
				if (!node.getRight().isLeaf()){
					stack.push(node.getRight());
				}
			}
		}
	}
	
	private static boolean isNodeOn(int i, BigInteger onOffBits){
		return onOffBits.shiftRight(i).and(BigInteger.ONE).compareTo(BigInteger.ZERO) > 0;
	}

	private static List pickTerminalNodesWorthJoining(
			ParentedBinaryTreeNode tree, Set mainBranchNodes, int metric) throws Exception{
		double caulCheck = tree.getAsymmetry().getCaulescence();
		
		LinkedStack stack = new LinkedStack();
		List list = new ArrayList();
		stack.push(tree);
		ParentedBinaryTreeNode node, requiredNode, checkNode;
		ExhaustiveBean bean;
		//System.out.println("TreeHead: "+tree);
		Map map = new HashMap();
		while (!stack.isEmpty()){
			node = (ParentedBinaryTreeNode)stack.pop();
			if (node.hasChildren()){
				if (!mainBranchNodes.contains(node)){
					// Add node to list If terminal node
					if ((node.getLeft().isLeaf() && node.getRight().isLeaf())
						// Or if larger child (main branch) is a leaf
							|| node.getLargerSubtree().isLeaf()){
						// Find where path from node to root would merge with another main stem
						checkNode = node;
						while (checkNode.getParent() != null
								&& isMainPathNode(checkNode)){
							checkNode = checkNode.getParent();
						}
						// Only add if where it would merge it actually would increase caulescence
						// OR if the caulescence is higher than the total tree caulescence
						// OR if the parent partition caulescence is less than the total tree caulescence
						if (checkNode.getAsymmetry().getCaulescence() 
								> checkNode.getParent().getAsymmetry().getPartitionAsymmetry()
								|| checkNode.getAsymmetry().getCaulescence() > caulCheck
								|| checkNode.getParent().getAsymmetry().getPartitionAsymmetry() < caulCheck){
							bean = new ExhaustiveBean(node);
							bean.setIndex(list.size());
							map.put(node, new Integer(list.size()));
							//System.out.println(node + ";; pos: "+list.size());
							list.add(bean);
						}
					}
				}
				if (!node.getLeft().isLeaf()){
					stack.push(node.getLeft());
				}
				if (!node.getRight().isLeaf()){
					stack.push(node.getRight());
				}
			}
		}
		for (int i = 0; i < list.size(); i++){
			bean = (ExhaustiveBean)list.get(i);
			//System.out.println("checking: "+bean.getNode());
			requiredNode = findDependentNode(bean.getNode());
			//System.out.println("required: "+requiredNode+"; mainBranch? "+mainBranchNodes.contains(requiredNode));
			if (requiredNode != null && !mainBranchNodes.contains(requiredNode)
					&& map.containsKey(requiredNode)){
				bean.setRequiredOnIndex(((Integer)map.get(requiredNode)).intValue());
			}
		}
		return list;
	}

	
	// Looking for the parents of main branch terminations
	private static List getTerminalParentNodes(ParentedBinaryTreeNode tree, Set mainBranchNodes)
	throws Exception{
		LinkedStack stack = new LinkedStack();
		List list = new ArrayList();
		stack.push(tree);
		ParentedBinaryTreeNode node, requiredNode;
		ExhaustiveBean bean;
		//System.out.println("TreeHead: "+tree);
		Map map = new HashMap();
		while (!stack.isEmpty()){
			node = (ParentedBinaryTreeNode)stack.pop();
			if (node.hasChildren()){
				if (!mainBranchNodes.contains(node)){
					// Add node to list If terminal node
					if ((node.getLeft().isLeaf() && node.getRight().isLeaf())
						// Or if larger child (main branch) is a leaf
							|| node.getLargerSubtree().isLeaf()){
						bean = new ExhaustiveBean(node);
						bean.setIndex(list.size());
						map.put(node, new Integer(list.size()));
						//System.out.println(node + ";; pos: "+list.size());
						list.add(bean);
					}
				}
				if (!node.getLeft().isLeaf()){
					stack.push(node.getLeft());
				}
				if (!node.getRight().isLeaf()){
					stack.push(node.getRight());
				}
			}
		}
		for (int i = 0; i < list.size(); i++){
			bean = (ExhaustiveBean)list.get(i);
			//System.out.println("checking: "+bean.getNode());
			requiredNode = findDependentNode(bean.getNode());
			//System.out.println("required: "+requiredNode+"; mainBranch? "+mainBranchNodes.contains(requiredNode));
			if (requiredNode != null && !mainBranchNodes.contains(requiredNode)){
				bean.setRequiredOnIndex(((Integer)map.get(requiredNode)).intValue());
			}
		}
		return list;
	}

	private static ParentedBinaryTreeNode findDependentNode(ParentedBinaryTreeNode node) throws Exception{
		while (node.getParent() != null
				&& isMainPathNode(node)){
			node = node.getParent();
		}
		// If this is the top node, we were looking at a main branch node already
		if (node.getParent() == null){
			return null;
		}
		node = node.getParent();
		// In this case, the required node is the main branch
		if (node.getParent() == null){
			return null;
		}
		while (node.hasChildren()){
			node = (ParentedBinaryTreeNode)node.getLargerSubtree();			
		}
		return node.getParent();
	}
	
	private static boolean isMainPathNode(ParentedBinaryTreeNode node) throws Exception{
		return (node.isLeft() && node.getParent().getAsymmetry().isLargerSubtreeLeft())
			|| (node.isRight() && node.getParent().getAsymmetry().isLargerSubtreeRight());
	}
	
	private static class ExhaustiveBean{
		ParentedBinaryTreeNode node;
		int requiredOnIndex;
		int index;
		public ExhaustiveBean(ParentedBinaryTreeNode node){
			this.node = node;
			this.requiredOnIndex = -1;
		}
		public int getIndex() {
			return index;
		}
		public void setIndex(int index) {
			this.index = index;
		}
		public ParentedBinaryTreeNode getNode() {
			return node;
		}
		public void setNode(ParentedBinaryTreeNode node) {
			this.node = node;
		}
		public int getRequiredOnIndex() {
			return requiredOnIndex;
		}
		public void setRequiredOnIndex(int requiredOnIndex) {
			this.requiredOnIndex = requiredOnIndex;
		}
		
	}

	private static Set getMainPathNodes(BinaryTreeNode tree) throws Exception{
		Set set = new LinkedHashSet();
		BinaryTreeNode node = tree;
		while (node != null && node.hasChildren()){
			set.add(node);
			if (node.getAsymmetry().isMainPathBoth()){
				set.add(getMainPathNodes(node.getLargerSubtree()));
				set.add(getMainPathNodes(node.getSmallerSubtree()));
				node = null;
			}
			else {
				node = node.getLargerSubtree();
			}
		}
		return set;
	}
	
	// Creates a copy of the tree from the given node up to the root, keeping caulescence
	// and meta data, but not any of the additional structure
	private static Asymmetry testMergeUp(ParentedBinaryTreeNode targetNode, int metric)
	throws Exception{
		ParentedBinaryTreeNode copyNode, copyParent, node;
		ParentedBinaryTreeNode initCopyNode = targetNode.copyParented();
		copyNode = initCopyNode;
		node = targetNode;
		Asymmetry parentCaul = null, nodeCaul;
		double allSummedSizeMod = 0, longerSummedSizeMod = 0;
		// create copy path; keeping track of changes to allSummedSize & longerSummedSize
		//System.out.println("TestMergeUp: "+targetNode);
		while (node.getParent() != null) {
			//System.out.println("NodeY: "+((SwcDataNode)node.getData()).getY()+"; numBranchesNow: "+node.getAsymmetry().getNumMainPathes());
			copyParent = node.getParent().copyParented();
			parentCaul = copyParent.getAsymmetry(metric);
			nodeCaul = copyNode.getAsymmetry(metric);
			// Get left/right correct for caulescence; if merging, update mod values
			if (node.isLeft()){
				copyParent.setLeft(copyNode);
				if (parentCaul.isMainPathRight()){
					allSummedSizeMod = nodeCaul.getAllSummedSize()
						- (parentCaul.getLargerSize() + parentCaul.getSmallerSize());
					longerSummedSizeMod = nodeCaul.getLargerSummedSize() - parentCaul.getLargerSize();
					parentCaul.setMainPath(Asymmetry.MAIN_BRANCH_BOTH);
				}
			}
			else{
				copyParent.setRight(copyNode);
				if (parentCaul.isMainPathLeft()){
					allSummedSizeMod = nodeCaul.getAllSummedSize()
						- (parentCaul.getLargerSize() + parentCaul.getSmallerSize());
					longerSummedSizeMod = nodeCaul.getLargerSummedSize() - parentCaul.getLargerSize();
					parentCaul.setMainPath(Asymmetry.MAIN_BRANCH_BOTH);
				}
			}
			// modify parent caulescence by running modification value
			parentCaul.setAllSummedSize(parentCaul.getAllSummedSize()+allSummedSizeMod);
			parentCaul.setLargerSummedSize(parentCaul.getLargerSummedSize()+longerSummedSizeMod);
			copyNode = copyNode.getParent();
			node = node.getParent();
		}
		
		return parentCaul;
	}

	// returns nodes that have been merged up
	private static Set mergeUp(ParentedBinaryTreeNode targetNode, int metric)
	throws Exception{
		Set mergedUpSet = new HashSet();
		ParentedBinaryTreeNode node, parent;
		node = targetNode;
		Asymmetry parentCaul = null, nodeCaul;
		double allSummedSizeMod = 0, longerSummedSizeMod = 0;
		int numBranchesMod = 0;
		// create copy path first; keep track of changes to allSummedSize & longerSummedSize
		while (node.getParent() != null) {
			//System.out.println("NodeY: "+((SwcDataNode)node.getData()).getY()+"; numBranchesNow: "+node.getAsymmetry().getNumMainPathes());
			mergedUpSet.add(node);
			parent = node.getParent();
			parentCaul = parent.getAsymmetry(metric);
			nodeCaul = node.getAsymmetry(metric);
			// Get left/right correct for caulescence; if merging, update mod values
			if ((node.isLeft() && parentCaul.isMainPathRight())
					|| (node.isRight() && parentCaul.isMainPathLeft())){
				allSummedSizeMod = nodeCaul.getAllSummedSize()
					- (parentCaul.getLargerSize() + parentCaul.getSmallerSize());
				longerSummedSizeMod = nodeCaul.getLargerSummedSize() - parentCaul.getLargerSize();
				numBranchesMod = nodeCaul.getNumMainPathes();
				parentCaul.setMainPath(Asymmetry.MAIN_BRANCH_BOTH);
			}
			// modify parent caulescence by running modification value
			parentCaul.setAllSummedSize(parentCaul.getAllSummedSize()+allSummedSizeMod);
			parentCaul.setLargerSummedSize(parentCaul.getLargerSummedSize()+longerSummedSizeMod);
			parentCaul.setNumMainPathes(parentCaul.getNumMainPathes()+numBranchesMod);
			node = node.getParent();
		}
		return mergedUpSet;
	}

	// Length caulescence only works if data is of type SwcDataNode
	public static double getMultibranchCaulescence(ParentedBinaryTreeNode tree, int metric)
	throws Exception{
		return getMultibranchCaulescence(tree,metric,Asymmetry.DIFF_MODE);
	}
	public static double getMultibranchCaulescence(
			ParentedBinaryTreeNode tree, int metric, int caulescenceMode)
	throws Exception{
		if (metric != Asymmetry.METRIC_NODES
				&& !tree.getData().getClass().equals(SwcDataNode.class)){
			throw new Exception("Data member in ParentedBinaryTreeNode must be of type "+SwcDataNode.class+
					". Data member of given in put is type "+tree.getData().getClass());
		}
		if (tree.getTreeSize() == 1) return 0;
		int mode = caulescenceMode;
		LinkedStack stack1 = new LinkedStack(), stack2 = new LinkedStack();
		ParentedBinaryTreeNode node, left, right;
		SwcDataNode parentData = null, leftData = null, rightData = null;
		double leftSize, rightSize, longerSummed, leftLargerSummed, rightLargerSummed, 
			allSummed, leftAllSummed, rightAllSummed;
		stack1.push(tree);
		// Generate stack of all bifurcating nodes
		while (!stack1.isEmpty()){
			node = (ParentedBinaryTreeNode)(stack1.pop());
			if (node.hasChildren()){
				stack1.push(node.getLeft());
				stack1.push(node.getRight());
				stack2.push(node);
			}
		}
		// Stack will pop off from leaf node parents up to the root
		while (!stack2.isEmpty()){
			node = (ParentedBinaryTreeNode)(stack2.pop());
			left = node.getParentedLeft();
			right = node.getParentedRight();
			parentData = (SwcDataNode)node.getData();
			//System.out.println("NodeId: "+parentData.getNodeId());
			// If using euclidean branch length, get those lengths
			switch (metric){
			case Asymmetry.METRIC_NODES:
				leftSize = rightSize = 1;
				break;
			case Asymmetry.METRIC_EUCLIDEAN_DISTANCE:
				leftData = (SwcDataNode)left.getData();
				rightData = (SwcDataNode)right.getData();
				leftSize = SwcDataUtils.getDistance(parentData, leftData);
				rightSize = SwcDataUtils.getDistance(parentData, rightData);
				break;
			// If using branch order distance, "length" is one
			case Asymmetry.METRIC_PATH_DISTANCE:
				parentData = (SwcDataNode)node.getData();
				leftData = (SwcDataNode)left.getData();
				rightData = (SwcDataNode)right.getData();
				leftSize = leftData.getSecondaryData().getPathLength();
				rightSize = rightData.getSecondaryData().getPathLength();
				break;
			default:
				throw new Exception("Unknown distance measure mode "+metric);
			}
			
			longerSummed = rightLargerSummed = leftLargerSummed = 0;
			allSummed = rightAllSummed = leftAllSummed = 0;

			// Add to total length of each sub-tree;
			if (left.hasChildren()){
				// total length will be the longer + shorter lengths of each of left's sub-tree
				leftSize += 
					left.getAsymmetry().getLargerSize()
					+ left.getAsymmetry().getSmallerSize();
				// keeping track of the sum of larger tree lengths for running caulescence calc.
				leftLargerSummed = left.getAsymmetry().getLargerSummedSize();
				// keeping track of the sum of all tree lengths for running caulescence calc.
				leftAllSummed = left.getAsymmetry().getAllSummedSize();
			}
			if (right.hasChildren()){
				rightSize += 
					right.getAsymmetry().getLargerSize()
					+ right.getAsymmetry().getSmallerSize();
				rightLargerSummed = right.getAsymmetry().getLargerSummedSize();
				rightAllSummed = right.getAsymmetry().getAllSummedSize();
			}
			
			Asymmetry nodeCaul = new Asymmetry();
			Asymmetry rightCaul = right.getAsymmetry(metric), leftCaul = left.getAsymmetry(metric);
			node.setAsymmetry(metric, nodeCaul);
			// Add current node's total length to the summation
			allSummed += leftSize + rightSize;
			// determine larger/longer sub-tree
			if (leftSize > rightSize){
				nodeCaul.setLargerSize(leftSize);
				nodeCaul.setSmallerSize(rightSize);
				longerSummed = leftSize + leftLargerSummed;
				allSummed += leftAllSummed;
				nodeCaul.setMainPath(Asymmetry.MAIN_BRANCH_LEFT);
				nodeCaul.setLargerSubtree(Asymmetry.MAIN_BRANCH_LEFT);
			}
			else{
				nodeCaul.setLargerSize(rightSize);
				nodeCaul.setSmallerSize(leftSize);
				longerSummed = rightSize + rightLargerSummed;
				allSummed += rightAllSummed;
				nodeCaul.setMainPath(Asymmetry.MAIN_BRANCH_RIGHT);
				nodeCaul.setLargerSubtree(Asymmetry.MAIN_BRANCH_RIGHT);
			}
			nodeCaul.setLargerSummedSize(longerSummed);
			nodeCaul.setAllSummedSize(allSummed);
			
			/** Determining if there is a main branch split 
			* If both sub-trees have children & both sub-trees have higher caulescence values
			*   Recalculate caulescence values:
			*   	Remove current node values, add both sub-tree sums
			**/
			if (left.hasChildren() && right.hasChildren()
				&& (nodeCaul.getCaulescence(mode) < leftCaul.getCaulescence(mode)
				|| nodeCaul.getCaulescence(mode) < rightCaul.getCaulescence(mode))){
				Asymmetry tmp = nodeCaul.copy();
				
				tmp.setLargerSummedSize(leftCaul.getLargerSummedSize()
						+ rightCaul.getLargerSummedSize());
				tmp.setAllSummedSize(
						leftCaul.getAllSummedSize()
						+ rightCaul.getAllSummedSize());
				if (tmp.getCaulescence() > nodeCaul.getCaulescence()){
					nodeCaul = tmp;
					nodeCaul.setNumMainPathes(
							leftCaul.getNumMainPathes()
							+ rightCaul.getNumMainPathes());
					nodeCaul.setMainPath(Asymmetry.MAIN_BRANCH_BOTH);
				}
			}
			// Keep track of number of main branches, taking from the longer sub-tree
			else if (left.hasChildren() || right.hasChildren()){
				if (leftSize > rightSize){
					nodeCaul.setNumMainPathes(
							left.hasChildren() ? 
									leftCaul.getNumMainPathes()
									: 1);
				}
				else{
					nodeCaul.setNumMainPathes(
							right.hasChildren() ? 
									rightCaul.getNumMainPathes()
									: 1);
				}
			}
		}
		return tree.getAsymmetry().getCaulescence(mode);
	}
	
	// Based on whatever measure mode caulescence was calculated with
	public static double getMainPathSize(ParentedBinaryTreeNode tree, int metric)
	throws Exception{
		return getMainPathSize(tree,metric,Asymmetry.SIZE_MODE_SUM);
	}
	public static double getMainPathSize(ParentedBinaryTreeNode tree, int metric, int sizeMode)
	throws Exception{
		tree.calculateAsymmetry(metric, sizeMode);
		return getMainPathSize(null, tree, metric, sizeMode);
	}
	public static double getMainPathSize(BinaryTreeNode tree)throws Exception{
		return getMainPathSize(null, tree, Asymmetry.METRIC_NODES, Asymmetry.SIZE_MODE_SUM);
	}
	public static double getMainPathSize(BinaryTreeNode parent, BinaryTreeNode node)
	throws Exception{
		return getMainPathSize(parent,node, Asymmetry.METRIC_NODES, Asymmetry.SIZE_MODE_SUM);
	}
	public static double getMainPathSize(BinaryTreeNode parent, BinaryTreeNode node, int metric, int sizeMode)
	throws Exception{
		double size = 0;
		Asymmetry nodeC = node.getAsymmetry(metric);
		if (parent != null){
			Asymmetry parentC = parent.getAsymmetry(metric);
			size = parentC.getLargerSize() - nodeC.getTotalSize();
		}
		BinaryTreeNode left = node.getLeft(), right = node.getRight();
		if (nodeC.isMainPathBoth() || nodeC.isMainPathLeft()){
			if (left.hasChildren()){
				size += getMainPathSize(node, left, metric, sizeMode);
			}
			else{
				size += nodeC.isLargerSubtreeLeft() 
					? node.getAsymmetry().getLargerSize() 
					: node.getAsymmetry().getSmallerSize();
			}
		}
		if (nodeC.isMainPathBoth() || nodeC.isMainPathRight()){
			if (right.hasChildren()){
				size += getMainPathSize(node, right, metric, sizeMode);
			}
			else{
				size += nodeC.isLargerSubtreeRight() 
					? node.getAsymmetry().getLargerSize() 
					: node.getAsymmetry().getSmallerSize();
			}
		}
		return size;
	}
	
	private static Map numShapesMap = new HashMap();
	public static BigInteger numShapes(long degree){
		return numShapes(degree, numShapesMap);
	}
	public static BigInteger numShapes(long degree, Map numShapesMap){
		Long key = new Long(degree);
		if (numShapesMap.containsKey(key)){
			return (BigInteger)numShapesMap.get(key);
		}
		if (degree == 1 || degree == 2){
			return BigInteger.ONE;
		}
		BigInteger numShapes = BigInteger.ZERO, two = BigInteger.ONE.add(BigInteger.ONE);
		BigInteger tmp, tmp1, tmp2;
		long m;
        if (degree%2 == 0){
            // Half Sum when degree is even: numShapes + (1/2) * numShapes(degree/2) * (numShapes(degree/2)+1);
        	m = degree / 2;
            tmp = numShapes(m,numShapesMap);
            numShapes = numShapes.add(
            		tmp.multiply(tmp.add(BigInteger.ONE))
            			.divide(two)
            	);
        }
        else{
            m = (degree+1)/2;
        }

        for (int i = 1; i < m; i++){
        	// numShapes = SUM(numShapes(i) * numShapes(degree-i))
        	tmp1 = numShapes(i,numShapesMap);
            tmp2 = numShapes(degree-i,numShapesMap);
            if (tmp1.compareTo(tmp2) == 0){
            	numShapes = numShapes.add(
            		tmp1.multiply(tmp1.add(BigInteger.ONE)).divide(two));
            }
            else{
            	numShapes = numShapes.add(tmp1.multiply(tmp2));
            }
        }
        numShapesMap.put(key, numShapes);
		return numShapes;
	}
	
	public static double getCNodeFraction(BinaryTreeNode tree){
		int size = 0;
		int cCount = 0;
		LinkedQueue queue = new LinkedQueue();
		queue.enqueue(tree);
		BinaryTreeNode node;
		while (!queue.isEmpty()){
			node = (BinaryTreeNode)queue.dequeue();
			size++;
			if ((node.getLeft().hasChildren() && !node.getRight().hasChildren())
					|| (!node.getLeft().hasChildren() && node.getRight().hasChildren())){
				cCount++;
			}
			if (node.getLeft().hasChildren()){
				queue.enqueue(node.getLeft());
			}
			if (node.getRight().hasChildren()){
				queue.enqueue(node.getRight());
			}
		}
		
		return (double)cCount/size;
	}
	
	public static LinkedStack getLeavesToRootStack(BinaryTreeNode tree){
		return getLeavesToRootStack(tree, false);
	}
	public static LinkedStack getLeavesToRootStack(BinaryTreeNode tree, boolean mustHaveChildren){
		LinkedStack initStack = new LinkedStack(), stack = new LinkedStack();
		BinaryTreeNode node;
		initStack.push(tree);
		while (!initStack.isEmpty()){
			node = (BinaryTreeNode)initStack.pop();
			if (!mustHaveChildren || node.hasChildren()){
				stack.push(node);
			}
			if (node.hasChildren()){
				initStack.push(node.getLeft());
				initStack.push(node.getRight());
			}
		}
		return stack;
	}

	public static Set getAllNodesSet(BinaryTreeNode tree){
		LinkedStack initStack = new LinkedStack();
		Set allNodes = new HashSet();
		BinaryTreeNode node;
		initStack.push(tree);
		while (!initStack.isEmpty()){
			node = (BinaryTreeNode)initStack.pop();
			allNodes.add(node);
			if (node.hasChildren()){
				initStack.push(node.getLeft());
				initStack.push(node.getRight());
			}
		}
		return allNodes;
	}

	public static ParentedBinaryTreeNode copyParentedTree(ParentedBinaryTreeNode tree){
		return copyParentedTree(tree,false);
	}
	public static ParentedBinaryTreeNode copyParentedTree(
			ParentedBinaryTreeNode tree, boolean keepData){
		ParentedBinaryTreeNode treeCopy = tree.copyParented(keepData), node, copyNode;
		LinkedQueue queue = new LinkedQueue(), copyQueue = new LinkedQueue();
		queue.enqueue(tree);
		copyQueue.enqueue(treeCopy);
		while (!queue.isEmpty()){
			node = (ParentedBinaryTreeNode)queue.dequeue();
			copyNode = (ParentedBinaryTreeNode)copyQueue.dequeue();
			if (node.hasChildren()){
				copyNode.setRight(node.getParentedRight().copyParented(), false);
				copyNode.getRight().setData(node.getRight().getData());
				copyNode.setLeft(node.getParentedLeft().copyParented(), false);
				copyNode.getLeft().setData(node.getLeft().getData());
				queue.enqueue(node.getLeft());
				queue.enqueue(node.getRight());
				copyQueue.enqueue(copyNode.getLeft());
				copyQueue.enqueue(copyNode.getRight());
			}
		}
		BinaryTreeNode.updateStatisticsDown(treeCopy);
		return treeCopy;
	}
	
	public static BinaryTreeNode copyTree(BinaryTreeNode tree){
		BinaryTreeNode treeCopy = new BinaryTreeNode(), node, copyNode;
		LinkedQueue queue = new LinkedQueue(), copyQueue = new LinkedQueue();
		queue.enqueue(tree);
		copyQueue.enqueue(tree.copy());
		while (!queue.isEmpty()){
			node = (BinaryTreeNode)queue.dequeue();
			copyNode = (BinaryTreeNode)copyQueue.dequeue();
			if (node.hasChildren()){
				copyNode.setRight(node.getRight().copy(), false);
				copyNode.getRight().setData(node.getRight().getData());
				copyNode.setLeft(node.getLeft().copy(), false);
				copyNode.getLeft().setData(node.getLeft().getData());
				queue.enqueue(node.getLeft());
				queue.enqueue(node.getRight());
				copyQueue.enqueue(copyNode.getLeft());
				copyQueue.enqueue(copyNode.getRight());
			}
		}
		BinaryTreeNode.updateStatisticsDown(treeCopy);
		return treeCopy;
	}

	// REFERENCE
	public static BigInteger partialSumOfNumShapes(
			long degree, long parts) 
	throws Exception{
		if (parts == 0){
			return BigInteger.ZERO;
		}
		if (parts > (degree/2)){
			throw new Exception("Parts should be no larger than degree/2. "+
					"Degree: "+degree+"; Parts: "+parts);
		}
		BigInteger s = BigInteger.ZERO;
		for (int i = 1; i <= parts; i++){
			// S_degree(parts) = ... + S_degree(i)*S_degree(degree-i) ...
			s = s.add(BinaryTreeUtils.numShapes(i).multiply(BinaryTreeUtils.numShapes(degree-i)));
		}
		return s;
	}

	public static double getExcessAsymmetry(BinaryTreeNode tree, int metric)
	throws Exception{
		LinkedStack stack = new LinkedStack();
		stack.push(tree);
		BinaryTreeNode node;
		double totalAsym = 0;
		int nodes = 0;
		while (!stack.isEmpty()){
			node = (BinaryTreeNode)stack.pop();
			if (node.getRight().hasChildren() && node.getLeft().hasChildren()){
				nodes++;
				totalAsym += getExcessPartitionAsymmetry(node, metric);
			}
			if (node.getLeft().hasChildren()){
				stack.push(node.getLeft());
			}
			if (node.getRight().hasChildren()){
				stack.push(node.getRight());
			}
		}
		if (nodes == 0){
			return -1;
		}
		return totalAsym/nodes;
	}

	public static double getExcessPartitionAsymmetry(BinaryTreeNode node, int metric)
	throws Exception{
		double a = 0, b = 0, c = 0, d = 0;
		double excessPartitionAsymmetry = 0;
		Asymmetry asymLeft = node.getLeft().getAsymmetry(metric);
		Asymmetry asymRight = node.getRight().getAsymmetry(metric);
		if (node.getLeft().hasChildren()){
			a = asymLeft.getLargerSize();
			b = asymLeft.getSmallerSize();
		}
		else{
			a = 0;
			b = 0;
		}
		if (node.getRight().hasChildren()){
			c = asymRight.getLargerSize();
			d = asymRight.getSmallerSize();
		}
		else{
			c = 0;
			d = 0;
		}
		// Ap = |a+b-c-d|/(a+b+c+d); Ap' = |a-b+c-d|/(a+b+c+d);  Ap'' = |a-b-c+d|/(a+b+c+d);
		double denom = a+b+c+d;
		// For nodal all are divided by (a+b+c+d-2)
		if (metric == Asymmetry.METRIC_NODES){
			denom -= 2;
		}
		double Ap = Math.abs(a+b-c-d)/denom;
		double Ap2 = Math.abs(a-b+c-d)/denom;
		double Ap3 = Math.abs(a-b-c+d)/denom;
		// Ep = Ap - <Ap|alpha> = (2Ap - A'p - A''p) / 3
		excessPartitionAsymmetry = 
			(2 * Ap - Ap2 - Ap3) / 3;
		return excessPartitionAsymmetry;
	}

	public static int numTwoDegreeNodes(BinaryTreeNode tree){
		if (tree.getDegree() == 2){
			return 1;
		}
		int count = 0;
		if (tree.getLeft().hasChildren()){
			count += numTwoDegreeNodes(tree.getLeft());
		}
		if (tree.getRight().hasChildren()){
			count += numTwoDegreeNodes(tree.getRight());
		}
		return count;
	}
	
	public static double getPercentInMainStem(BinaryTreeNode tree)throws Exception{
		double totalSize = tree.getAsymmetry().getTotalSize();
		double mainStemSize = getMainPathSize(tree);
		
		return 100*mainStemSize/totalSize;
	}
	
	/**
	 * @param root
	 * @return
	 * 	Set of nodes under and including root
	 */
	public static Set createNodeSet(BinaryTreeNode root){
		LinkedStack stack = new LinkedStack();
		Set set = new HashSet();
		stack.push(root);
		BinaryTreeNode node;
		while (!stack.isEmpty()){
			node = (BinaryTreeNode)stack.pop();
			set.add(node);
			if (node.hasChildren()){
				stack.push(node.getRight());
				stack.push(node.getLeft());
			}
		}
		return set;
	}

	public static List createNodeList(BinaryTreeNode root){
		return createNodeList(root, false);
	}

	public static List createBifurcationList(BinaryTreeNode root){
		return createNodeList(root, true);
	}

	/**
	 * @param root
	 * @param onlyBifurcations
	 * @return
	 * 	List of nodes under and including root
	 */
	public static List createNodeList(BinaryTreeNode root, boolean onlyBifurcations){
		LinkedStack stack = new LinkedStack();
		List list = new ArrayList();
		stack.push(root);
		BinaryTreeNode node;
		while (!stack.isEmpty()){
			node = (BinaryTreeNode)stack.pop();
			if (!onlyBifurcations || node.hasChildren()){
				list.add(node);
			}
			if (node.hasChildren()){
				stack.push(node.getRight());
				stack.push(node.getLeft());
			}
		}
		return list;
	}

}
