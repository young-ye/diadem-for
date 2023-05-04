package org.krasnow.cng.domain;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.krasnow.cng.utils.BinaryTreeUtils;
import org.krasnow.cng.utils.SwcDataUtils;

public class BinaryTreeNode {

    public static final String ORDER_STL = "SmallThenLarge";
    public static final String ORDER_LTS = "LargeThenSmall";
    public static final String TRAVERSE_SIZE = "Size";
    public static final String TRAVERSE_MAX_LENGTH = "Height";
    public static final String TRAVERSE_HEIGHT = "Height";
    public static final String TRAVERSE_AVG_LENGTH = "AvgHeight";

    protected Object data;
    private BinaryTreeNode left,right;

    protected int treeSize;
    protected NodeMetaData metaData;
    private Map asymmetryMap = new HashMap();
    
    public BinaryTreeNode(){
        data = null;
        left = right = null;
        treeSize = 1;
    }
    public BinaryTreeNode(Object d){
        data = d;
        left = right = null;
        treeSize = 1;
    }

    public void setLeft(BinaryTreeNode l){
        left = l;
        updateStatistics();
    }
    public void setLeft(BinaryTreeNode l, boolean updateStatistics){
    	left = l;
    	if (updateStatistics) updateStatistics();
    }
    
    public void setRight(BinaryTreeNode r){
    	right = r;
    	updateStatistics();
    }
    public void setRight(BinaryTreeNode r, boolean updateStatistics){
    	right = r;
    	if (updateStatistics) updateStatistics();
    }
    
    public void updateStatistics(){
    	int treeSize, maxHeight, totalHeight, leaves, leftSize, rightSize, asym;
    	treeSize = 1;
		leaves = totalHeight = maxHeight = leftSize = rightSize = asym = 0;
		if (this.getLeft() != null){
			leftSize = this.getLeft().getTreeSize();
			treeSize += this.getLeft().getTreeSize();
			leaves += this.getLeft().getLeaves();
			maxHeight = this.getLeft().getHeight() + 1;
			// totalLength is the total length of the children plus 1 for each child below (treeSize)
			totalHeight += this.getLeft().getTotalHeight()
				+ this.getLeft().getLeaves();
			asym += this.getLeft().getTotalAsymmetry();
		}
		else if (this.getRight() == null){
			leaves++;
		}
		if (this.getRight() != null){
			rightSize = this.getRight().getTreeSize();
			treeSize += this.getRight().getTreeSize();
			leaves += this.getRight().getLeaves();
			if (this.getRight().getHeight() >= maxHeight){
				maxHeight = this.getRight().getHeight() + 1;
			}
			totalHeight += this.getRight().getTotalHeight()
				+ this.getRight().getLeaves();
			asym += this.getRight().getTotalAsymmetry();
		}
		this.setTreeSize(treeSize);
		this.setLeaves(leaves);
		this.setHeight(maxHeight);
		this.setTotalHeight(totalHeight);
		// treeSize-1 so this node is not included
		this.setAverageHeight((float)totalHeight/leaves);
		if (treeSize > 2){
			this.setTotalAsymmetry(asym
				+ (float)(treeSize - 2 * Math.min(leftSize, rightSize)) /
					(treeSize - 2));
		}
		else{
			this.setTotalAsymmetry(0);
		}
    }
    
    public void setData(Object d){
        data = d;
    }
    public BinaryTreeNode getLeft(){
        return left;
    }
    public BinaryTreeNode getRight(){
        return right;
    }
    public SwcDataNode getSwcData(){
    	if (data != null && data.getClass().equals(SwcDataNode.class)){
    		return (SwcDataNode)data;
    	}
        return null;
    }
    public Object getData(){
        return data;
    }
    public String toString(){
        return ""+hashCode()+" "+data;
    }
    public int getTreeSize(){
    	return treeSize;
    }
    
    public void setTreeSize(int treeSize) {
		this.treeSize = treeSize;
	}
    
	public int getTreeSizeRecursively(){
    	int size = 1;
    	if (left != null){
    		size += left.getTreeSizeRecursively();
    	}
    	if (right != null){
    		size += right.getTreeSizeRecursively();
    	}
    	return size;
    }


    public static void updateTreeSizeDown(BinaryTreeNode node){
    	BinaryTreeNode current;
    	LinkedStack assembleStack = new LinkedStack();
    	LinkedStack branchStack = new LinkedStack();
    	int treeSize, height;
    	assembleStack.push(node);
    	branchStack.push(node);
    	while (!assembleStack.isEmpty()){
    		current = (BinaryTreeNode)assembleStack.pop();
    		if (current.hasLeft()){
    			assembleStack.push(current.getLeft());
    			branchStack.push(current.getLeft());
    		}
    		if (current.hasRight()){
    			assembleStack.push(current.getRight());
    			branchStack.push(current.getRight());
    		}
    	}
    	assembleStack = null;
    	while (!branchStack.isEmpty()){
    		current = (ParentedBinaryTreeNode)branchStack.pop();
    		treeSize = 1;
    		height = 0;
    		if (node.getLeft() != null){
    			treeSize += node.getLeft().getTreeSize();
    			height = node.getLeft().getHeight();
    		}
    		if (node.getRight() != null){
    			treeSize += node.getRight().getTreeSize();
    			if (node.getRight().getHeight() > height){
    				height = node.getRight().getHeight();
    			}
    		}
    		node.setTreeSize(treeSize);
    		node.setHeight(height+1);
    	}
    	branchStack = null;
    }

