package org.krasnow.cng.domain;

public class Asymmetry implements Comparable{

	public static final int DIFF_MODE = 1;
	public static final int ORIGINAL_MODE = 2;
	
	public static final int METRIC_NODES = 1;
	public static final int METRIC_EUCLIDEAN_DISTANCE = 2;
	public static final int METRIC_PATH_DISTANCE = 3;
	public static final int METRIC_SURFACE_AREA = 4;
	public static final int METRIC_VOLUME = 5;
	
	public static final int SIZE_MODE_SUM = 0;
	public static final int SIZE_MODE_MAX = 1;
	
	public static final int MAIN_BRANCH_RIGHT = 0;
	public static final int MAIN_BRANCH_LEFT = 1;
	public static final int MAIN_BRANCH_BOTH = 2;
	
	private double largerSize;
	private double smallerSize;
	private double largerSummedSize;
	private double allSummedSize;
	private double caulescence = 0;
	private double partitionCaulescence = 0;
	private int numMainPathes = 1;
	private int mainBranch;
	private int largerSubtree;
	private BinaryTreeNode node;
	
	public double getLargerSize() {
		return largerSize;
	}
	public void setLargerSize(double largerSize) {
		this.largerSize = largerSize;
	}
	public double getSmallerSize() {
		return smallerSize;
	}
	public void setSmallerSize(double smallerSize) {
		this.smallerSize = smallerSize;
	}
	public double getTotalSize() {
		return smallerSize + largerSize;
	}
	
	public double getLargerSummedSize() {
		return largerSummedSize;
	}
	public void setLargerSummedSize(double largerSummedSize) {
		caulescence = 0;
		this.largerSummedSize = largerSummedSize;
	}
	public double getAllSummedSize() {
		return allSummedSize;
	}
	public void setAllSummedSize(double allSummedSize) {
		caulescence = 0;
		this.allSummedSize = allSummedSize;
	}
	public double getPartitionAsymmetry(){
		if (partitionCaulescence == 0 && !(largerSize == 0 && smallerSize == 0)){
			partitionCaulescence = (largerSize - smallerSize) / (smallerSize + largerSize);
		}
		return partitionCaulescence;
	}

	public double getCaulescence(){
		if (caulescence == 0 && !(largerSummedSize == 0 && allSummedSize == 0)){
			double shorterSummedSize = allSummedSize - largerSummedSize;
			caulescence = (largerSummedSize - shorterSummedSize) / allSummedSize;
		}
		return caulescence;
	}
	public double getCaulescence(int mode){
		double caulescence = 0;
		if (mode == ORIGINAL_MODE){
			caulescence = largerSummedSize / allSummedSize;
		}
		else if (mode == DIFF_MODE){
			double shorterSummedSize = allSummedSize - largerSummedSize;
			caulescence = (largerSummedSize - shorterSummedSize) / allSummedSize;
		}
		return caulescence;
	}

	public int getNumMainPathes() {
		return numMainPathes;
	}
	public void setNumMainPathes(int numMainPathes) {
		this.numMainPathes = numMainPathes;
	}
	public void incrementMainPathes(){
		numMainPathes++;
	}
	public int getMainPath() {
		return mainBranch;
	}
	public void setMainPath(int mainBranch) {
		this.mainBranch = mainBranch;
	}
	public boolean isMainPathLeft(){
		return mainBranch == MAIN_BRANCH_LEFT;
	}
	public boolean isMainPathBoth(){
		return mainBranch == MAIN_BRANCH_BOTH;
	}
	public boolean isMainPathRight(){
		return mainBranch == MAIN_BRANCH_RIGHT;
	}
	
	public BinaryTreeNode getNode() {
		return node;
	}
	public void setNode(BinaryTreeNode node) {
		this.node = node;
	}
	public int getLargerSubtree() {
		return largerSubtree;
	}
	public void setLargerSubtree(int largerSubtree) {
		this.largerSubtree = largerSubtree;
	}
	public boolean isLargerSubtreeLeft(){
		return largerSubtree == MAIN_BRANCH_LEFT;
	}
	public boolean isLargerSubtreeRight(){
		return largerSubtree == MAIN_BRANCH_RIGHT;
	}
	public int compareTo(Object obj){
		if (obj == null || !obj.getClass().equals(this.getClass())){
			return 1;
		}
		Asymmetry c = (Asymmetry)obj;
		if (caulescence > c.getCaulescence()){
			return 1;
		}
		else if (caulescence < c.getCaulescence()){
			return -1;
		}
		return 0;
	}
	// Subtracts effect (longer and shorter) of this node, adds summed values of new main branch part
	public void mergeSubtreeCaulescence(Asymmetry newSubMainPath){
		largerSummedSize = 
			largerSummedSize - largerSize + newSubMainPath.getLargerSummedSize();
		allSummedSize = 
			allSummedSize - (largerSize+smallerSize) 
			+ newSubMainPath.getAllSummedSize();
		mainBranch = MAIN_BRANCH_BOTH;
	}
	public Asymmetry copy(){
		Asymmetry copy = new Asymmetry();
		copy.setAllSummedSize(allSummedSize);
		copy.setLargerSubtree(largerSubtree);
		copy.setLargerSize(largerSize);
		copy.setLargerSummedSize(largerSummedSize);
		copy.setMainPath(mainBranch);
		copy.setNode(node);
		copy.setNumMainPathes(numMainPathes);
		copy.setSmallerSize(smallerSize);
		return copy;
	}
}
