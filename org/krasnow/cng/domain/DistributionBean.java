package org.krasnow.cng.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DistributionBean{
	protected int id;
	protected float total = 0;
	protected int count = 0;
	protected List data = null;
	protected double mean = 0;
	protected double variance = 0;
	protected double standardDeviation = 0;
	protected double standardError = 0;
	protected double skew = 0;
	protected double kurtosis = 0;
	
	// percentile and related value
	protected Map percentileMap = new LinkedHashMap(5);
	protected boolean meanReady = false;
	protected double interquartileRange = 0;
	
	protected double adjustedMean = 0;
	protected double adjustedStandardDeviation = 0;
	protected double adjustedVariation = 0;
	protected int adjustedCount = 0;
	protected Map adjustedPercentileMap = new LinkedHashMap(5);
	
	public DistributionBean(){
		data = new LinkedList();
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
		//meanReady = false;
	}
	public float getTotal() {
		return total;
	}
	public void setTotal(float total) {
		this.total = total;
		meanReady = false;
	}
	public double getMean(){
		if (!meanReady && count > 0){
			meanReady = true;
			mean = total/count;
		}
		return mean;
	}
	public double getAverage(){
		return getMean();
	}
	public void addData(int data){
		addData(new Integer(data));
	}
	public void addData(double data){
		addData(new Double(data));
	}
	public void addData(long data){
		addData(new Long(data));
	}
	public void addData(float data){
		addData(new Float(data));
	}
	public void addData(Number number){
		if (data == null){
			data = new LinkedList();
			total = 0;
		}
		total += number.doubleValue();
		int index = Collections.binarySearch(data, number);
		if (index < 0){
			data.add(-index - 1, number);
		}
		else{
			data.add(index, number);
		}
		count = data.size();
		mean = variance = standardDeviation = 0;
		meanReady = false;
	}
	public void addData(List dataList){
		for (int i = 0; i < dataList.size(); i++){
			addData((Number)dataList.get(i));
		}
	}

	public double getMedian(){
		return getPercentile(.5);
	}
	// Common percentiles
	public double get50thPercentile(){
		return getPercentile(.5);
	}
	public double get95thPercentile(){
		return getPercentile(.95);
	}
	public double get99thPercentile(){
		return getPercentile(.99);
	}
	
	// If percentile falls between two data points, returns an interpolation
	public double getPercentile(Double percentile){
		return getPercentile(percentile.doubleValue());
	}
	
	public double getPercentile(double percentile){
		if (percentile >= 1 && percentile < 100){
			percentile = percentile / 100;
		}
		Double percentileObj = new Double(percentile);
		if (meanReady && percentileMap.containsKey(percentileObj)){
			return ((Double)percentileMap.get(percentileObj)).doubleValue();
		}
		if (data == null || percentile >= 1 || percentile < 0){
			return 0;
		}
		double rank = percentile * (count + 1);
		double percentileIndex = rank - 1;
		double value;
		int below = (int)(percentileIndex);
		int above = (int)(percentileIndex)+1;
		if (below >= count - 1){
			value = ((Number)data.get(count - 1)).doubleValue();
		}
		else if (above <= 0){
			value = ((Number)data.get(0)).doubleValue();
		}
		else{
			double belowVal = ((Number)data.get(below)).doubleValue();
			double aboveVal = ((Number)data.get(above)).doubleValue();
			value = (belowVal * (percentile * count - below) 
				+ aboveVal * (above - percentile * count));
		}
		percentileMap.put(percentileObj, new Double(value));
		return value;
	}
	
	public double getStandardDeviation() {
		getMean();
		if (standardDeviation == 0 && data != null){
			getVariance();
			standardDeviation = Math.sqrt(variance);
		}
		return standardDeviation;
	}

	public double getStandardError() {
		getMean();
		if (standardError == 0 && data != null){
			if (variance == 0){
				getVariance();
				standardDeviation = Math.sqrt(variance);
			}
			standardError = standardDeviation/Math.sqrt(count);
		}
		return standardError;
	}

	public double getVariance() {
		getMean();
		double val;
		if (variance == 0 && data != null){
			for (int i = 0; i < data.size(); i++){
				val = ((Number)data.get(i)).doubleValue();
				variance += Math.pow((val - mean), 2);
			}
			variance = variance / (count-1);
		}
		return variance;
	}
	public List getData() {
		if (data != null){
			List copy = new LinkedList(data);
			return copy;
		}
		else{
			return null;
		}
	}
	
	public boolean equals(Object o){
		if (o != null && o.getClass().equals(this.getClass())){
			DistributionBean b = (DistributionBean)o;
			return (b.getId() == id);
		}
		return false;
	}

	public void setMean(double mean) {
		this.mean = mean;
		meanReady = true;
	}
	public void setStandardDeviation(double standardDeviation) {
		this.standardDeviation = standardDeviation;
		meanReady = true;
	}
	public void setVariance(double variance) {
		this.variance = variance;
		meanReady = true;
	}
	public void setPercentile(double percentile, double value) {
		if (percentile >= 1 && percentile < 100){
			percentile = percentile / 100;
		}
		percentileMap.put(new Double(percentile), new Double(value));
	}
	public int getAdjustedCount() {
		return adjustedCount;
	}
	public void setAdjustedCount(int adjCount) {
		this.adjustedCount = adjCount;
	}
	public double getAdjustedMean() {
		return adjustedMean;
	}
	public void setAdjustedMean(double adjMean) {
		this.adjustedMean = adjMean;
	}
	public Map getAdjustedPercentileMap() {
		return adjustedPercentileMap;
	}
	public void setAdjustedPercentileMap(Map adjustedPercentileMap) {
		this.adjustedPercentileMap = adjustedPercentileMap;
	}
	public void setAdjustedPercentile(double percentile, double value) {
		if (percentile >= 1 && percentile < 100){
			percentile = percentile / 100;
		}
		adjustedPercentileMap.put(new Double(percentile), new Double(value));
	}

	public double getAdjustedPercentile(Double percentile){
		return getAdjustedPercentile(percentile.doubleValue());
	}
	
	// Common percentiles
	public double getAdjusted50thPercentile(){
		return getAdjustedPercentile(.5);
	}
	public double getAdjusted95thPercentile(){
		return getAdjustedPercentile(.95);
	}
	public double getAdjusted99thPercentile(){
		return getAdjustedPercentile(.99);
	}

	public double getAdjustedPercentile(double percentile){
		Double percentileObj = new Double(percentile);
		if (adjustedPercentileMap.containsKey(percentileObj)){
			return ((Double)adjustedPercentileMap.get(percentileObj)).doubleValue();
		}
		if (percentile >= 1 && percentile < 100){
			percentileObj = new Double(percentile / 100);
			if (adjustedPercentileMap.containsKey(percentileObj)){
				return ((Double)adjustedPercentileMap.get(percentileObj)).doubleValue();
			}
		}
		return 0;
	}
	public double getAdjustedStandardDeviation() {
		return adjustedStandardDeviation;
	}
	public void setAdjustedStandardDeviation(double adjustedStandardDeviation) {
		this.adjustedStandardDeviation = adjustedStandardDeviation;
	}
	public double getAdjustedVariation() {
		return adjustedVariation;
	}
	public void setAdjustedVariation(double adjustedVariation) {
		this.adjustedVariation = adjustedVariation;
	}
	
	public double getInterquartileRange(){
		if (interquartileRange == 0){
			interquartileRange = getPercentile(.75) - getPercentile(.25);
		}
		return interquartileRange;
	}
	
	public boolean isOutlier(double value){
		return (value > getPercentile(.75) + 1.5*getInterquartileRange()
				|| value < getPercentile(.25) - 1.5*getInterquartileRange());
	}

	public double getLowerOutlierLimit(){
		return getPercentile(.25) - 1.5*getInterquartileRange();
	}

	public double getUpperOutlierLimit(){
		return getPercentile(.75) + 1.5*getInterquartileRange();
	}
	
	public double getSkew() {
		getMean();
		double val;
		if (skew == 0 && data != null){
			for (int i = 0; i < data.size(); i++){
				val = ((Number)data.get(i)).doubleValue();
				skew += Math.pow((val - mean), 3);
			}
			skew = skew / (count-1);
		}
		return skew;
	}

	public double getKurtosis() {
		getMean();
		double val;
		if (kurtosis == 0 && data != null){
			for (int i = 0; i < data.size(); i++){
				val = ((Number)data.get(i)).doubleValue();
				kurtosis += Math.pow((val - mean), 4);
			}
			kurtosis = kurtosis / (count-1);
		}
		return kurtosis;
	}

	public double getTtestT(double mean2){
		return (getMean() - mean2)/(getStandardDeviation()/Math.sqrt(getCount()));
	}
	
	public double getTtestT(DistributionBean dist2){
		if (dist2.getCount() == getCount()){
			return (getMean() - dist2.getMean()) /
					Math.sqrt((getVariance() + dist2.getVariance())/getCount());
		}
		else{
			return (getMean() - dist2.getMean()) /
					Math.sqrt(
							((double)1/getCount()+(double)1/dist2.getCount()) *
							(getVariance()*(getCount()-1) + dist2.getVariance()*(dist2.getCount()-1)) /
							(getCount() + dist2.getCount() - 2));
		}
	}
	
	
}