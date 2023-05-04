package org.krasnow.cng.domain;

public class NodePair {

	private SwcDataNode swcData1;
	private SwcDataNode swcData2;
	private double distance = 0;
	private double distanceSq = 0;
	
	public NodePair(){}
	public NodePair(SwcDataNode swcData1, SwcDataNode swcData2){
		this.swcData1 = swcData1;
		this.swcData2 = swcData2;
		setDistanceSquared();
	}
	public SwcDataNode getSwcDataNode1() {
		return swcData1;
	}
	public void setSwcDataNode1(SwcDataNode swcData1) {
		this.swcData1 = swcData1;
		if (swcData2 != null){
			setDistanceSquared();
		}
	}
	public SwcDataNode getSwcDataNode2() {
		return swcData2;
	}
	public void setNode2(SwcDataNode swcData2) {
		this.swcData2 = swcData2;
		if (swcData1 != null){
			setDistanceSquared();
		}
	}
	public double getDistance(){
		if (distanceSq > 0){
			distance = Math.sqrt(distanceSq);
		}
		return distance;
	}
	public void setDistance(double distance){
		this.distance = distance;
		distanceSq = -1;
	}
	public void setDistanceSquared(){
		double dx = swcData2.getX() - swcData1.getX();
		double dy = swcData2.getY() - swcData1.getY();
		double dz = swcData2.getZ() - swcData1.getZ();
		distanceSq = dx*dx + dy*dy + dz*dz;
	}
	public double getDistanceSq() {
		return distanceSq;
	}
	public void setDistanceSq(double distanceSq) {
		this.distanceSq = distanceSq;
	}
	
}
