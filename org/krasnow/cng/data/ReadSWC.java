package org.krasnow.cng.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import org.krasnow.cng.domain.EuclideanPoint;
import org.krasnow.cng.domain.LinkedStack;
import org.krasnow.cng.domain.NodePair;
import org.krasnow.cng.domain.ParentedBinaryTreeNode;
import org.krasnow.cng.domain.SwcDataNode;
import org.krasnow.cng.domain.SwcSecondaryData;
import org.krasnow.cng.domain.SwcTreeNode;
import org.krasnow.cng.utils.SwcDataUtils;

/**
 * 
 * @author gillette
 * Converts .swc files info .fasta files and appends them all together into one database.
 * Conversion based on method of division (full cell, arborizations, trees), primary 
 * method of traversal (height, size), and order of traversal (small then large, large
 * then small).  
 * 
 */
public class ReadSWC {
	
	private static boolean debug = false;
	public static double TRAJECTORY_NONE = Math.PI;
	
	/**
	 * 
	 * @param inputFile
	 * @param classification
	 * @param minSize
	 * @param calcPathDist
	 * @return
	 * SwcTreeNode with child list of ParentedBinaryTreeNode. 
	 * Assumes any number of binary trees branching from a single initial point, 
	 * regardless of type.
	 * @throws Exception
	 */
	public static SwcTreeNode convertSwcToBinaryTreeList(
			File inputFile) throws Exception{
		return convertSwcToBinaryTreeList(inputFile, true, 1);
	}
	public static SwcTreeNode convertSwcToBinaryTreeList(File inputFile, boolean zInPathDist) throws Exception{
		return convertSwcToBinaryTreeList(inputFile, zInPathDist, 1, null);
	}
	public static SwcTreeNode convertSwcToBinaryTreeList(File inputFile, double scaleZ) throws Exception{
		return convertSwcToBinaryTreeList(inputFile, true, scaleZ, null);
	}
	
