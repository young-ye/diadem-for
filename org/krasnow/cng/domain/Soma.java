package org.krasnow.cng.domain;

import java.util.List;

/**
 * 
 * @author gillette
 * Contains data about the neuron as well as various potential types of processes.
 * Dendrites of non-pyramidal cells can be stored as "basal" trees.
 *
 */
public class Soma {

	private Neuron neuron;
	private NeuronProcess axonalTree;
	// Used for non-apical trees. 
	// Multiple dendrites may be combined with artificial nodes into one dendritic tree
	private NeuronProcess basalDendriteTree;
	private NeuronProcess apicalDendriteTree;
	
	/**
	 * List objects are for when more than one tree that exists for a dendrite
	 * type should be represented as separate.
	 **/
	private List basalDendriteTrees;
	private List apicalDendriteTrees;
	
	// For a full cell representation
	private ParentedBinaryTreeNode allTreesTogether;
	
	public NeuronProcess getApicalDendriteTree() {
		return apicalDendriteTree;
	}
	public boolean hasApicalDendriteTree(){
		return apicalDendriteTree != null && apicalDendriteTree.getTreeHead() != null;
	}
	public boolean hasApicalDendriteTrees(){
		return apicalDendriteTrees != null && apicalDendriteTrees.size() > 0;
	}
	public void setApicalDendriteTree(NeuronProcess apicalDendriteTree) {
		this.apicalDendriteTree = apicalDendriteTree;
	}
	public NeuronProcess getAxonalTree() {
		return axonalTree;
	}
	public boolean hasAxonalTree(){
		return axonalTree != null && axonalTree.getTreeHead() != null;
	}
	public void setAxonalTree(NeuronProcess axonalTree) {
		this.axonalTree = axonalTree;
	}
	public NeuronProcess getBasalDendriteTree() {
		return basalDendriteTree;
	}
	public boolean hasBasalDendriteTree(){
		return basalDendriteTree != null && basalDendriteTree.getTreeHead() != null;
	}
	public boolean hasBasalDendriteTrees(){
		return basalDendriteTrees != null && basalDendriteTrees.size() > 0;
	}
	public void setBasalDendriteTree(NeuronProcess basalDendriteTree) {
		this.basalDendriteTree = basalDendriteTree;
	}
	public List getApicalDendriteTrees() {
		return apicalDendriteTrees;
	}
	public void setApicalDendriteTrees(List apicalDendriteTrees) {
		this.apicalDendriteTrees = apicalDendriteTrees;
	}
	public List getBasalDendriteTrees() {
		return basalDendriteTrees;
	}
	public void setBasalDendriteTrees(List basalDendriteTrees) {
		this.basalDendriteTrees = basalDendriteTrees;
	}
	public ParentedBinaryTreeNode getAllTreesTogether() {
		return allTreesTogether;
	}
	public void setAllTreesTogether(ParentedBinaryTreeNode allTreesTogether) {
		this.allTreesTogether = allTreesTogether;
	}
	public Neuron getNeuron() {
		return neuron;
	}
	public void setNeuron(Neuron neuron) {
		this.neuron = neuron;
	}
	
}
