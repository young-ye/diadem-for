package org.krasnow.cng.domain;

public class LinkedQueue {

	DoubleLinkedListNode head = null;
	DoubleLinkedListNode tail = null;
	int size = 0;
	
	public void enqueue(Object object){
		DoubleLinkedListNode newNode = new DoubleLinkedListNode(object);
		newNode.setPrevious(tail);
		if (tail != null){
			tail.setNext(newNode);
		}
		tail = newNode;
		if (head == null){
			head = tail;
		}
		size++;
	}
	
	public Object dequeue(){
		Object obj = null;
		if (head != null){
			obj = head.getObject();
			if (head.hasNext()){
				head.getNext().setPrevious(null);
			}
			head = head.getNext();
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
