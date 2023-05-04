package org.krasnow.cng.domain;


public class ParentedBinaryTreeNode extends BinaryTreeNode implements Comparable{
    private static final int RIGHT = 0;
    private static final int LEFT = 1;

    protected ParentedBinaryTreeNode left,right;
    protected ParentedBinaryTreeNode parent;
    protected int side; // left or right of parent
    protected long level;
    
    public ParentedBinaryTreeNode(){
        data = null;
        left = right = parent = null;
        treeSize = 1;
    }
    public ParentedBinaryTreeNode(Object d){
        data = d;
        left = right = parent = null;
        treeSize = 1;
    }
    public void setParent(ParentedBinaryTreeNode p){
    	setParent(p,true);
    }
    public void setParent(ParentedBinaryTreeNode p, boolean updateStats){
        parent = p;
        if (p != null && updateStats){
        	ParentedBinaryTreeNode.updateBranchStatisticsUp(p);
        }
        if (parent == null){
        	level = 0;
        }
        else{
        	level = parent.getLevel() + 1;
        }
    }
    public ParentedBinaryTreeNode getParent(){
    	return parent;
    }
    public BinaryTreeNode getRight(){
    	return right;
    }
    public BinaryTreeNode getLeft(){
    	return left;
    }
    public ParentedBinaryTreeNode getParentedRight(){
    	return right;
    }
    public ParentedBinaryTreeNode getParentedLeft(){
    	return left;
    }
    
    public boolean hasLeft(){
    	if (left == null){
    		return false;
    	}
    	else{
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

    public void setLeft(ParentedBinaryTreeNode l){
        setLeft(l,true);
    }
    public void setLeft(ParentedBinaryTreeNode l, boolean updateStats){
        //super.setLeft(l,false);
        if (left != null){
        	left.setParent(null);
            left.setLevel(0);
        }
        left = l;
        if (l != null){
	        left.setParent(this,false);
	        left.side = LEFT;
	        left.setLevel(this.level+1);
        }
        if (updateStats){
        	ParentedBinaryTreeNode.updateBranchStatisticsUp(l);
        }
    }
    
    public void setRight(ParentedBinaryTreeNode r){
    	setRight(r,true);
    }
    public void setRight(ParentedBinaryTreeNode r, boolean updateStats){
    	//super.setRight(r,false);
        if (right != null){
           right.setParent(null);
           right.setLevel(0);
        }
        right = r;
        if (r != null){
	        right.setParent(this,false);
	        right.side = RIGHT;
	        right.setLevel(this.level+1);
        }
        if (updateStats){
        	ParentedBinaryTreeNode.updateBranchStatisticsUp(r);
        }
    }
    
    public static void updateBranchStatisticsUp(ParentedBinaryTreeNode node){
    	ParentedBinaryTreeNode current = node;
    	int treeSize, maxHeight, totalHeight, leaves, leftSize, rightSize;
    	while (current != null){
    		treeSize = 1;
    		leaves = totalHeight = maxHeight = leftSize = rightSize = 0;
    		if (current.hasLeft()){
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
    		if (current.hasRight()){
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
    		// treeSize-1 so this node is not included
    		current.setAverageHeight((float)totalHeight/leaves);
    		if (treeSize > 3){
    			current.setPartitionAsymmetry((float)(rightSize + leftSize - 2 * Math.min(leftSize, rightSize)) /
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

    		current = current.getParent();
    	}
    }

    public boolean addLeft(ParentedBinaryTreeNode l){
       if (hasLeft()){
          return false;
       }
       else{
          left = l;
          ParentedBinaryTreeNode.updateBranchStatisticsUp(this);
          return true;
       }
    }

    public boolean addRight(ParentedBinaryTreeNode r){
       if (hasRight()){
          return false;
       }
       else{
          right = r;
          ParentedBinaryTreeNode.updateBranchStatisticsUp(this);
          return true;
       }
    }

    public void detach(){
    	detach(true);
    }

    public void detach(boolean updateParentStats){
        ParentedBinaryTreeNode p = parent;
        if (p != null){
 	       if (side == RIGHT){
 	          p.setRight(null,!updateParentStats);
 	       }
 	       else{
 	          p.setLeft(null,!updateParentStats);
 	       }
 	       if (updateParentStats){
 	    	   ParentedBinaryTreeNode.updateBranchStatisticsUp(p);
 	       }
        }
        parent = null;
     }
    
    public boolean isLeft(){
    	return side == LEFT;
    }
    
    public boolean isRight(){
    	return side == RIGHT;
    }

	public long getLevel() {
		return level;
	}
	public void setLevel(long level) {
		this.level = level;
	}
    
	public boolean hasParent(){
		return parent != null;
	}
	
    public boolean hasChildren(){
        if (left == null && right == null){
           return false;
        }
        else{
           return true;
        }
     }
	
    public boolean isRoot(){
    	return parent == null;
    }
    
    public ParentedBinaryTreeNode copyParented(){
    	return copyParented(false);
    }

    public ParentedBinaryTreeNode copyParented(boolean keepData){
    	ParentedBinaryTreeNode copy = new ParentedBinaryTreeNode();
    	copy.setLevel(level);
    	return (ParentedBinaryTreeNode)super.copy(copy, keepData);
    }
}