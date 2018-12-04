
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.swing.Spring;

public class packer {
	FileOutputStream temp=null;
	packetBuffer buf;
	int currentSeq=-1;
	packer(String name){
		buf=new packetBuffer();
		try {
			temp = new FileOutputStream(name);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	void prePack(byte[] buffer) {//收到包，并将其添加入缓冲队列
		int tempseq=Formatter.getSEQ(buffer);//packetBuffer.byteArrayToInt(buffer, 0);
		if(tempseq<=currentSeq) {
			return;//要添加的包号必须大于当前最大包号，否则肯定是重复的
		}
		buf.addPacket(buffer);

	}
	void packing() {//从缓冲队列中提取元素，直到无法再提取
		while(buf.getTopSeq()==currentSeq+1) {//可以提取
			byte[] toWrite=buf.getPacket();
			int length_of_toWrite=Formatter.getDataLength(toWrite);//packetBuffer.byteArrayToInt(toWrite, 4);
			//System.out.println(length_of_toWrite);
			
			try {
				//System.out.println("toWrite.length:"+ toWrite.length + "length_of_toWrite:"+ length_of_toWrite);
				if(length_of_toWrite < 0)
					return;
				temp.write(toWrite, 32,length_of_toWrite);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//写入文件
			
			//System.out.println(currentSeq);
			currentSeq++;//提高当前的序列号
			buf.releasePacket();//移去顶端的包
		}
	}
	void pack(byte[] buffer) {//传入一个包，把他装载到对应的位置上
	//	System.out.println("once");
		prePack(buffer);
		packing();

	}
	int getMaxSeq() {
		return currentSeq;//返回当前写完的最大的seq
	}
}
