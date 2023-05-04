package org.krasnow.cng.utils;

import org.krasnow.cng.domain.EuclideanPoint;
import org.krasnow.cng.domain.SwcDataNode;

public class SwcDataUtils {

	/**
	 * @param point1
	 * @param point2
	 * @return distance between point centroids on XY plane
	 */
	public static double getXYDistance(EuclideanPoint point1, EuclideanPoint point2){
		double xDist = point1.getX() - point2.getX();
		double yDist = point1.getY() - point2.getY();
		
		return Math.sqrt(xDist*xDist + yDist*yDist);
	}

	/**
	 * @param point1
	 * @param point2
	 * @return distance between point centroids
	 */
	public static double getDistance(EuclideanPoint point1, EuclideanPoint point2){
		double xDist = point1.getX() - point2.getX();
		double yDist = point1.getY() - point2.getY();
		double zDist = point1.getZ() - point2.getZ();
		
		return Math.sqrt(xDist*xDist + yDist*yDist + zDist*zDist);
	}

	/**
	 * @param point1
	 * @param point2
	 * @return minimum distance between point surfaces (approximate, based on radius box)
	 */
	public static double getSurfaceDistance(SwcDataNode node1, SwcDataNode node2){
		double radiusDist = (node1.getDiameter() + node2.getDiameter())/2;
		double xDist = Math.abs(node1.getX() - node2.getX());
		double yDist = Math.abs(node1.getY() - node2.getY());
		double zDist = Math.abs(node1.getZ() - node2.getZ());
		if (xDist > radiusDist) xDist -= radiusDist;
		if (yDist > radiusDist) yDist -= radiusDist;
		if (zDist > radiusDist) zDist -= radiusDist;
		
		return Math.sqrt(xDist*xDist + yDist*yDist + zDist*zDist);
	}

	public static double getAngle(EuclideanPoint parent, EuclideanPoint point1, EuclideanPoint point2){
		EuclideanPoint v1 = getVector(parent, point1), v2 = getVector(parent, point2);
		normalizeVector(v1);
		normalizeVector(v2);
		return Math.acos(dotProduct(v1,v2));
	}
	
	public static EuclideanPoint getVector(EuclideanPoint base, EuclideanPoint end){
		EuclideanPoint v = new EuclideanPoint();
		v.setX(end.getX() - base.getX());
		v.setY(end.getY() - base.getY());
		v.setZ(end.getZ() - base.getZ());
		return v;
	}
	
	public static void normalizeVector(EuclideanPoint v){
		double length = Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
		v.setX(v.getX() / length);
		v.setY(v.getY() / length);
		v.setZ(v.getZ() / length);
	}
	
	public static double dotProduct(EuclideanPoint v1, EuclideanPoint v2){
		return v1.getX()*v2.getX() + v1.getY()*v2.getY() + v1.getZ()*v2.getZ();
	}
	
}