    public static void updateStatisticsDown(BinaryTreeNode node){
    	BinaryTreeNode current;
    	LinkedStack assembleStack = new LinkedStack();
    	LinkedStack branchStack = new LinkedStack();
    	int treeSize, maxHeight, totalHeight, leaves, leftSize, rightSize, leftDeg, rightDeg;
    	assembleStack.push(node);
    	branchStack.push(node);
    	while (!assembleStack.isEmpty()){
    		current = (BinaryTreeNode)assembleStack.pop();
    		if (current.hasLeft()){
    			assembleStack.push(current.getLeft());
    			branchStack.push(current.getLeft());
    		}
    		if (current.hasRight()){
    			assembleStack.push(current.getRight());
    			branchStack.push(current.getRight());
    		}
    	}
    	assembleStack = null;
    	while (!branchStack.isEmpty()){
    		current = (BinaryTreeNode)branchStack.pop();
    		treeSize = 1;
    		leaves = totalHeight = maxHeight = leftSize = rightSize = 0;
    		if (current.getLeft() != null){
    			leftSize = current.getLeft().getTreeSize();
    			treeSize += current.getLeft().getTreeSize();
    			leaves += current.getLeft().getLeaves();
    			maxHeight = current.getLeft().getHeight() + 1;
    			// totalLength is the total length of the children plus 1 for each child below (treeSize)
    			totalHeight += current.getLeft().getTotalHeight()
    				+ current.getLeft().getLeaves();
    		}
    		else if (current.getRight() == null){
    			leaves++;
    		}
    		if (current.getRight() != null){
    			rightSize = current.getRight().getTreeSize();
    			treeSize += current.getRight().getTreeSize();
    			leaves += current.getRight().getLeaves();
    			if (current.getRight().getHeight() >= maxHeight){
    				maxHeight = current.getRight().getHeight() + 1;
    			}
    			totalHeight += current.getRight().getTotalHeight()
    				+ current.getRight().getLeaves();
    		}
    		current.setTreeSize(treeSize);
    		current.setLeaves(leaves);
    		current.setHeight(maxHeight);
    		current.setTotalHeight(totalHeight);
    		rightDeg = rightSize/2 + 1;
    		leftDeg = leftSize/2 + 1;
    		// treeSize-1 so this node is not included
    		current.setAverageHeight((float)totalHeight/leaves);
    		if (treeSize > 3){
    			current.setPartitionAsymmetry((float)(Math.max(rightDeg, leftDeg) - Math.min(leftDeg, rightDeg)) /
    					(rightSize + leftSize - 2));
    			if (current.hasLeft() && current.hasRight()){
    				current.setTotalAsymmetry(
    					(float)current.getPartitionAsymmetry()
    					+ current.getRight().getTotalAsymmetry()
    					+ current.getLeft().getTotalAsymmetry());
    			}
    		}
    		else{
    			current.setPartitionAsymmetry(0);
    			current.setTotalAsymmetry(0);
    		}
    	}
    	branchStack = null;
    }

    public boolean hasChildren(){
        if (left == null && right == null){
           return false;
        }
        else{
           return true;
        }
     }
    
    public boolean hasLeft(){
    	//System.out.println("hasLeft: "+left);
    	if (left == null){
    		//System.out.println("FALSE");
    		return false;
    	}
    	else{
    	//	System.out.println("TRUE");
    		return true;
    	}
    }

    public boolean hasRight(){
       if (right == null){
          return false;
       }
       else{
          return true;
       }
    }

    public BinaryTreeNode removeOneChildNodes(){
    	BinaryTreeNode node = this;
    	if (hasLeft() && !hasRight()){
    		node = left;
    		if (node != null){
    			node.removeOneChildNodes();
    		}
    	}
    	else if (hasRight() && !hasLeft()){
    		node = right;
    		if (node != null){
    			node.removeOneChildNodes();
    		}
    	}
    	else{
    		left.removeOneChildNodes();
    		right.removeOneChildNodes();
    	}
    	return node;
    }
    
    public boolean isLeaf(){
    	/*System.out.println("isLeaf... hasLeft: "+hasLeft() + "; hasRight: "+hasRight()+
    			"; isLeaf: "+(!hasLeft() && !hasRight()));*/
    	return !hasLeft() && !hasRight();
    }
    
