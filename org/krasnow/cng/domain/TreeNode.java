package org.krasnow.cng.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TreeNode implements Comparable {

    protected Object data;
    //protected List children;
    protected List children;
    protected TreeNode parent;
    protected long level;

    protected int branchSize;
    protected int height;
    protected int totalHeight;
    protected float averageHeight;
    protected int leaves;

    public TreeNode(){
        data = null;
        parent = null;
        children = new ArrayList();
        branchSize = 1;
    }
    public TreeNode(Object d){
        data = d;
        parent = null;
        children = new ArrayList();
        branchSize = 1;
    }
    public void setParent(TreeNode p){
    	setParent(p, true);
    	
    }
    public void setParent(TreeNode p, boolean addChild){
        parent = p;
        if (p != null && addChild){
        	if (!p.getChildren().contains(this)){
        		p.addChildNode(this, false);
        		p.updateBranchSize();
        	}
        }
    }
    public void addChildNode(TreeNode n){
    	addChildNode(n, true);
    }
    private void addChildNode(TreeNode n, boolean setParent){
    	if (setParent){
    		n.setParent(this, false);
    	}
//    	childrenMap.put(n.getData(), n);
    	children.add(n);
    	TreeNode.updateBranchStatisticsUp(this);
    }
    
    public void setData(Object d){
        data = d;
    }
    public Object getData(){
        return data;
    }
    
    public List getChildren(){
        return children;
    }
    public void setChildren(List children){
        this.children = children;
    }

    public String toString(){
        return ""+data;
    }
    
    public int getBranchSize(){
        return branchSize;
    }
    
    public int getBranchSizeRecursively(){
    	int size = 1;
    	
    	List list = children;
        for (int i = 0; i < list.size(); i++){
     	   size += ((TreeNode)list.get(i)).getBranchSizeRecursively();
        }
    	return size;
    }

    public void updateBranchSize(){
       branchSize = 1;
       TreeNode node;
       List list = children;
       for (int i = 0; i < list.size(); i++){
    	   node = (TreeNode)list.get(i);
    	   branchSize += node.getBranchSize();
       }
       
       if (parent != null){
          parent.updateBranchSize();
       }
    }

    public boolean hasChildren(){
       if (children == null || children.size() == 0){
          return false;
       }
       else{
          return true;
       }
    }

    public void detach(){
       TreeNode p = parent;
       if (p != null){
	       p.getChildren().remove(this);
	       TreeNode.updateBranchStatisticsUp(p);
	       parent = null;
       }
    }

    public String toStringLargeThenSmall(){
       String str = data.toString();
       List list = children;
       while (!list.isEmpty()){
    	   int largest = 0;
    	   int size = 0;
	       for (int i = 0; i < list.size(); i++){
	    	   if (((TreeNode)list.get(i)).getBranchSize() > size){
	    		   size = ((TreeNode)list.get(i)).getBranchSize();
	    		   largest = i;
	    	   }
	       }
	       str.concat(((TreeNode)list.get(largest)).toStringLargeThenSmall());
	       list.remove(largest);
       }
       return str;
    }

    public String toStringSmallThenLarge(){
        String str = data.toString();
        List list = children;
        while (!list.isEmpty()){
     	   int smallest = 0;
     	   int size = 10000;
 	       for (int i = 0; i < list.size(); i++){
 	    	   if (((TreeNode)list.get(i)).getBranchSize() < size){
 	    		   size = ((TreeNode)list.get(i)).getBranchSize();
 	    		   smallest = i;
 	    	   }
 	       }
 	       str.concat(((TreeNode)list.get(smallest)).toStringSmallThenLarge());
 	       list.remove(smallest);
        }
        return str;
    }

    public static void updateStatisticsDown(SwcTreeNode node){
    	SwcTreeNode current, child;
    	LinkedStack assembleStack = new LinkedStack();
    	LinkedStack branchStack = new LinkedStack();
    	int branchSize, maxHeight, totalHeight, leaves;
    	assembleStack.push(node);
    	branchStack.push(node);
    	while (!assembleStack.isEmpty()){
    		current = (SwcTreeNode)assembleStack.pop();
    		for (Iterator it = current.getChildren().iterator(); it.hasNext();){
    			child = (SwcTreeNode)it.next();
    			assembleStack.push(child);
    			branchStack.push(child);
    		}
    	}
    	assembleStack = null;
    	while (!branchStack.isEmpty()){
    		current = (SwcTreeNode)branchStack.pop();
    		branchSize = 1;
    		leaves = totalHeight = maxHeight = 0;
    		if (current.hasChildren()){
	    		for (Iterator it = current.getChildren().iterator(); it.hasNext();){
	    			child = (SwcTreeNode)it.next();
	
	    			branchSize += child.getBranchSize();
	    			leaves += child.getLeaves();
	    			maxHeight = child.getHeight() + 1;
	    			// totalLength is the total length of the children plus 1 for each child below (branchSize)
	    			totalHeight += child.getTotalHeight()
	    				+ child.getLeaves();
	    		}
    		}
    		else{
    			leaves++;
    		}
    		current.setBranchSize(branchSize);
    		current.setLeaves(leaves);
    		current.setHeight(maxHeight);
    		current.setTotalHeight(totalHeight);
    		// branchSize-1 so this node is not included
    		current.setAverageHeight((float)totalHeight/leaves);
    	}
    	branchStack = null;
    }

    public static void updateBranchStatisticsUp(TreeNode node){
    	TreeNode current = node, child;
    	int branchSize, maxHeight, totalHeight, leaves;
    	while (current != null){
    		branchSize = 1;
    		leaves = totalHeight = maxHeight = 0;
    		if (current.hasChildren()){
	    		for (Iterator it = current.getChildren().iterator(); it.hasNext();){
	    			child = (TreeNode)it.next();
	
	    			branchSize += child.getBranchSize();
	    			leaves += child.getLeaves();
	    			maxHeight = child.getHeight() + 1;
	    			// totalLength is the total length of the children plus 1 for each child below (branchSize)
	    			totalHeight += child.getTotalHeight()
	    				+ child.getLeaves();
	    		}
    		}
    		else{
    			leaves++;
    		}
    		current.setBranchSize(branchSize);
    		current.setLeaves(leaves);
    		current.setHeight(maxHeight);
    		current.setTotalHeight(totalHeight);
    		// branchSize-1 so this node is not included
    		current.setAverageHeight((float)totalHeight/leaves);
    		
    		current = current.getParent();
    	}
    }

    public int compareTo(Object o){
    	if (o == null){
    		return 1;
    	}
    	TreeNode node = (TreeNode)o;
    	if (branchSize != node.getBranchSize()){
    		return branchSize - node.getBranchSize();	
    	}
    	else if (height != node.getHeight()){
    		return height - node.getHeight();
    	}
    	else if (averageHeight != node.getAverageHeight()){
    		return (Math.round(averageHeight - node.getAverageHeight()));
    	}
    	else {
    		return leaves - node.getLeaves();
    	}
    }
	protected float getAverageHeight() {
		return averageHeight;
	}
	protected void setAverageHeight(float averageHeight) {
		this.averageHeight = averageHeight;
	}
	protected int getHeight() {
		return height;
	}
	protected void setHeight(int height) {
		this.height = height;
	}
	protected int getLeaves() {
		return leaves;
	}
	protected void setLeaves(int leaves) {
		this.leaves = leaves;
	}
	protected long getLevel() {
		return level;
	}
	protected void setLevel(long level) {
		this.level = level;
	}
	protected int getTotalHeight() {
		return totalHeight;
	}
	protected void setTotalHeight(int totalHeight) {
		this.totalHeight = totalHeight;
	}
	public TreeNode getParent() {
		return parent;
	}
	public boolean hasParent(){
		return parent != null;
	}
	protected void setBranchSize(int branchSize) {
		this.branchSize = branchSize;
	}
	public boolean isLeaf(){
		return children == null || children.isEmpty();
	}
    
}