	public static SwcTreeNode convertSwcToBinaryTreeList(
			File inputFile, boolean zInPathDist, double scaleZ) throws Exception{
		return convertSwcToBinaryTreeList(inputFile, zInPathDist, scaleZ, null);
	}
	public static SwcTreeNode convertSwcToBinaryTreeList(
			File inputFile, boolean zInPathDist, double scaleZ, double xyTrajectoryThreshold, double zTrajectoryThreshold) throws Exception{
		SwcDataNode tmp = new SwcDataNode();
		tmp.setX(xyTrajectoryThreshold);
		tmp.setZ(zTrajectoryThreshold);
		return convertSwcToBinaryTreeList(inputFile, zInPathDist, scaleZ, tmp);
		
	}
	public static SwcTreeNode convertSwcToBinaryTreeList(
			File inputFile, boolean zInPathDist, double scaleZ, SwcDataNode trajectoryThresholds) throws Exception{
	    
		List binaryTrees = new ArrayList();
		
		// 1. Builds individual trees
		// 2. Removes continuations.
		// 3. Transforms each into binary tree since all tree info is now known (provides stability in transformation).

		BufferedReader reader = new BufferedReader(new FileReader(inputFile));

		String dataString = "";
		Map nodeMap = new HashMap();
		Map addNodeSideMap = new HashMap();
		SwcTreeNode node, parent, root = null;
		Pattern p = Pattern.compile("[ \\t]*(\\d+)[ \\t]+(\\d+)[ \\t]+(\\-?[\\.\\d]+)[ \\t]+(\\-?[\\.\\d]+)[ \\t]+(\\-?[\\.\\d]+)[ \\t]+([\\.\\d]+)[ \\t]+(\\-?\\d+)[ \\t]*");
		Matcher m;
		int nodeId, parentId, line = 0;
		
		// 1. Assembles individual trees
		while ( (dataString=reader.readLine()) != null ) {
			line++;
	        m = p.matcher(dataString);
	        //System.out.println(dataString);
	        
	        if (m.matches()){
	        	node = new SwcTreeNode();
	        	nodeId = Integer.parseInt(m.group(1));
	        	node.setNodeId(nodeId);
	        	//type = Integer.parseInt(m.group(2));
	        	//node.setType(type);
	        	// Node types will all be set to 1
	        	node.setType(1);
	        	node.setX(Double.parseDouble(m.group(3)));
	        	node.setY(Double.parseDouble(m.group(4)));
	        	if (scaleZ == 1){
	        		node.setZ(Double.parseDouble(m.group(5)));
	        	}
	        	else{
	        		node.setZ(scaleZ * Double.parseDouble(m.group(5)));
	        	}
	        	node.setRadius(Double.parseDouble(m.group(6)));
	        	parentId = Integer.parseInt(m.group(7));
	        	node.setParentId(parentId);
	        	//System.out.println(nodeId + " " + type + " " + parentId);
	        	if (parentId == -1){
	        		root = node;
	        	}
	
        		// Get parent from nodeMap
	        	parent = null;
	        	if (nodeMap.containsKey(new Integer(parentId))){
	        		parent = (SwcTreeNode)nodeMap.get(new Integer(parentId));
	        	}

    			addNode(node,parent,nodeMap,addNodeSideMap);
        	}
	        else if (dataString.charAt(0) != '#'){
	        	throw new DataFormatException("Improper SWC format at line "+line+" of "+inputFile.getName());
	        }
	        m = null;
	    }

		if (root == null){
			throw new DataFormatException("No root node found in "+inputFile.getName());
		}
		
		// Assuming only multifurcation is root
		ParentedBinaryTreeNode binaryTree, binaryRoot = new ParentedBinaryTreeNode(root.getSwcData());
		for (int i = 0; i < root.getChildren().size(); i++){
			binaryTree = convertTreeToBinary((SwcTreeNode)root.getChildren().get(i));
			binaryTree.setParent(binaryRoot);
			
			if (trajectoryThresholds != null){
				calculateTrajectories(binaryTree, trajectoryThresholds, zInPathDist);
			}
			//tree = removeContinuations((SwcTreeNode)root.getChildren().get(0), true, zInPathDist);
			binaryTree = removeContinuations(root, binaryTree, true, zInPathDist);
			
			//binaryTrees.add(convertTreeToBinary(tree));
			binaryTrees.add(binaryTree);
		}
		root.setChildren(binaryTrees);
		
		//System.out.println("Done assembling trees from swc");
		
		return root;
	}

	private static void addNode(
			SwcTreeNode node,
			SwcTreeNode parent,
			Map nodeMap,
			Map addNodeSideMap){

		if (node != null){
			//System.out.println("Parent: "+parent);
    		// Put new node in map
    		nodeMap.put(new Integer(node.getNodeId()), node);
    		// Set node in meta data map (used for nodes with > 2 children)
    		addNodeSideMap.put(node, new Integer(0));
    		// If parent is null, then this is just the start of a new tree
    		// If parent is of a different node type, DO NOT ATTACH
    		if (parent != null && 
    				parent.getType() == node.getType()){
    			parent.addChildNode(node);
    		}
    	}
	}