	public float getAverageHeight() {
		if (metaData != null){
			return metaData.getAverageHeight();
		}
		return 0;
	}
	public void setAverageHeight(float averageBranchHeight) {
		if (metaData == null){
			metaData = new NodeMetaData();
		}
		metaData.setAverageHeight(averageBranchHeight);
	}
	public int getLeaves() {
		if (metaData != null){
			return metaData.getLeaves();
		}
		return 0;
	}
	public void setLeaves(int leaves) {
		if (metaData == null){
			metaData = new NodeMetaData();
		}
		metaData.setLeaves(leaves);
	}
	public int getHeight() {
		if (metaData != null){
			return metaData.getHeight();
		}
		return 0;
	}
	public void setHeight(int height) {
		if (metaData == null){
			metaData = new NodeMetaData();
		}
		metaData.setHeight(height);
	}
	public int getTotalHeight() {
		if (metaData != null){
			return metaData.getTotalHeight();
		}
		return 0;
	}
	public void setTotalHeight(int totalHeight) {
		if (metaData == null){
			metaData = new NodeMetaData();
		}
		metaData.setTotalHeight(totalHeight);
	}
	public float getTreeAsymmetry() {
		if (metaData != null){
			return metaData.getTotalAsymmetry()/((treeSize-1)/2);
		}
		return 0;
	}
	
	public float getTotalAsymmetry() {
		if (metaData != null){
			return metaData.getTotalAsymmetry();
		}
		return 0;
	}
	public void setTotalAsymmetry(float totalAsymmetry) {
		if (metaData == null){
			metaData = new NodeMetaData();
		}
		metaData.setTotalAsymmetry(totalAsymmetry);
	}

	public float getBifurcationAsymmetry() {
		if (treeSize <= 2){
			return 0;
		}
		else if (treeSize == 3){
			return 1;
		}
		return (float)(treeSize - 2 * Math.min(left.getTreeSize(),right.getTreeSize()))
			/ (treeSize - 2);
	}
	
	public String toStringTraversed(){
		return toStringTraversed(TRAVERSE_SIZE, ORDER_LTS);
	}
	
	public String toStringTopology(String traversal, String order){
		//String[] topoMap = {"0","1","2"};
		String[] topoMap = {"A","","C"};
		return toStringTopology(traversal,order,topoMap);
	}
	public String toStringTopology(String traversal, String order, String[] topoMap){
		String out = "";
		if (traversal != null && order != null){
			String method = "toStringBy"+traversal;
			if (order.equals("LTS")){
				order = "LargeThenSmall";
			}
			else if (order.equals("STL")){
				order = "SmallThenLarge";
			}
			try{
				Object[] params = {order,topoMap};
				Class[] classes = {String.class,String[].class};
				out = (String)this.getClass().getMethod(method, classes).invoke(this, params);
			}
			catch (Exception e){
				System.out.println("ERROR: no such method '"+method+"' in class ParentedBinaryTreeNode\n");
				e.printStackTrace();
			}
		}
		return out;
	}

	public String toStringTraversed(String traversal, String order){
		String out = "";
		if (traversal != null && order != null){
			String method = "toStringBy"+traversal;
			if (order.equals("LTS")){
				order = "LargeThenSmall";
			}
			else if (order.equals("STL")){
				order = "SmallThenLarge";
			}
			try{
				Object[] params = {order};
				Class[] classes = {String.class};
				out = (String)this.getClass().getMethod(method, classes).invoke(this, params);
			}
			catch (Exception e){
				System.out.println("ERROR: no such method '"+method+"' in class BinaryTreeNode\n");
				e.printStackTrace();
			}
		}
		return out;
	}
	
	public String toStringBySize(String order) throws Exception{
		return toStringBySize(order,null);
	}
    public String toStringBySize(String order, String[] topoMap) throws Exception{
    	LinkedStack stack = new LinkedStack();
    	BinaryTreeNode current, right, left;
    	StringBuffer seq = new StringBuffer();
    	stack.push(this);
    	while (!stack.isEmpty()){
    		current = (BinaryTreeNode)stack.pop();
    		if (topoMap == null){
    			seq.append(current.getData());
    		}
    		else{
    			if (current.isLeaf()){
    				seq.append(topoMap[0]);
    			}
    			else if (current.hasLeft() && current.hasRight()){
    				seq.append(topoMap[2]);
    			}
    			else{
    				seq.append(topoMap[1]);
    			}
    		}
    		right = current.getRight();
    		left = current.getLeft();
        	if (right != null){
        		if (left != null){
    		    	if (right.getTreeSize() > left.getTreeSize()){
    		    		addToStack(stack, right, left, order);
    		    	}
    		    	else if (left.getTreeSize() > right.getTreeSize()){
    		    		addToStack(stack, left, right, order);
    		    	}
    		    	else if (right.getHeight() > left.getHeight()){
    		    		addToStack(stack, right, left, order);
        	    	}
        	    	else if (left.getHeight() > right.getHeight()){
    		    		addToStack(stack, left, right, order);
        	    	}
        	    	else if (right.getAverageHeight() > left.getAverageHeight()){
    		    		addToStack(stack, right, left, order);
            	    }
        	    	else if (left.getAverageHeight() > right.getAverageHeight()){
    		    		addToStack(stack, left, right, order);
        	    	}
        	    	else if (right.getTreeAsymmetry() > left.getTreeAsymmetry()){
    		    		addToStack(stack, right, left, order);
            	    }
        	    	else {
    		    		addToStack(stack, left, right, order);
        	    	}
        		}
        		else{
        			stack.push(right);
        		}
        	}
        	else if (left != null){
        		stack.push(left);
        	}
    	}

    	return seq.toString();
    }

