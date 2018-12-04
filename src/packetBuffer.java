import java.util.ArrayList;
import java.util.List;

public class packetBuffer {
	List<byte[]> receiveQueue;
	packetBuffer(){
		receiveQueue=new ArrayList<byte[]>();
	}
	void addPacket(byte[] toAdd) {//添加一个包，从前向后遍历当前receiveQueue内的所有元素，寻找符合自身序列号的，如果重复则不添加，否则添加
		int toAddSEQ=Formatter.getSEQ(toAdd);//byteArrayToInt(toAdd,0);
		if(receiveQueue.size()==0) {
			receiveQueue.add(toAdd);//直接添加的情况
			return;
		}
		for(int i=0;i<receiveQueue.size();++i) {
			int currentSEQ=Formatter.getSEQ(receiveQueue.get(i));//byteArrayToInt(receiveQueue.get(i),0);//得到当前的SEQ
			//int nextSEQ=byteArrayToInt(receiveQueue.get(i+1),0);
			if(toAddSEQ==currentSEQ) {
				System.out.println("dfsdf");
				return;//此时是加入元素能在列表中找到重复的
			}else if(toAddSEQ<currentSEQ) {
				System.out.println("sdfwer");
				receiveQueue.add(i, toAdd);//在该位置添加本元素
				return;
			}
			
			if(i==receiveQueue.size()-1) {//此时加入元素为当前的最大的
				receiveQueue.add(toAdd);
				return;
			}
		}
	}
	byte[] getPacket() {
		if(receiveQueue.size()==0) {
			System.out.println("No!!!!");
		}
		return receiveQueue.get(0);//返回队列初始处的元素
	}
	void releasePacket() {
		receiveQueue.remove(0);
	}
/*	public static int byteArrayToInt(byte[] src, int offset) {  //把byte数组转成int
	    int value;    
	    value = (int) ((src[offset+3] & 0xFF)   
	            | ((src[offset+2] & 0xFF)<<8)   
	            | ((src[offset+1] & 0xFF)<<16)   
	            | ((src[offset] & 0xFF)<<24));  
	    return value;  
	}*/
	public int getTopSeq() {
	//	System.out.println(receiveQueue.size());
		if(receiveQueue.size()==0)return-1;
		
		int temp=Formatter.getSEQ(getPacket());// byteArrayToInt(getPacket(),0);
		System.out.println("当前顶部SEQ"+temp+"当前接受方缓冲区大小"+receiveQueue.size());
		return temp;
	}

}