	private static ParentedBinaryTreeNode removeContinuations(SwcTreeNode treeRoot, ParentedBinaryTreeNode binaryRoot, 
			boolean calcPathDist, boolean zInPathDist){
    	LinkedStack stack = new LinkedStack();
    	ParentedBinaryTreeNode node, headNode = binaryRoot, child;
    	SwcDataNode data, childData;

    	if (calcPathDist){
    		data = binaryRoot.getSwcData();
    		if (data.getSecondaryData() == null){
    			data.setSecondaryData(new SwcSecondaryData());
    		}
	    	if (zInPathDist){
	    		data.getSecondaryData().setPathLength(
	    			SwcDataUtils.getDistance(
	    					treeRoot.getSwcData(), data));
	    		data.getSecondaryData().setXYPathLength(SwcDataUtils.getXYDistance(
	    				treeRoot.getSwcData(), data));
	    	}
	    	else{
	    		data.getSecondaryData().setPathLength(
		    			SwcDataUtils.getXYDistance(
		    					treeRoot.getSwcData(), data));
	    		data.getSecondaryData().setXYPathLength(
	    				data.getSecondaryData().getPathLength());
	    	}
	    	data.getSecondaryData().setZPathLength(
	    			Math.abs(treeRoot.getSwcData().getZ() - binaryRoot.getSwcData().getZ()));
    	}

    	stack.push(binaryRoot);
    	while (!stack.isEmpty()){
    		node = (ParentedBinaryTreeNode)stack.pop();
    		data = node.getSwcData();
    		if (debug) System.out.println("RemoveCont Coords: "+data.getX()+","+data.getY());
    		
    		if (!node.isLeaf()){
    			stack.push(node.getLeft());
    			if (calcPathDist) calculatePathData(node.getParentedLeft(), zInPathDist);
    			
				if (node.getRight() == null){
					// Accumulate node data in child since node will be removed
					child = node.getParentedLeft();
					childData = child.getSwcData();
    				if (calcPathDist && data.getSecondaryData() != null){
    					childData.getSecondaryData().addData(data.getSecondaryData());
    				}
    				
    				if (node == headNode){
    	    			//System.out.println("Node has no parent");
    					// make solo child head if parent was head
    					headNode = child;
    				}
    				else{
						// add child to parent, remove self
	    				if (node.isLeft()){
	    					node.getParent().setLeft(child);
	    				}
	    				else{
	    					node.getParent().setRight(child);
	    				}
    				}
				}
				else{
					stack.push(node.getRight());
					if (calcPathDist) calculatePathData(node.getParentedRight(), zInPathDist);
				}
    		}
    	}
    	return headNode;
	}
	
	private static void calculatePathData(ParentedBinaryTreeNode node, boolean zInPathDist){
		if (debug) System.out.println("Child: "+node.getSwcData());
		SwcDataNode data = node.getSwcData(), parentData = node.getParent().getSwcData();
		// Add child node to stack
		if (data.getSecondaryData() == null){
			data.setSecondaryData(new SwcSecondaryData());
		}
		// Calculate path distance and related metrics
		if (data.getSecondaryData().getPathLength() == 0){
	    	if (zInPathDist){
	    		data.getSecondaryData().setPathLength(
						SwcDataUtils.getDistance(parentData, data));
	    		data.getSecondaryData().setXYPathLength(
		    			SwcDataUtils.getXYDistance(parentData, data));
	    	}
	    	else{
	    		data.getSecondaryData().setPathLength(
						SwcDataUtils.getXYDistance(parentData, data));
	    		data.getSecondaryData().setXYPathLength(data.getSecondaryData().getPathLength());
	    	}
	    	data.getSecondaryData().setZPathLength(
	    			Math.abs(parentData.getZ() - data.getZ()));    				    	
			
	    	data.getSecondaryData().setSurfaceArea(
	    			data.getSecondaryData().getPathLength()
					* Math.PI * data.getDiameter());
	    	data.getSecondaryData().setVolume(
	    			data.getSecondaryData().getPathLength()
					* Math.PI * data.getRadiusSquared());
		}
	}
	
