package org.krasnow.cng.domain;

public class NeuronProcess {

	private Neuron neuron;
	private Soma soma;
	private ParentedBinaryTreeNode treeHead;
	private String arborizationType;

	public NeuronProcess(){
		treeHead = new ParentedBinaryTreeNode();
	}
	public NeuronProcess(ParentedBinaryTreeNode treeHead){
		this.treeHead = treeHead;
	}

	public ParentedBinaryTreeNode getTreeHead() {
		return treeHead;
	}
	public void setTreeHead(ParentedBinaryTreeNode treeHead) {
		this.treeHead = treeHead;
	}
	public Neuron getNeuron() {
		return neuron;
	}
	public void setNeuron(Neuron neuron) {
		this.neuron = neuron;
	}
	public Soma getSoma() {
		return soma;
	}
	public void setSoma(Soma soma) {
		this.soma = soma;
	}
	public String getArborizationType() {
		return arborizationType;
	}
	public void setArborizationType(String arborizationType) {
		this.arborizationType = arborizationType;
	}
	
}
