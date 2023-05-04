package org.krasnow.cng.domain;

public class DoubleLinkedListNode {

	private Object object;
	private DoubleLinkedListNode previous;
	private DoubleLinkedListNode next;
	
	public DoubleLinkedListNode(){
		object = null;
		previous = null;
		next = null;
	}
	
	public DoubleLinkedListNode(Object object){
		this.object = object;
		previous = null;
		next = null;
	}
	
	public DoubleLinkedListNode getNext() {
		return next;
	}
	public void setNext(DoubleLinkedListNode next) {
		this.next = next;
	}
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	public DoubleLinkedListNode getPrevious() {
		return previous;
	}
	public void setPrevious(DoubleLinkedListNode previous) {
		this.previous = previous;
	}
	
	public boolean hasNext(){
		return (next != null);
	}

	public boolean hasPrevious(){
		return (previous != null);
	}
	
}