	private static SwcTreeNode removeContinuations(SwcTreeNode root, boolean calcPathDist, boolean zInPathDist){
    	LinkedStack stack = new LinkedStack();
    	SwcTreeNode node, headNode = root, child;
    	Collection children;
    	stack.push(root);

    	if (calcPathDist){
	    	root.setSecondaryData(new SwcSecondaryData());
	    	if (zInPathDist){
	    		root.getSecondaryData().setPathLength(
	    			SwcDataUtils.getDistance(
	    					root.getSwcParent().getSwcData(), root.getSwcData()));
		    	root.getSecondaryData().setXYPathLength(SwcDataUtils.getXYDistance(
    					root.getSwcParent().getSwcData(), root.getSwcData()));
	    	}
	    	else{
	    		root.getSecondaryData().setPathLength(
		    			SwcDataUtils.getXYDistance(
		    					root.getSwcParent().getSwcData(), root.getSwcData()));
		    	root.getSecondaryData().setXYPathLength(
		    			root.getSecondaryData().getPathLength());
	    	}
	    	root.getSecondaryData().setZPathLength(
	    			Math.abs(root.getSwcParent().getSwcData().getZ() - root.getSwcData().getZ()));
    	}

    	while (!stack.isEmpty()){
    		node = (SwcTreeNode)stack.pop();
    		if (debug) System.out.println("RemoveCont Coords: "+node.getX()+","+node.getY());
    		if (node.hasChildren()){
    			children = node.getChildren();
    			//System.out.println("Node has children "+children.size());
    			// Go through each child before continuing through stack
    			for (Iterator it = children.iterator(); it.hasNext();){
    				child = (SwcTreeNode)it.next();
    				if (debug) System.out.println("Child: "+child.getSwcData());
    				// Add child node to stack
    				stack.push(child);
    				if (calcPathDist){
    					if (child.getSecondaryData() == null){
    						child.setSecondaryData(new SwcSecondaryData());
    					}
    					// Calculate path distance and related metrics
    					if (child.getSecondaryData().getPathLength() == 0){
    				    	if (zInPathDist){
    				    		child.getSecondaryData().setPathLength(
        								SwcDataUtils.getDistance(node.getSwcData(), child.getSwcData()));
    					    	child.getSecondaryData().setXYPathLength(
    					    			SwcDataUtils.getXYDistance(node.getSwcData(), child.getSwcData()));
    				    	}
    				    	else{
    				    		child.getSecondaryData().setPathLength(
        								SwcDataUtils.getXYDistance(node.getSwcData(), child.getSwcData()));
    				    		child.getSecondaryData().setXYPathLength(child.getSecondaryData().getPathLength());
    				    	}
    				    	child.getSecondaryData().setZPathLength(
    				    			Math.abs(node.getSwcData().getZ() - child.getSwcData().getZ()));    				    	
    						
    						child.getSecondaryData().setSurfaceArea(
        							child.getSecondaryData().getPathLength()
        							* Math.PI * child.getDiameter());
        					child.getSecondaryData().setVolume(
        							child.getSecondaryData().getPathLength()
        							* Math.PI * child.getRadiusSquared());
    					}
    				}
    			}
    			// Accumulate node data in child since node will be removed
    			if (children.size() == 1){
    				child = (SwcTreeNode)children.iterator().next();
    				if (calcPathDist && node.getSecondaryData() != null){
    					child.getSecondaryData().addData(node.getSecondaryData());
    				}
    				
    				//if (!node.hasParent()){
    				if (node == headNode){
    	    			//System.out.println("Node has no parent");
    					// make solo child head if parent was head
    					headNode = child;
    				}

    				//System.out.println("Node has parent");
					// add child to parent, remove self
					node.getParent().addChildNode(child);
					node.detach();
    			}
    		}
    	}
    	return headNode;
	}

