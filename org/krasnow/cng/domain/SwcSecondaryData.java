package org.krasnow.cng.domain;

public class SwcSecondaryData {
	
	private double localAngleOfBifurcation;
	private double remoteAngleOfBifurcation;
	// Path distance is the same as path length
	private double pathLength;
	private double XYPathLength;
	private double ZPathLength;
	private double surfaceArea;
	private double volume;
	private double euclideanDistance;
	
	private EuclideanPoint parentTrajectoryPoint;
	private EuclideanPoint leftTrajectoryPoint;
	private EuclideanPoint rightTrajectoryPoint;

	public EuclideanPoint getLeftTrajectoryPoint() {
		return leftTrajectoryPoint;
	}
	public void setLeftTrajectoryPoint(EuclideanPoint leftTrajectoryPoint) {
		this.leftTrajectoryPoint = leftTrajectoryPoint;
	}
	public EuclideanPoint getParentTrajectoryPoint() {
		return parentTrajectoryPoint;
	}
	public void setParentTrajectoryPoint(EuclideanPoint parentTrajectoryPoint) {
		this.parentTrajectoryPoint = parentTrajectoryPoint;
	}
	public EuclideanPoint getRightTrajectoryPoint() {
		return rightTrajectoryPoint;
	}
	public void setRightTrajectoryPoint(EuclideanPoint rightTrajectoryPoint) {
		this.rightTrajectoryPoint = rightTrajectoryPoint;
	}
	public double getEuclideanDistance() {
		return euclideanDistance;
	}
	public void setEuclideanDistance(double euclideanDistance) {
		this.euclideanDistance = euclideanDistance;
	}
	public double getLocalAngleOfBifurcation() {
		return localAngleOfBifurcation;
	}
	public void setLocalAngleOfBifurcation(double localAngleOfBifurcation) {
		this.localAngleOfBifurcation = localAngleOfBifurcation;
	}
	public double getPathLength() {
		return pathLength;
	}
	public void setPathLength(double pathLength) {
		this.pathLength = pathLength;
	}
	public void incrementPathLength(double pathLength) {
		this.pathLength += pathLength;
	}
	public double getRemoteAngleOfBifurcation() {
		return remoteAngleOfBifurcation;
	}
	public void setRemoteAngleOfBifurcation(double remoteAngleOfBifurcation) {
		this.remoteAngleOfBifurcation = remoteAngleOfBifurcation;
	}
	public double getSurfaceArea() {
		return surfaceArea;
	}
	public void setSurfaceArea(double surfaceArea) {
		this.surfaceArea = surfaceArea;
	}
	public double getVolume() {
		return volume;
	}
	public void setVolume(double volume) {
		this.volume = volume;
	}
	public double getXYPathLength() {
		return XYPathLength;
	}
	public void setXYPathLength(double pathLength) {
		XYPathLength = pathLength;
	}
	public void incrementXYPathLength(double pathLength) {
		this.XYPathLength += pathLength;
	}

	public double getZPathLength() {
		return ZPathLength;
	}
	public void setZPathLength(double pathLength) {
		ZPathLength = pathLength;
	}
	public void incrementZPathLength(double pathLength) {
		this.ZPathLength += pathLength;
	}
	public void incrementPathLengths(SwcSecondaryData dists){
		this.pathLength += dists.getPathLength();
		this.XYPathLength += dists.getXYPathLength();
		this.ZPathLength += dists.getZPathLength();
	}
	public void addData(SwcSecondaryData data){
		pathLength += data.getPathLength();
		XYPathLength += data.getXYPathLength();
		ZPathLength += data.getZPathLength();
		volume += data.getVolume();
		surfaceArea += data.getSurfaceArea();
	}
}
