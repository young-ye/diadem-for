package org.krasnow.cng.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.krasnow.cng.domain.LinkedStack;

public class StringUtils {

	public static String getNeuronNameFromSwcFile(String swcFilename){
		Matcher m = Pattern.compile("(.*)\\.CNG?\\.swc").matcher(swcFilename);
		if (m.matches()){
			return m.group(1);
		}
		else{
			m = Pattern.compile("(.*)\\.swc").matcher(swcFilename);
			if (m.matches()){
				return m.group(1);
			}
		}
		return null;
	}
	
	public static String insertBreaksAtLength(String string, int chars){
		StringBuffer sb = new StringBuffer();
		int end;
		for (int i = 0; i < string.length(); i+=chars){
			end = i + chars;
			if (end > string.length()){
				end = string.length();
			}
			if (i > 0){
				sb.append("\r\n");
			}
			sb.append(string.subSequence(i, end));
		}
		return sb.toString();
	}

	public static boolean isEmpty(String string){
		return (string == null || string.equals(""));
	}
	
	public static void printList(List list){
		for (int i = 0; i < list.size(); i++){
			System.out.println(i+": "+list.get(i));
		}
	}

	private static Pattern arborP 
		= Pattern.compile("(.*)\\-(ApicalDendrite|BasalDendrite|Dendrite|Axon)");
	public static String getNeuronFromSequence(String sequence){
		Matcher m = arborP.matcher(sequence);
		if (m.matches()){
			return m.group(1);
		}
		return null;
	}

	public static String getArborizationType(String sequence){
		Matcher m = arborP.matcher(sequence);
		if (m.matches()){
			return m.group(2);
		}
		return null;
	}

	public static int getLengthFromName(String name){
		Pattern p = Pattern.compile("LEN(\\d+)NUM(\\d+)");
		Matcher m = p.matcher(name);
		if (m.matches()){
			return (new Integer(m.group(1))).intValue();
		}
		return 0;
	}

	public static int getNumFromName(String name){
		Pattern p = Pattern.compile("LEN(\\d+)NUM(\\d+)");
		Matcher m = p.matcher(name);
		if (m.matches()){
			return (new Integer(m.group(2))).intValue();
		}
		return 0;
	}

	public static Set generatePossibleACTMotifsSet(int length){
		return new TreeSet(generatePossibleACTMotifs(length));
	}
	public static List generatePossibleACTMotifs(int length){
		List list = new ArrayList();
		LinkedStack counterStack;
		List counters;
		String curr;
		NodeCounter counter;
		int i, j, k, num, power, currNum;
		boolean keep;
		for (i = 0; i < Math.pow(3, length); i++){
			curr = "";
			num = i;
			counter = null;
			counterStack = new LinkedStack();
			counters = new ArrayList();
			keep = true;
			//System.out.println(i);
			for (j = 1; j <= length; j++){
				power = (int)Math.pow(3,length-j);
				currNum = num / power;
				num = num - currNum * power;
				switch (currNum){
				case 0:
					curr = curr.concat("A");
					if (counter != null){
						counterStack.push(counter);
						counters.add(counter);
					}
					for (k = 0; k < counters.size(); k++){
						((NodeCounter)counters.get(k)).a();
					}
					counter = new NodeCounter();
					counters.add(counter);
					break;
				case 1:
					curr = curr.concat("C");
					for (k = 0; k < counters.size(); k++){
						((NodeCounter)counters.get(k)).c();
					}
					break;
				case 2:
					curr = curr.concat("T");
					for (k = 0; k < counters.size(); k++){
						((NodeCounter)counters.get(k)).t();
					}
					if (counter != null){
						boolean keepGoing = true;
						do{
							if (counter.up){
								counter.flip();
								keepGoing = false;
							}
							else{
								if (counter.getCount() < 0){
									keep = false;
									j = length;
								}
								else{
									counters.remove(counter);
									counter = (NodeCounter)counterStack.pop();
								}
							}
						} while (keep && keepGoing && counter != null);
					}
					break;
				}
				//if (i == 7) System.out.println("j="+j+"; curr = "+curr+"; count="+counter.count);
			}
			//if (i == 7) System.out.println(curr);
			for (j = 0; j < counters.size(); j++){
				if (((NodeCounter)counters.get(j)).count < 0){
					keep = false;
				}
			}
			if (keep){
				list.add(curr);
			}
			
		}
		return list;
	}
	
	private static final class NodeCounter{
		public boolean up = true;
		private int count = 0;
		public void c(){
			count += up ? 1 : -1;
		}
		public void a(){
			count += up ? 2 : -2;
		}
		public void t(){
			
		}
		public void flip(){
			up = false;
		}
		public int getCount(){
			return count;
		}
	}

}