	/** 
	 * 
	 * @param root
	 * @param zInPathDist
	 */
	private static void calculateTrajectories(ParentedBinaryTreeNode root, SwcDataNode thresholds, boolean zInPathDist){
    	LinkedStack stack = new LinkedStack();
    	ParentedBinaryTreeNode node, c;
    	SwcDataNode data, prevData, cData;
    	EuclideanPoint point, tmpPoint;
    	double xyDistance, zDistance;
    	boolean doneX, doneZ;

		if (root.getSwcData().getSecondaryData() == null){
			root.getSwcData().setSecondaryData(new SwcSecondaryData());
		}

		// Add first bifurcation
		node = root;
		while (node.getLeft() != null && node.getRight() == null){
			node = node.getParentedLeft();
		}
		stack.push(node);

    	while (!stack.isEmpty()){
    		node = (ParentedBinaryTreeNode)stack.pop();
    		if (debug) System.out.println("Calculating trajectories for Node: "+node);
    		data = node.getSwcData();

    		if (data.getSecondaryData() == null){
    			data.setSecondaryData(new SwcSecondaryData());
    		}
    		
    		// Calculate for parent direction
    		point = new EuclideanPoint();
    		doneX = doneZ = false;
    		c = node;
    		cData = c.getSwcData();
    		while (!doneX || !doneZ) {
        		c = c.getParent();
        		prevData = cData;
        		cData = c.getSwcData();
        		
        		if (!doneX){
            		// Calculate distance from bifurcation node to current continuation node 
            		xyDistance = SwcDataUtils.getXYDistance(data, cData);
		    		// If the continuation node is outside the threshold
		    		if (xyDistance > thresholds.getX()){
		    			tmpPoint = calculateTrajectoryXY(data, prevData, cData, thresholds.getX());
		    			point.setX(tmpPoint.getX());
		    			point.setY(tmpPoint.getY());
						doneX = true;
					}
		    		// Stop if root node is reached
		    		else if (c.getParent() == null){
						point.setX(cData.getX());
						point.setY(cData.getY());
						doneX = true;
					}
        		}
        		if (!doneZ){
            		zDistance = Math.abs(data.getZ() - cData.getZ());
            		if (zDistance > thresholds.getZ()){
            			point.setZ(calculateTrajectoryZ(data, prevData, cData, thresholds.getZ()));
            			doneZ = true;
            		}
            		// Stop if root node is reached
 		    		else if (c.getParent() == null){
 						point.setZ(cData.getZ());
 						doneZ = true;
 		    		}
        		}
    		}
    		data.getSecondaryData().setParentTrajectoryPoint(point);
    		
    		// If bifurcation, calculate trajectories down left and right children
    		if (!node.isLeaf()){
				// calculate for left
	    		data.getSecondaryData().setLeftTrajectoryPoint(
	    				findChildTrajectory(data, node.getParentedLeft(), thresholds));
				
				// calculate for right
	    		data.getSecondaryData().setRightTrajectoryPoint(
	    				findChildTrajectory(data, node.getParentedRight(), thresholds));

				// Add child bifurcations to stack
	    		addChildBifurcationsToStack(stack, node);
			}
    	}
	}
	
	private static void addChildBifurcationsToStack(LinkedStack stack, ParentedBinaryTreeNode node){
		ParentedBinaryTreeNode c = node.getParentedLeft();
		// Traverse continuations until a bifurcation or termination (leaf) is found
		while(c.getRight() == null && c.getLeft() != null) {
			c = c.getParentedLeft();
		} 
		stack.push(c);

		c = node.getParentedRight();
		while(c.getRight() == null && c.getLeft() != null) {
			c = c.getParentedLeft();
		}
		stack.push(c);
	}
	
	private static EuclideanPoint findChildTrajectory(SwcDataNode data, ParentedBinaryTreeNode child, SwcDataNode thresholds){
		EuclideanPoint point = new EuclideanPoint(), tmpPoint;
		SwcDataNode cData, prevData = data;
		// calculate for parent
		double xyDistance, zDistance;
		boolean doneX = false, doneZ = false;

		while (!doneX || !doneZ) {
    		cData = child.getSwcData();
    		
    		if (!doneX){
        		xyDistance = SwcDataUtils.getXYDistance(data, cData);
	    		// If the continuation node is outside the threshold region 
	    		if (xyDistance > thresholds.getX()){
	    			tmpPoint = calculateTrajectoryXY(data, prevData, cData, thresholds.getX());
	    			point.setX(tmpPoint.getX());
	    			point.setY(tmpPoint.getY());
					doneX = true;
				}
	    		else if (child.isLeaf()){
	    			point.setX(cData.getX());
					point.setY(cData.getY());
					doneX = true;
	    		}
	    		else if (child.getRight() != null){
					point.setX(TRAJECTORY_NONE);
					point.setY(TRAJECTORY_NONE);
					doneX = true;
				}
    		}
    		
    		if (!doneZ){
        		zDistance = Math.abs(data.getZ() - cData.getZ());
	    		if (zDistance > thresholds.getZ()){
	    			point.setZ(calculateTrajectoryZ(data, prevData, cData, thresholds.getZ()));
	    			doneZ = true;
	    		}
	    		// If we've reached a termination then that is the trajectory node
	    		else if (child.isLeaf()){
					point.setZ(cData.getZ());
					doneZ = true;
				}
	    		// If we've reached a bifurcation, will rely on correct path's child for trajectory
				else if (child.getRight() != null){
					point.setZ(TRAJECTORY_NONE);
					doneZ = true;
				}
    		}
    		
    		// Traverse down
    		prevData = cData;
    		if (child.hasChildren()){
    			child = child.getParentedLeft();
    		}
		}
		
		return point;
	}
	