    public String toStringByHeight(String order) throws Exception{
    	return toStringByHeight(order,null);
    }
    public String toStringByHeight(String order, String[] topoMap) throws Exception{
    	LinkedStack stack = new LinkedStack();
    	BinaryTreeNode current, right, left;
    	StringBuffer seq = new StringBuffer();
    	stack.push(this);
    	while (!stack.isEmpty()){
    		current = (BinaryTreeNode)stack.pop();
    		if (topoMap == null){
    			seq.append(current.getData());
    		}
    		else{
    			if (current.isLeaf()){
    				seq.append(topoMap[0]);
    			}
    			else if (current.hasLeft() && current.hasRight()){
    				seq.append(topoMap[2]);
    			}
    			else{
    				seq.append(topoMap[1]);
    			}
    		}
    		right = current.getRight();
    		left = current.getLeft();
        	if (right != null){
        		if (left != null){
    		    	if (right.getHeight() > left.getHeight()){
    		    		addToStack(stack, right, left, order);
        	    	}
        	    	else if (left.getHeight() > right.getHeight()){
    		    		addToStack(stack, left, right, order);
        	    	}
        	    	else if (right.getAverageHeight() > left.getAverageHeight()){
    		    		addToStack(stack, right, left, order);
            	    }
        	    	else if (left.getAverageHeight() > right.getAverageHeight()){
    		    		addToStack(stack, left, right, order);
        	    	}
        	    	else if (right.getTreeSize() > left.getTreeSize()){
    		    		addToStack(stack, right, left, order);
    		    	}
    		    	else if (left.getTreeSize() > right.getTreeSize()){
    		    		addToStack(stack, left, right, order);
    		    	}
        	    	else if (right.getTreeAsymmetry() > left.getTreeAsymmetry()){
    		    		addToStack(stack, right, left, order);
            	    }
        	    	else {
    		    		addToStack(stack, left, right, order);
        	    	}
        		}
        		else{
        			stack.push(right);
        		}
        	}
        	else if (left != null){
        		stack.push(left);
        	}
    	}

    	return seq.toString();
    }

    private void addToStack(
    		LinkedStack stack, 
    		BinaryTreeNode larger, 
    		BinaryTreeNode smaller,
    		String direction) throws Exception{
    	if (direction.equals(BinaryTreeNode.ORDER_LTS)){
    		stack.push(smaller);
    		stack.push(larger);
    	}
    	else if (direction.equals(BinaryTreeNode.ORDER_STL)){
    		stack.push(larger);
    		stack.push(smaller);
    	}
    	else{
    		throw new Exception("Improper value '"+direction+"' for parameter 'direction'."
    				+"Value must be '"+ORDER_STL+"' or '"+ORDER_LTS+"'.");
    	}
    }

    public String toStringByAvgHeight(String order) throws Exception{
    	return toStringByHeight(order,null);
    }

    public String toStringPreorder(){
    	LinkedStack stack = new LinkedStack();
    	BinaryTreeNode current, right, left;
    	StringBuffer seq = new StringBuffer();
    	stack.push(this);
    	while (!stack.isEmpty()){
    		current = (BinaryTreeNode)stack.pop();
    		seq.append(current.getData());
    		right = current.getRight();
    		left = current.getLeft();
        	if (right != null){
        		if (left != null){
        			stack.push(right);
        			stack.push(left);
        		}
        		else{
        			stack.push(right);
        		}
        	}
        	else if (left != null){
        		stack.push(left);
        	}
    	}

    	return seq.toString();
    }

    public int compareTo(Object o){
    	if (o == null){
    		return 1;
    	}
    	try{
    		BinaryTreeNode node = (BinaryTreeNode)o;
	    	if (treeSize != node.getTreeSize()){
	    		return treeSize - node.getTreeSize();	
	    	}
	    	else if (this.getHeight() != node.getHeight()){
	    		return this.getHeight() - node.getHeight();
	    	}
	    	else if (this.getAverageHeight() != node.getAverageHeight()){
	    		return (Math.round(this.getAverageHeight() - node.getAverageHeight()));
	    	}
	    	else {
	    		return this.getLeaves() - node.getLeaves();
	    	}
    	} catch (ClassCastException cce){
    		//System.out.println("ClassCaseException!");
    		return 1;
    	}
    }

