import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class slicer {
	public slicerBuffer senderBuffer;
	
	
	int count=-1;
	int len=1000;
	FileReader toRead=null;
	//BufferedReader br=null;
	FileInputStream in=null;
	byte[] buffer=new byte[1000];
	slicer(String fileName) throws FileNotFoundException{
		toRead=new FileReader(fileName);
		//br=new BufferedReader(toRead);
		in=new FileInputStream(new File(fileName));

		senderBuffer=new slicerBuffer();
	}
	
	public byte[] readFileByBytes() {
		count++;
		buffer=new byte[1000];
		try {
		//	System.out.println(buffer[2]);
			len=in.read(buffer);
			System.out.println(len);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer;
		
	}
	public boolean IsEnd() {
		if(len==1000)return false;
		else return true;
	}
	public byte[] getPack(int seq) {//输入标号,返回一个含有标号的包
		byte[] temp;
		if(seq==count+1) {//是还没有读入的包
			byte[] readed=readFileByBytes();
			/*byte[] length=intToByteArray(len);
			byte[] haslength=byteMerger(length,readed);
			//System.out.println(len+"xieru"+packetBuffer.byteArrayToInt(intToByteArray(len), 0));
			temp=byteMerger(intToByteArray(count),haslength);*/
			temp=Formatter.getFormat(0, 0, 0, seq, 0, 0, 0, len,readed);
			senderBuffer.StorePacket(temp);//暂存直到被ack
		}else {//是已经读过，理论上还在缓冲中的包
			temp=senderBuffer.GetPacket(seq);
		}

		//System.out.print(temp.length);

		return temp;
	}
/*	public static byte[] intToByteArray(int a) {   //把int转化成byte[]
		return new byte[] {   
		        (byte) ((a >> 24) & 0xFF),   
		        (byte) ((a >> 16) & 0xFF),      
		        (byte) ((a >> 8) & 0xFF),      
		        (byte) (a & 0xFF)   
		};   
	}  
    public static byte[] byteMerger(byte[] bt1, byte[] bt2){  //拼接两个byte[]
        byte[] bt3 = new byte[bt1.length+bt2.length];  
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);  
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);  
        return bt3;  
    } */
}