	public static EuclideanPoint calculateTrajectoryXY(
			EuclideanPoint origin, EuclideanPoint first, EuclideanPoint second, double threshold){
		EuclideanPoint point = new EuclideanPoint();
		// Initializing to maximum proportion
		double toFirst, toSecond, proportionAlongLine;
		
		toFirst = SwcDataUtils.getXYDistance(origin, first); 
		toSecond = SwcDataUtils.getXYDistance(origin, second);
		
		// Calculate the proportion along the segment to the point that is at the threshold distance from the origin node
		proportionAlongLine = (threshold - toFirst) / (toSecond - toFirst);
		
		// Set the coordinates based on the first point plus the distance to the threshold point
		point.setX(first.getX() + proportionAlongLine*(second.getX()-first.getX()));
		point.setY(first.getY() + proportionAlongLine*(second.getY()-first.getY()));
		return point;
	}
	public static double calculateTrajectoryZ(
			EuclideanPoint origin, EuclideanPoint first, EuclideanPoint second, double threshold){
		
		// Initializing to maximum proportion
		double toFirst, toSecond, proportionAlongLine;
		
		toFirst = Math.abs(origin.getZ() - first.getZ()); 
		toSecond = Math.abs(origin.getZ() - second.getZ());
		
		// Calculate the proportion along the segment to the point that is at the threshold distance from the origin node
		proportionAlongLine = (threshold - toFirst) / (toSecond - toFirst);

		// Set the coordinates based on the first point plus the distance to the threshold point
		return (first.getZ() + proportionAlongLine*(second.getZ()-first.getZ()));
	}
	