    public static final int ASYMMETRY_WEIGHT_UNIFORM = 0;
    public static final int ASYMMETRY_WEIGHT_DEGREE_ABOVE_3 = 1;
    public static final int ASYMMETRY_WEIGHT_SIZE = 2;
    public static final int ASYMMETRY_WEIGHT_DF = 3;
    public static final int ASYMMETRY_WEIGHT_GLOBAL = 4;
    
    /* Traditional asymmetry calculation */
    public double getTreeAsymmetry(int weightMode) throws Exception{
    	if (!this.hasChildren()){
    		return 0;
    	}
		double totalWeightedAsymmetry = 0;
		double totalWeights = 0, weight;
		int degree;
    	BinaryTreeNode current;
    	LinkedStack calcStack = new LinkedStack();
    	calcStack.push(this);
    	while (!calcStack.isEmpty()){
    		current = (BinaryTreeNode)calcStack.pop();
    		if (current.getLeft().hasChildren()){
    			calcStack.push(current.getLeft());
    		}
    		if (current.getRight().hasChildren()){
    			calcStack.push(current.getRight());
    		}
    		degree = (current.getTreeSize() / 2) + 1;
    		switch (weightMode){
    		case ASYMMETRY_WEIGHT_UNIFORM:
    			weight = 1;
    			break;
    		case ASYMMETRY_WEIGHT_DEGREE_ABOVE_3:
    			if (degree > 3){
    				weight = 1;
    			}
    			else{
    				weight = 0;
    			}
    			break;
    		case ASYMMETRY_WEIGHT_SIZE:
    			if (degree > 3){
    				weight = degree - 2;
    			}
    			else{
    				weight = 0;
    			}
    			break;
    		case ASYMMETRY_WEIGHT_DF:
    			if (degree > 3){
    				weight = degree - 3;
    			}
    			else{
    				weight = 0;
    			}
    			break;
    		case ASYMMETRY_WEIGHT_GLOBAL:
   				weight = degree;
    			break;
    		default:
    			weight = 1;
    		}
    		totalWeights += weight;
    		totalWeightedAsymmetry += weight*current.getPartitionAsymmetry();
    	}
		return totalWeightedAsymmetry/totalWeights;
    }
    
    /* Asymmetry calculations for non-nodal measures */
	public double getTreeAsymmetry(int weightMode, int metric, int sizeMode)
	throws Exception{
		double totalWeightedAsymmetry = 0;
		double totalWeights = 0, weight;
		if (getAsymmetry(metric,sizeMode) == null){
			calculateAsymmetry(metric);
		}
		int degree;
    	BinaryTreeNode current;
    	LinkedStack calcStack = new LinkedStack();
    	calcStack.push(this);
    	Asymmetry asym;
    	while (!calcStack.isEmpty()){
    		current = (BinaryTreeNode)calcStack.pop();
    		asym = current.getAsymmetry(metric, sizeMode);
    		if (current.getLeft().hasChildren()){
    			calcStack.push(current.getLeft());
    		}
    		if (current.getRight().hasChildren()){
    			calcStack.push(current.getRight());
    		}
    		degree = (current.getTreeSize() / 2) + 1;
    		switch (weightMode){
    		case ASYMMETRY_WEIGHT_UNIFORM:
    			weight = 1;
    			break;
    		case ASYMMETRY_WEIGHT_DEGREE_ABOVE_3:
    			if (degree > 3){
    				weight = 1;
    			}
    			else{
    				weight = 0;
    			}
    			break;
    		case ASYMMETRY_WEIGHT_GLOBAL:
   				weight = asym.getTotalSize();
    			break;
    		default:
    			weight = 1;
    		}
    		totalWeights += weight;
    		totalWeightedAsymmetry += weight*current.getPartitionAsymmetry(metric,sizeMode);
    	}
		
		return totalWeightedAsymmetry/totalWeights;
	}
	
    public static final int LOPSIDEDNESS_BRANCHES = 0;
    public static final int LOPSIDEDNESS_DEGREE = 1;
    public double getLopsidedness() {
    	return getLopsidedness(LOPSIDEDNESS_BRANCHES);
    }
	public double getLopsidedness(int mode) {
		double sumDiff = 0, sumSum = 0;
		int leftDeg, rightDeg, leftBranch, rightBranch;
    	BinaryTreeNode current;
    	LinkedStack calcStack = new LinkedStack();
    	calcStack.push(this);
    	while (!calcStack.isEmpty()){
    		current = (BinaryTreeNode)calcStack.pop();
			leftDeg = rightDeg = 0;
			leftBranch = rightBranch = 0;
    		if (current.hasLeft()){
    			calcStack.push(current.getLeft());
    			leftDeg = current.getLeft().getDegree();
    			 //+1 connects tree to curr node
    			leftBranch = current.getLeft().getTreeSize()+1;
    		}
    		if (current.hasRight()){
    			calcStack.push(current.getRight());
    			rightDeg = current.getRight().getDegree();
    			rightBranch = current.getRight().getTreeSize()+1;
    		}
    		
    		switch (mode){
    		case LOPSIDEDNESS_DEGREE:
    			sumDiff += (Math.max(rightDeg, leftDeg) - Math.min(rightDeg, leftDeg));
    			sumSum += rightDeg + leftDeg;
    			break;
    		case LOPSIDEDNESS_BRANCHES:
    			sumDiff += (Math.max(rightBranch, leftBranch) - Math.min(rightBranch, leftBranch));
    			sumSum += rightBranch + leftBranch;
    			break;
    		}
    	}
		
		return sumDiff/sumSum;
	}
	
