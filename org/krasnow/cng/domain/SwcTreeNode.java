package org.krasnow.cng.domain;

public class SwcTreeNode extends TreeNode {

	private SwcDataNode swcData = new SwcDataNode();
	
	public SwcDataNode getSwcData() {
		return swcData;
	}
	public SwcTreeNode getSwcParent(){
		return (SwcTreeNode)parent;
	}
	public int getNodeId() {
		return swcData.getNodeId();
	}
	public void setNodeId(int nodeId) {
		swcData.setNodeId(nodeId);
	}
	public int getParentId() {
		return swcData.getParentId();
	}
	public void setParentId(int parentId) {
		swcData.setParentId(parentId);
	}
	public int getType() {
		return swcData.getType();
	}
	public void setType(int type) {
		swcData.setType(type);
	}
	
	public String toString(){
		return ("SwcTreeNode [nodeId: "+getNodeId()+"; parentId: "+getParentId()+"; type: "+getType()+"]");
	}
	public double getX() {
		return swcData.getX();
	}
	public void setX(double x) {
		swcData.setX(x);
	}
	public double getY() {
		return swcData.getY();
	}
	public void setY(double y) {
		swcData.setY(y);
	}
	public double getZ() {
		return swcData.getZ();
	}
	public void setZ(double z) {
		swcData.setZ(z);
	}
	public SwcSecondaryData getSecondaryData() {
		return swcData.getSecondaryData();
	}
	public void setSecondaryData(SwcSecondaryData secondaryData) {
		swcData.setSecondaryData(secondaryData);
	}
	public double getDiameter() {
		return swcData.getDiameter();
	}
	public double getRadius() {
		return swcData.getRadius();
	}
	public double getRadiusSquared() {
		return swcData.getRadiusSquared();
	}
	public void setRadius(double radius) {
		swcData.setRadius(radius);
	}
	public SwcTreeNode getSwcChild(int i){
		if (children != null && children.size() > i){
			return (SwcTreeNode)children.get(i);
		}
		return null;
	}
	
	public boolean equals(Object o){
		if (o != null && o.getClass().equals(this.getClass())){
			SwcTreeNode node = (SwcTreeNode)o;
			return (node.getNodeId() == this.getNodeId());
		}
		return false;
	}

	public int compareTo(Object o){
		if (o != null && o.getClass().equals(this.getClass())){
			SwcTreeNode node = (SwcTreeNode)o;
			return getNodeId() - node.getNodeId();
		}
		return 1;
	}
	
}
