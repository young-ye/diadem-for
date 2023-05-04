package org.krasnow.cng.data;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.krasnow.cng.domain.DistributionBean;

/**
 * 
 * @author gillette
 * Helper methods for writing data to files.
 *
 */
public class DataWriter {

	public static void writeStringToFile(String fileName, StringBuffer data)
	throws FileNotFoundException, IOException{
		writeStringToFile(fileName,data.toString(),false);
	}
	public static void writeStringToFile(String fileName, String data)
	throws FileNotFoundException, IOException{
		writeStringToFile(fileName,data,false);
	}
	public static void writeStringToFile(String fileName, String data, boolean append)
	throws FileNotFoundException, IOException{
		writeStringToFile(new File(fileName),data,append);
	}

	public static void writeStringToFile(File file, String data)
	throws FileNotFoundException, IOException{
		writeStringToFile(file, data, false);
	}
	public static void writeStringToFile(File file, String data, boolean append)
	throws FileNotFoundException, IOException{
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try{
			fos = new FileOutputStream(file, append);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			
		    dos.writeBytes(data);
		} catch (IOException ioe){
			throw ioe;
		} finally {
			if (dos != null){
				dos.close();
			}
			if (bos != null){
				bos.close();
			}
			if (fos != null){
				fos.close();
			}
		}
	}

	public static void appendStringToFile(String fileName, String data)
	throws FileNotFoundException, IOException{
		writeStringToFile(new File(fileName),data,true);
	}

	public static void appendStringToFile(File file, String data)
	throws FileNotFoundException, IOException{
		writeStringToFile(file,data,true);
	}

	public static void writeDistributionToFile(String fileName, DistributionBean bean)
	throws FileNotFoundException, IOException{
		writeDistributionToFile(new File(fileName),bean);
	}
	public static void writeDistributionToFile(File file, DistributionBean bean)
	throws FileNotFoundException, IOException{
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		StringBuffer sb = new StringBuffer();
		List data = bean.getData();
		for (int i = 0; i < data.size(); i++){
			sb.append(data.get(i)+"\r\n");
		}
		try{
			fos = new FileOutputStream(file);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			
		    dos.writeBytes(sb.toString());
		} catch (IOException ioe){
			throw ioe;
		} finally {
			if (dos != null){
				dos.close();
			}
			if (bos != null){
				bos.close();
			}
			if (fos != null){
				fos.close();
			}
		}
	}
	
}