	// NOT assuming continuations have been removed
	private static ParentedBinaryTreeNode convertTreeToBinary(SwcTreeNode head) throws Exception{
		if (head == null){
			return null;
		}
		ParentedBinaryTreeNode root = new ParentedBinaryTreeNode(), node, tmp;
		SwcTreeNode treeNode;
		root.setData(head);
		LinkedStack stack = new LinkedStack(), recentTreeStack = new LinkedStack();
		stack.push(root);
		if (debug) System.out.println("Converting Tree to Binary");
		while (!stack.isEmpty()){
			node = (ParentedBinaryTreeNode)stack.pop();
			treeNode = (SwcTreeNode)node.getData();
			if (debug) System.out.println("Node: "+treeNode.getData());
			// Copy tree node data to binary tree node data
			node.setData(treeNode.getSwcData());
			if (debug) System.out.println("Node data: "+node.getData());
			if (treeNode.hasChildren()){
				if (treeNode.getChildren().size() > 1){
					tmp = convertTreeNodeToBinaryTreeNode(
							(SwcDataNode)node.getData(),treeNode.getChildren());
					node.setLeft(tmp.getParentedLeft());
					node.setRight(tmp.getParentedRight());
					// Find new bottom nodes
					recentTreeStack.push(tmp.getLeft());
					recentTreeStack.push(tmp.getRight());
					while (!recentTreeStack.isEmpty()){
						node = (ParentedBinaryTreeNode)recentTreeStack.pop();
						if (node.isLeaf()){
							stack.push(node);
						}
						else{
							recentTreeStack.push(node.getLeft());
							recentTreeStack.push(node.getRight());
						}
					}
				}
				else{
					node.setLeft(new ParentedBinaryTreeNode());
					node.getLeft().setData((SwcTreeNode)treeNode.getChildren().get(0));
					stack.push(node.getLeft());	
				}
			}
		}
		
		return root;
	}

	
	private static ParentedBinaryTreeNode convertTreeNodeToBinaryTreeNode(
			SwcDataNode data, List childList) {
		ParentedBinaryTreeNode newNode = null;
		SwcDataNode swcData1, swcData2, newSwcDataNode;
		NodePair nodePair, bestNodePair = new NodePair();
		Object object, best1, best2;

		if (childList.size() == 1){
			return new ParentedBinaryTreeNode(childList.get(0));
		}
		
		// Keep going until there is only one node left on top, it will be parent to a binary tree
		while (childList.size() > 1){
			best1 = childList.get(0); best2 = childList.get(1);
			bestNodePair = new NodePair();
			bestNodePair.setDistanceSq(-1);
			// Loop through multiple children, find closest two nodes
			for (int i = 0; i < childList.size()-1; i++){
				// Get data, need to know node type for pulling out of list
				if (childList.get(i).getClass().equals(SwcTreeNode.class)){
					swcData1 = (SwcDataNode)((SwcTreeNode)childList.get(i)).getSwcData();	
				}
				else{
					swcData1 = (SwcDataNode)((ParentedBinaryTreeNode)childList.get(i)).getData();
				}
				// Loop through the rest, comparing current child to all others
				for (int j = i+1; j < childList.size(); j++){
					if (childList.get(j).getClass().equals(SwcTreeNode.class)){
						swcData2 = (SwcDataNode)((SwcTreeNode)childList.get(j)).getSwcData();	
					}
					else{
						swcData2 = (SwcDataNode)((ParentedBinaryTreeNode)childList.get(j)).getData();
					}
					// create a node pair for determining closest two nodes
					nodePair = new NodePair(swcData1,swcData2);
					if (bestNodePair.getDistanceSq() == -1 
							|| nodePair.getDistanceSq() < bestNodePair.getDistanceSq()){
						bestNodePair = nodePair;
						best1 = childList.get(i);
						best2 = childList.get(j);
					}
				}
			}
			
			// Insert new node with as parent of closest nodes
			newNode = new ParentedBinaryTreeNode();
			newSwcDataNode = new SwcDataNode();
			newNode.setData(newSwcDataNode);
			newSwcDataNode.setNodeId(data.getNodeId());
			newSwcDataNode.setX(data.getX());
			newSwcDataNode.setY(data.getY());
			newSwcDataNode.setZ(data.getZ());
			newSwcDataNode.setType(data.getType());
			// Nodes share parent data
			newSwcDataNode.setSecondaryData(data.getSecondaryData());

			// Make closest nodes children of the new node
			object = best1;
			childList.remove(best1);
			if (object.getClass().equals(SwcTreeNode.class)){
				newNode.setLeft(new ParentedBinaryTreeNode());
				newNode.getLeft().setData((SwcTreeNode)object);
			}
			else{
				newNode.setLeft((ParentedBinaryTreeNode)object);
			}
			
			object = best2;
			childList.remove(best2);
			if (object.getClass().equals(SwcTreeNode.class)){
				newNode.setRight(new ParentedBinaryTreeNode());
				newNode.getRight().setData((SwcTreeNode)object);
			}
			else{
				newNode.setRight((ParentedBinaryTreeNode)object);
			}
			childList.add(newNode);
		}
		return newNode;
	}
	
	public static SwcTreeNode convertSwcToSwcTree(
			File inputFile) throws Exception{
		return convertSwcToSwcTree(inputFile, 1);
	}
	public static SwcTreeNode convertSwcToSwcTree(
			File inputFile, double scaleZ) throws Exception{
	    
		// 1. Builds individual trees
		// 2. Removes continuations.
		// 3. Transforms each into binary tree since all tree info is now known (provides stability in transformation).

		BufferedReader reader = new BufferedReader(new FileReader(inputFile));

		String dataString = "";
		Map nodeMap = new HashMap();
		Map addNodeSideMap = new HashMap();
		SwcTreeNode node, parent, root = null;
		Pattern p = Pattern.compile(" *(\\d+) +(\\d+) +(\\-?[\\.\\d]+) +(\\-?[\\.\\d]+) +(\\-?[\\.\\d]+) +([\\.\\d]+) +(\\-?\\d+) *");
		Matcher m;
		int nodeId, parentId, type;
		
		// 1. Assembles individual trees
		while ( (dataString=reader.readLine()) != null ) { 
	        m = p.matcher(dataString);
	        //System.out.println(dataString);
	        
	        if (m.matches()){
	        	node = new SwcTreeNode();
	        	nodeId = Integer.parseInt(m.group(1));
	        	node.setNodeId(nodeId);
	        	type = Integer.parseInt(m.group(2));
	        	node.setType(type);
	        	node.setX(Double.parseDouble(m.group(3)));
	        	node.setY(Double.parseDouble(m.group(4)));
	        	if (scaleZ == 1){
	        		node.setZ(Double.parseDouble(m.group(5)));
	        	}
	        	else{
	        		node.setZ(scaleZ * Double.parseDouble(m.group(5)));
	        	}
	        	node.setRadius(Double.parseDouble(m.group(6)));
	        	parentId = Integer.parseInt(m.group(7));
	        	node.setParentId(parentId);
	        	//System.out.println(nodeId + " " + type + " " + parentId);
	        	if (parentId == -1){
	        		root = node;
	        	}
	
        		// Get parent from nodeMap
	        	parent = null;
	        	if (nodeMap.containsKey(new Integer(parentId))){
	        		//parent = (ParentedBinaryTreeNode)nodeMap.get(new Integer(parentId));
	        		parent = (SwcTreeNode)nodeMap.get(new Integer(parentId));
	        	}

    			addNode(node,parent,nodeMap,addNodeSideMap);

        	}
	        m = null;
	    }

		return root;
	}
	
