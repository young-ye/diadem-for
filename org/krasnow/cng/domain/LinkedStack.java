package org.krasnow.cng.domain;

public class LinkedStack {

	private DoubleLinkedListNode head = null;
	private int size = 0;
	
	public void push(Object object){
		DoubleLinkedListNode newNode = new DoubleLinkedListNode(object);
		newNode.setNext(head);
		if (head != null){
			head.setPrevious(newNode);
		}
		head = newNode;
		size++;
	}
	
	public Object pop(){
		Object obj = null;
		if (head != null){
			obj = head.getObject();
			if (head.hasNext()){
				head = head.getNext();
				head.setPrevious(null);
			}
			else{
				head = null;
			}
		}
		size--;
		return obj;
	}
	
	public boolean isEmpty(){
		return (size == 0);
	}
	
	public int getSize(){
		return size;
	}

}