	private class NodeMetaData{
	    protected int height;
	    protected int totalHeight;
	    protected float averageBranchHeight;
	    protected int leaves;
	    protected float partitionAsymmetry;
	    protected float totalAsymmetry;
	    protected BigInteger shapeNum;
	    
		public float getAverageHeight() {
			return averageBranchHeight;
		}
		public void setAverageHeight(float averageBranchHeight) {
			this.averageBranchHeight = averageBranchHeight;
		}
		public int getLeaves() {
			return leaves;
		}
		public void setLeaves(int leaves) {
			this.leaves = leaves;
		}
		public int getHeight() {
			return height;
		}
		public void setHeight(int height) {
			this.height = height;
		}
		public int getTotalHeight() {
			return totalHeight;
		}
		public void setTotalHeight(int totalHeight) {
			this.totalHeight = totalHeight;
		}
		public float getTreeAsymmetry() {
			return totalAsymmetry/((treeSize-1)/2);
		}
		
		public float getTotalAsymmetry() {
			return totalAsymmetry;
		}
		public void setTotalAsymmetry(float totalAsymmetry) {
			this.totalAsymmetry = totalAsymmetry;
		}
		/**
		 * 
		 * @return partition asymmetry calculated as tree was built; based on degree, not size
		 */
		public float getPartitionAsymmetry() {
			return partitionAsymmetry;
		}
		public void setPartitionAsymmetry(float partitionAsymmetry) {
			this.partitionAsymmetry = partitionAsymmetry;
		}
		public float getBifurcationAsymmetry() {
			if (treeSize <= 2){
				return 0;
			}
			else if (treeSize == 3){
				return 1;
			}
			return (float)(treeSize - 2 * Math.min(left.getTreeSize(),right.getTreeSize()))
				/ (treeSize - 2);
		}
		public BigInteger getShapeNum() {
			return shapeNum;
		}
		public void setShapeNum(BigInteger shapeNum) {
			this.shapeNum = shapeNum;
		}
		
	}

	public void setPartitionAsymmetry(float partitionAsymmetry) {
		if (metaData == null){
			metaData = new NodeMetaData();
		}
		metaData.setPartitionAsymmetry(partitionAsymmetry);
	}

	public Asymmetry getAsymmetry()throws Exception{
		return getAsymmetry(Asymmetry.METRIC_NODES, Asymmetry.SIZE_MODE_SUM);
	}
	/**
	 * Traditional partition asymmetry 
	 * @return partition asymmetry calculated as tree was built; based on degree, not size
	 */

	public double getPartitionAsymmetry(){
		if (metaData != null){
			return metaData.getPartitionAsymmetry();
		}
		return 0;
	}

	/** 
	 * 
	 * @param metric
	 * @return Partition asymmetry based on size, not degree
	 * @throws Exception
	 */
	public double getPartitionAsymmetry(int metric)throws Exception{
		return getPartitionAsymmetry(metric, Asymmetry.SIZE_MODE_SUM);
	}
	
	public double getPartitionAsymmetry(int metric, int sizeMode)
	throws Exception{
		Asymmetry asym = null;

		asym = getAsymmetry(metric, sizeMode);
		if (asym == null){
			calculateAsymmetry(metric, sizeMode); 
			asym = getAsymmetry(metric, sizeMode);
		}
		return asym.getPartitionAsymmetry();
	}
	
	private Integer getAsymmetryKey(int metric, int sizeMode){
		int key = 0;
		if (sizeMode == Asymmetry.SIZE_MODE_MAX){
			key = 4;
		}
		switch (metric){
		case Asymmetry.METRIC_NODES:
			break;
		case Asymmetry.METRIC_PATH_DISTANCE:
			key = key + 1;
			break;
		case Asymmetry.METRIC_SURFACE_AREA:
			key = key + 2;
			break;
		case Asymmetry.METRIC_VOLUME:
			key = key + 3;
			break;
		}
		return new Integer(key);
	}
	
	public Asymmetry getAsymmetry(int metric)throws Exception{
		return getAsymmetry(metric,Asymmetry.SIZE_MODE_SUM);
	}
	public Asymmetry getAsymmetry(int metric, int sizeMode) throws Exception{
		Integer key = getAsymmetryKey(metric, sizeMode);
		if (!asymmetryMap.containsKey(key)){
			this.calculateAsymmetry(metric, sizeMode);
		}
		return (Asymmetry)asymmetryMap.get(getAsymmetryKey(metric, sizeMode));
	}