	public static Map convertSwcToTreeNodeMap(
			File inputFile, double scaleZ) throws Exception{
	    
		// 1. Builds individual trees
		// 2. Removes continuations.
		// 3. Transforms each into binary tree since all tree info is now known (provides stability in transformation).

		BufferedReader reader = new BufferedReader(new FileReader(inputFile));

		String dataString = "";
		Map nodeMap = new HashMap();
		Map addNodeSideMap = new HashMap();
		SwcTreeNode node, parent, root = null;
		Pattern p = Pattern.compile(" *(\\d+) +(\\d+) +(\\-?[\\.\\d]+) +(\\-?[\\.\\d]+) +(\\-?[\\.\\d]+) +([\\.\\d]+) +(\\-?\\d+) *");
		Matcher m;
		int nodeId, parentId;
		
		// 1. Assembles individual trees
		while ( (dataString=reader.readLine()) != null ) { 
	        m = p.matcher(dataString);
	        if (debug) System.out.println(dataString);
	        
	        if (m.matches()){
	        	node = new SwcTreeNode();
	        	nodeId = Integer.parseInt(m.group(1));
	        	node.setNodeId(nodeId);
	        	//type = Integer.parseInt(m.group(2));
	        	//node.setType(type);
	        	// Node types will all be set to 1
	        	node.setType(1);
	        	node.setX(Double.parseDouble(m.group(3)));
	        	node.setY(Double.parseDouble(m.group(4)));
	        	if (scaleZ == 1){
	        		node.setZ(Double.parseDouble(m.group(5)));
	        	}
	        	else{
	        		node.setZ(scaleZ * Double.parseDouble(m.group(5)));
	        	}
	        	node.setRadius(Double.parseDouble(m.group(6)));
	        	parentId = Integer.parseInt(m.group(7));
	        	node.setParentId(parentId);
	        	if (debug) System.out.println(nodeId + " " + parentId);
	        	if (parentId == -1){
	        		root = node;
	        	}
	
        		// Get parent from nodeMap
	        	parent = null;
	        	if (nodeMap.containsKey(new Integer(parentId))){
	        		//parent = (ParentedBinaryTreeNode)nodeMap.get(new Integer(parentId));
	        		parent = (SwcTreeNode)nodeMap.get(new Integer(parentId));
	        	}

    			addNode(node,parent,nodeMap,addNodeSideMap);

        	}
	        m = null;
	    }

		// Find terminations and assemble them into a list
		int i;
		nodeMap.clear();
		LinkedStack stack = new LinkedStack();
		stack.push(root);
		while (!stack.isEmpty()){
			node = (SwcTreeNode)stack.pop();
			if (node.isLeaf() || node.getChildren().size() > 1){
				nodeMap.put(new Integer(node.getSwcData().getNodeId()), node);
			}
			for (i = 0; i < node.getChildren().size(); i++){
				stack.push(node.getChildren().get(i));
			}
		}
		
		if (debug) System.out.println("Done assembling trees from swc");
		
		return nodeMap;
	}
	
}