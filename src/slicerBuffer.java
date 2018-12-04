import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class slicerBuffer {
//	public List<byte[]> sendQueue;
	Map<Integer,byte[]> sendMap = new HashMap<Integer,byte[]>();
	slicerBuffer(){
	//	sendQueue=new ArrayList<byte[]>();
	}
	public void ReleasePacket(int seq) {//传入一个seq号，小于该seq号的包//迭代器爆炸
		/*sendMap.entrySet();
		for(Map.Entry<Integer, byte[]> i:sendMap.entrySet()) {
			if(i.getKey()<seq)i.remove();
		}*/
		Iterator<Entry<Integer, byte[]>> temp=sendMap.entrySet().iterator();
		for (; temp.hasNext();){
		    Map.Entry<Integer, byte[]> item = temp.next();
		    if(item.getKey()<seq)temp.remove();
		    //todo with key and val
		    //you may remove this item using  "it.remove();"
		}
	/*	Iterator<byte[]> iter=sendQueue.iterator();
		
		while(iter.hasNext()) {
			byte[] temp=iter.next();
			
			if(Formatter.getSEQ(temp)<seq){
				iter.remove();
			}
		}
*/
		/*for(Iterator<byte[]> iter=sendQueue.iterator();iter.hasNext();) {
			byte[] temp=iter.next();
			if(Formatter.getSEQ(temp)<seq){
				sendQueue.remove(temp);
			}
		}*/

	/*	for(int i=0;i<sendQueue.size();++i) {

			if(Formatter.getSEQ(sendQueue.get(i))<=seq) {
				sendQueue.remove(i);
				break;
			}
		}*/
	}
	public void StorePacket(byte[] toStore) {//传入一个已经准备发射的包，将它存入缓冲

	/*	for(int i=sendQueue.size()-1;i>=0;--i) {
			if(Formatter.getSEQ(sendQueue.get(i))==Formatter.getSEQ(toStore)) {
				return;//此时链表中已经存在了，无需再次加入
			}
		}*/
	//	if(sendQueue.contains(toStore))return;
		//sendQueue.add(toStore);
		int seq=Formatter.getSEQ(toStore);
		if(sendMap.containsKey(seq)) {
			return;
		}else {
			sendMap.put(seq, toStore);
		}
	}
	public void Print() {
		//for(int i=0;i<sendQueue.size();++i) {
			//System.out.println(Formatter.getSEQ(sendQueue.get(i)));
		//}
		System.out.println(sendMap.size());
	}
	public byte[] GetPacket(int seq) {//传入seq号，返回该seq对应的包，如果不存在，那么应该报错
		return sendMap.get(seq);
	/*	byte[] temp = null;
		for(int i=sendQueue.size()-1;i>=0;--i) {
			if(Formatter.getSEQ(sendQueue.get(i))==seq) {
				temp=sendQueue.get(i);
				break;
			}
		}
		return temp;*/
	}

}