	public void setAsymmetry(int metric, Asymmetry asym){
		setAsymmetry(metric,Asymmetry.SIZE_MODE_SUM,asym);
	}
	public void setAsymmetry(int metric, int sizeMode, Asymmetry asym){
		asymmetryMap.put(getAsymmetryKey(metric,sizeMode), asym);
	}

	public int getDegree(){
		return treeSize/2 + 1;
	}
	public BinaryTreeNode getLargerSubtree()throws Exception{
		return getLargerSubtree(Asymmetry.METRIC_NODES);
	}
	public BinaryTreeNode getLargerSubtree(int metric)throws Exception{
		return getLargerSubtree(metric, Asymmetry.SIZE_MODE_SUM);
	}
	public BinaryTreeNode getLargerSubtree(int metric, int sizeMode)throws Exception{
		Asymmetry asym = getAsymmetry(metric, sizeMode);
		return asym == null ? null : (asym.isLargerSubtreeLeft() ? left : right);
	}
	public BinaryTreeNode getSmallerSubtree()throws Exception{
		return getSmallerSubtree(Asymmetry.METRIC_NODES);
	}
	public BinaryTreeNode getSmallerSubtree(int metric)throws Exception{
		return getSmallerSubtree(metric, Asymmetry.SIZE_MODE_SUM);
	}
	public BinaryTreeNode getSmallerSubtree(int metric, int sizeMode)
	throws Exception{
		Asymmetry asym = getAsymmetry(metric, sizeMode);
		return asym == null ? null : (asym.isLargerSubtreeLeft() ? right : left);
	}
	
	public BinaryTreeNode copy(BinaryTreeNode copy){
		return copy(copy,false);
	}
	
	private void setAsymmetryMap(Map asymmetryMap) {
		this.asymmetryMap = asymmetryMap;
	}
	public BinaryTreeNode copy(BinaryTreeNode copy, boolean keepData){
		Map asymMap = new HashMap();
		copy.setAsymmetryMap(asymMap);
		Object key;
		for (Iterator it = asymmetryMap.keySet().iterator(); it.hasNext();){
			key = it.next();
			asymMap.put(key,((Asymmetry)asymmetryMap.get(key)).copy());
		}
    	copy.setTreeSize(treeSize);
    	if (metaData != null){
    		copy.setAverageHeight(metaData.getAverageHeight());
    		copy.setHeight(metaData.getHeight());
    		copy.setLeaves(metaData.getLeaves());
    		copy.setPartitionAsymmetry(metaData.getPartitionAsymmetry());
    		copy.setTotalAsymmetry(metaData.getTotalAsymmetry());
    	}
    	if (keepData){
    		copy.setData(data);
    	}
    	return copy;
    }

	public BinaryTreeNode copy(){
    	return copy(new BinaryTreeNode());
    }
	
	public BigInteger getShapeNum() throws Exception{
		if (metaData == null){
			metaData = new NodeMetaData();
		}
		if (metaData.getShapeNum() == null){
			if (hasChildren()){
				BinaryTreeNode larger, smaller;
				BigInteger base;
				if (left.getDegree() == right.getDegree()){
					int deg = getLeft().getDegree();
					base = BinaryTreeUtils.partialSumOfNumShapes(getDegree(),deg-1);
					BigInteger shapeNumLeft = left.getShapeNum();
					BigInteger shapeNumRight = right.getShapeNum();
					BigInteger largerShapeNum, smallerShapeNum;
					if (shapeNumLeft.compareTo(shapeNumRight) > 0){
						largerShapeNum = shapeNumLeft;
						smallerShapeNum = shapeNumRight;
					}
					else{
						largerShapeNum = shapeNumRight;
						smallerShapeNum = shapeNumLeft;
					}
					BigInteger two = BigInteger.ONE.add(BigInteger.ONE);
					metaData.setShapeNum(base.add(smallerShapeNum).add(
							largerShapeNum.multiply(largerShapeNum.subtract(BigInteger.ONE))
								.divide(two)));
				}
				else{
					if (left.getDegree() > right.getDegree()){
						larger = left;
						smaller = right;
					}
					else{
						larger = right;
						smaller = left;
					}
					int smallerDeg = smaller.getDegree();
					base = BinaryTreeUtils.partialSumOfNumShapes(getDegree(),smallerDeg-1);
					metaData.setShapeNum(base.add(
							smaller.getShapeNum().add(
							larger.getShapeNum().subtract(BigInteger.ONE).multiply(
									BinaryTreeUtils.numShapes(smallerDeg)))));
				}
			}
			else{
				metaData.setShapeNum(BigInteger.ONE);
			}
		}
		return metaData.getShapeNum();
	}
	
