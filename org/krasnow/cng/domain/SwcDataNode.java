package org.krasnow.cng.domain;

public class SwcDataNode extends EuclideanPoint implements Comparable {

	private int nodeId;
	private int parentId;
	private int type;
	private double radius;
	
	private SwcSecondaryData secondaryData;
	
	public int getNodeId() {
		return nodeId;
	}
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}
	public int getParentId() {
		return parentId;
	}
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	public String getPositionString(){
		return ("(X: "+super.getX()+"; Y: "+super.getY()+"; Z: "+super.getZ()+")");
	}
	public String toString(){
		return ("SwcDataNode [nodeId: "+nodeId+"; parentId: "+parentId+"; type: "+type+"; X: "+super.getX()+"; Y: "+super.getY()+"; Z: "+super.getZ()+"]");
	}

	public SwcSecondaryData getSecondaryData() {
		return secondaryData;
	}
	public void setSecondaryData(SwcSecondaryData secondaryData) {
		this.secondaryData = secondaryData;
	}
	public double getDiameter() {
		return 2*radius;
	}
	public double getRadius() {
		return radius;
	}
	public double getRadiusSquared() {
		return radius*radius;
	}
	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	public boolean equals(Object o){
		System.out.println(this.getClass() +" " +this.getClass());
		if (o != null && o.getClass().equals(this.getClass())){
			SwcDataNode node = (SwcDataNode)o;
			return (node.getNodeId() == this.getNodeId());
		}
		return false;
	}

	public int compareTo(Object o){
		if (o != null && o.getClass().equals(this.getClass())){
			SwcDataNode node = (SwcDataNode)o;
			return nodeId - node.getNodeId();
		}
		return 1;
	}

}
