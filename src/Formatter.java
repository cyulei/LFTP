
public class Formatter {//��������������ĵĸ�ʽ
	public static int getACK(byte[] input) {
		int result=byteArrayToInt(input,0);
		return result;
	}

	public static int getSYN(byte[] input) {
		int result=byteArrayToInt(input,4);
		return result;
	}
	public static int getFIN(byte[] input) {
		int result=byteArrayToInt(input,8);
		return result;
	}
	public static int getSEQ(byte[] input) {
		int result=byteArrayToInt(input,12);
		return result;
	}
	public static int getReceiveWindow(byte[] input) {
		int result=byteArrayToInt(input,16);
		return result;
	}
	public static int getblanket1(byte[] input) {
		int result=byteArrayToInt(input,20);
		return result;
	}
	public static int getblanket2(byte[] input) {
		int result=byteArrayToInt(input,24);
		return result;
	}
	public static int getDataLength(byte[] input) {
		int result=byteArrayToInt(input,28);
		return result;
		
	}
	public static byte[] getData(byte[] input) {
		byte[] temp = new byte[input.length - 32];
		System.arraycopy(input,32,temp,0,input.length - 32);
		return temp;
	}
	public static byte[] getFormat(int ack,int syn,int fin, int seq,int recw,int blanket1,int blanket2,int dataLength,byte[] data) {//ע��ȱʡ����//�Ż��㣬Ԥ�ȷ������ݶ���ƴ��
		byte[] format;//=new byte[1032];
		byte[] ACK=intToByteArray(ack);
		byte[] SYN=intToByteArray(syn);
		byte[] FIN=intToByteArray(fin);
		byte[] SEQ=intToByteArray(seq);
		byte[] RECW=intToByteArray(recw);
		byte[] BLANKET1=intToByteArray(blanket1);
		byte[] BLANKET2=intToByteArray(blanket2);
		byte[] DATALENGTH=intToByteArray(dataLength);
		format=byteMerger(ACK,SYN);
		format=byteMerger(format,FIN);
		format=byteMerger(format,SEQ);
		format=byteMerger(format,RECW);
		format=byteMerger(format,BLANKET1);
		format=byteMerger(format,BLANKET2);
		format=byteMerger(format,DATALENGTH);
		format=byteMerger(format,data);
		return format;
	}
	public static int byteArrayToInt(byte[] src, int offset) {  //��byte����ת��int
	    int value;    
	    System.out.println(src.length);
	    value = (int) ((src[offset+3] & 0xFF)   
	            | ((src[offset+2] & 0xFF)<<8)   
	            | ((src[offset+1] & 0xFF)<<16)   
	            | ((src[offset] & 0xFF)<<24));  
	    return value;  
	}
	public static byte[] intToByteArray(int a) {   //��intת����byte[]
		return new byte[] {   
		        (byte) ((a >> 24) & 0xFF),   
		        (byte) ((a >> 16) & 0xFF),      
		        (byte) ((a >> 8) & 0xFF),      
		        (byte) (a & 0xFF)   
		};   
	}  
    public static byte[] byteMerger(byte[] bt1, byte[] bt2){  //ƴ������byte[]
        byte[] bt3 = new byte[bt1.length+bt2.length];  
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);  
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);  
        return bt3;  
    } 
}
