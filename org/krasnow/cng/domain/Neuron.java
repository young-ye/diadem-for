package org.krasnow.cng.domain;

import java.io.File;

public class Neuron implements Comparable{
	
	private String name;
	private String animal;
	private String brainRegion;
	private String cellType;
	private String labName;
	
	private Soma soma;

	private File swcFile;
	
	public String getAnimal() {
		return animal;
	}
	public void setAnimal(String animal) {
		this.animal = animal;
	}
	public String getBrainRegion() {
		return brainRegion;
	}
	public void setBrainRegion(String brainRegion) {
		this.brainRegion = brainRegion;
	}
	public String getCellType() {
		return cellType;
	}
	public void setCellType(String cellType) {
		this.cellType = cellType;
	}
	public File getSwcFile() {
		return swcFile;
	}
	public void setSwcFile(File swcFile) {
		this.swcFile = swcFile;
	}
	public String getLabName() {
		return labName;
	}
	public void setLabName(String labName) {
		this.labName = labName;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Soma getSoma() {
		return soma;
	}
	public void setSoma(Soma soma) {
		this.soma = soma;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(name);
		sb.append(" / ");
		sb.append(cellType);
		sb.append(" / ");
		sb.append(brainRegion);
		sb.append(" / ");
		sb.append(animal);
		sb.append(" / ");
		sb.append(labName);
		return sb.toString();
	}

	public int compareTo(Object o){
		if (o == null){
			return 1;
		}
		Neuron neuron = (Neuron)o;
		if (name == null){
			return (neuron.getName() == null ? 0 : -1);
		}
		if (neuron.getName() == null){
			return 1;
		}
		return name.compareTo(neuron.getName());
	}
	
}
