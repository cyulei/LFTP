import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class slicerBuffer {
	public List<byte[]> sendQueue;
	slicerBuffer(){
		sendQueue=new ArrayList<byte[]>();
	}
	public void ReleasePacket(int seq) {//传入一个seq号，小于该seq号的包//迭代器爆炸
		Iterator<byte[]> iter=sendQueue.iterator();
		
		while(iter.hasNext()) {
			byte[] temp=iter.next();
			
			if(Formatter.getSEQ(temp)<seq){
				iter.remove();
			}
		}
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
		for(int i=0;i<sendQueue.size();++i) {
			if(Formatter.getSEQ(sendQueue.get(i))==Formatter.getSEQ(toStore)) {
				return;//此时链表中已经存在了，无需再次加入
			}
		}
		sendQueue.add(toStore);
	}
	public void Print() {
		for(int i=0;i<sendQueue.size();++i) {
			System.out.println(Formatter.getSEQ(sendQueue.get(i)));
		}
	}
	public byte[] GetPacket(int seq) {//传入seq号，返回该seq对应的包，如果不存在，那么应该报错
		byte[] temp = null;
		for(int i=0;i<sendQueue.size();++i) {
			if(Formatter.getSEQ(sendQueue.get(i))==seq) {
				temp=sendQueue.get(i);
				break;
			}
		}

		return temp;
	}
	void print() {
		for(int i=0;i<sendQueue.size();++i) {
			int temp = Formatter.getSEQ(sendQueue.get(i));
			System.out.println(temp);
		}
	}
}