	public void calculateAsymmetry(int metric) throws Exception{
		calculateAsymmetry(metric, Asymmetry.SIZE_MODE_SUM);
	}
	public void calculateAsymmetry(int metric, int sizeMode)
	throws Exception{
		if (metric != Asymmetry.METRIC_NODES 
				&& !getData().getClass().equals(SwcDataNode.class)){
			throw new Exception("Data member in ParentedBinaryTreeNode must be of type "+SwcDataNode.class+
					". Data member of given in put is type "+getData().getClass());
		}
		// Get all bifurcating nodes from leaf to root
		LinkedStack stack = BinaryTreeUtils.getLeavesToRootStack(this, true);

		ParentedBinaryTreeNode node, left, right;
		SwcDataNode parentData, leftData, rightData;
		Asymmetry nodeAsym, rightAsym, leftAsym;
		double leftSize, rightSize, longerSummed, leftLargerSummed, rightLargerSummed, 
			allSummed, leftAllSummed, rightAllSummed;

		if (stack.isEmpty()){
			return;
		}
		// Go through all bifurcation nodes
		while (!stack.isEmpty()){
			node = (ParentedBinaryTreeNode)(stack.pop());
			left = node.getParentedLeft();
			right = node.getParentedRight();
			
			// Get sizes based on given metric
			switch (metric){
			case Asymmetry.METRIC_NODES:
				leftSize = rightSize = 1;
				break;
			case Asymmetry.METRIC_EUCLIDEAN_DISTANCE:
				parentData = (SwcDataNode)node.getData();
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
			case Asymmetry.METRIC_SURFACE_AREA:
				parentData = (SwcDataNode)node.getData();
				leftData = (SwcDataNode)left.getData();
				rightData = (SwcDataNode)right.getData();
				leftSize = leftData.getSecondaryData().getSurfaceArea();
				rightSize = rightData.getSecondaryData().getSurfaceArea();
				break;
			case Asymmetry.METRIC_VOLUME:
				parentData = (SwcDataNode)node.getData();
				leftData = (SwcDataNode)left.getData();
				rightData = (SwcDataNode)right.getData();
				leftSize = leftData.getSecondaryData().getVolume();
				rightSize = rightData.getSecondaryData().getVolume();
				break;
			default:
				throw new Exception("Unknown distance measure mode "+metric);
			}
			longerSummed = rightLargerSummed = leftLargerSummed = 0;
			allSummed = rightAllSummed = leftAllSummed = 0;

			// Add to total size of each sub-tree;
			if (left.hasChildren()){
				leftAsym = left.getAsymmetry(metric, sizeMode);
				if (sizeMode == Asymmetry.SIZE_MODE_SUM){
					// total size will be the longer + shorter lengths of each of left's sub-tree
					leftSize += 
						leftAsym.getLargerSize() + leftAsym.getSmallerSize();
				}
				else{
					// Max size of the left subtree will be the max size of this child + the branch from this node to its child
					leftSize += leftAsym.getLargerSize();
				}
				// keeping track of the sum of larger tree lengths for running caulescence calc.
				leftLargerSummed = leftAsym.getLargerSummedSize();
				// keeping track of the sum of all tree lengths for running caulescence calc.
				leftAllSummed = leftAsym.getAllSummedSize();
			}
			if (right.hasChildren()){
				rightAsym = right.getAsymmetry(metric, sizeMode);
				if (sizeMode == Asymmetry.SIZE_MODE_SUM){
					rightSize += 
						rightAsym.getLargerSize() + rightAsym.getSmallerSize();
				}
				else{
					rightSize += rightAsym.getLargerSize();
				}
				rightLargerSummed = rightAsym.getLargerSummedSize();
				rightAllSummed = rightAsym.getAllSummedSize();
			}
			
			// Create new Asymmetry object
			nodeAsym = new Asymmetry();
			node.setAsymmetry(metric, sizeMode, nodeAsym);
			// Add current node's total length to the summation
			allSummed += leftSize + rightSize;
			// determine larger/longer sub-tree
			if (leftSize > rightSize){
				nodeAsym.setLargerSize(leftSize);
				nodeAsym.setSmallerSize(rightSize);
				longerSummed = leftSize + leftLargerSummed;
				allSummed += leftAllSummed;
				nodeAsym.setMainPath(Asymmetry.MAIN_BRANCH_LEFT);
				nodeAsym.setLargerSubtree(Asymmetry.MAIN_BRANCH_LEFT);
			}
			else{
				nodeAsym.setLargerSize(rightSize);
				nodeAsym.setSmallerSize(leftSize);
				longerSummed = rightSize + rightLargerSummed;
				allSummed += rightAllSummed;
				nodeAsym.setMainPath(Asymmetry.MAIN_BRANCH_RIGHT);
				nodeAsym.setLargerSubtree(Asymmetry.MAIN_BRANCH_RIGHT);
			}
			nodeAsym.setLargerSummedSize(longerSummed);
			nodeAsym.setAllSummedSize(allSummed);
			
		}
	}

}
