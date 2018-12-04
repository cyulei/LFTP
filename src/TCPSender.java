import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPSender {
	FileOutputStream TCPSenderLog=null;				 //日志
	//--------通用---------------------------------------------------------------
    private String netAddress = "";              //发送到的地址
    private int PORT = 0;                        //发送到的端口
	private DatagramSocket datagramSocket;       //数据报套接字

	public AClock clock = new AClock();          //定时器
	
	int TIME = 100; //超时时间是100ms
	private int timeoutInterval = TIME;             //超时的时间
	private int timerStart = 0;					//计时器
	int realDataLength = 1000;                   //真实数据的长度
	private ExecutorService executor = Executors.newFixedThreadPool(5) ;  //线程池
	
	private int MAX_RECEIVE_ACK = realDataLength+32;         //最大接收ACK报文的字节长度
	private byte[] receACK = new byte[MAX_RECEIVE_ACK];     //接收到的ACK报文
	
	private slicer oneslicer;
	boolean isSendComplete=true;
	boolean isReceiveComplete=true;
	public int currentSEQ=-1;//已经发送过的最近的SEQ
	public int ACKedSEQ=-1;//已经确认受到过的最近的SEQ
	int EndSEQ=0;
	boolean isComplete = false;   //最后一个发送的包已经收到
	boolean oneslicerIsEnd = false;  //文件已经全部发送完毕
	
	int lastTimeoutSEQ = -1;    //上一次超时时候的序号
	
	//-------------------拥塞机制----------------------------
	private int lastByteSent = currentSEQ+1;             //（最后一个发送的字节） = 之前的NextSeqNum-1
	private int lastByteAcked =  ACKedSEQ;              //（最后一个被接收方确认的字节） = 之前的 SendBase-1
	private int congWin = MAX_RECEIVE_ACK;                 // = 1MSS(最大报文段长度)
	private int threshold = 640000;                         //阈值
	private int congestionState = 0;                        //0为慢启动，1位拥塞避免
	
	private int receWindow = threshold;
	
	//统计ack次数
	Map<Integer,Integer> ackMap = new HashMap<Integer,Integer>();
	
	//初始化
	public void InitializeTCPSender(DatagramSocket ds,String address,int port,String filename)
	{
		try {
			TCPSenderLog = new FileOutputStream("TCPSenderLog.txt");
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		
		datagramSocket = ds;
		netAddress = address;
		PORT = port;
		try {
			oneslicer = new slicer(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	public void StartSendData() {
		while(oneslicer.IsEnd()==false) {
			System.out.print("");
			if(isSendComplete) {
				isSendComplete=false;
				byte[] datapack=oneslicer.getPack(currentSEQ+1);
				//System.out.println("currentSEQ:"+(currentSEQ+1));
				currentSEQ=Formatter.getSEQ(datapack);
				
				
            	executor.submit(new Runnable(){public void run(){

            		/*
	        		try {
						//1毫秒发送一个数据包
	    				Thread.sleep(1);
	    			} catch (InterruptedException e) {
	    				e.printStackTrace();
	    			}
					*/
	        		SendData(datapack);
	        		isSendComplete = true;
        		}});
			}
    		if(isReceiveComplete)
    		{
    			isReceiveComplete = false;

    			executor.submit(new Runnable(){public void run(){
    				ReceiveACK();
    				isReceiveComplete=true;
    				}});
    			
    		}
    		if(clock.isOverTime)
    		{
    			WriteLog("发送超时!!");
    			//tcp超时重传
    			Timeout();
    		}
		}
		oneslicerIsEnd = true;
		EndSEQ = currentSEQ;
		while(!isComplete)
		{
			//等待最后一个包被接收到
			System.out.print("");
    		if(clock.isOverTime)
    		{
    			WriteLog("发送超时!!");
    			//tcp超时重传
    			Timeout();
    		}
    		
    		if(isReceiveComplete)
    		{
    			isReceiveComplete = false;

    			executor.submit(new Runnable(){public void run(){
    				ReceiveACK();
    				isReceiveComplete=true;
    				}});
    			
    		}
		}
	}
	public void Timeout()
	{
		//如果还是上次传的那个包加倍超时时间间隔
		if(lastTimeoutSEQ == ACKedSEQ)
		{
			timeoutInterval = timeoutInterval*2;
		}
		else
		{
			timeoutInterval = TIME;
		}
		//将该包重传
		RetransMission();
		
		//定时器重新开始
		try {
			clock.RestartClock(timeoutInterval);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		timerStart = 1;		
		
		//超时的拥塞避免机制
		threshold = congWin / 2;
		congWin = MAX_RECEIVE_ACK;
		congestionState = 0;						//状态转到慢启动	
	}
	//重新传一个序号的包的函数
	private void RetransMission()
	{
		try
		{
			 
			//从缓冲池中取出序号为SendBase/realDataLength，因为这里SendBase是代表第几个字节的包，赋值给data_haveseq
			int tempSEQ = ACKedSEQ;
			lastTimeoutSEQ = ACKedSEQ;
			byte[] data_haveseq = oneslicer.getPack(ACKedSEQ);
			//将报文发送到指定目的地
	        InetAddress add = InetAddress.getByName(netAddress);
	        DatagramPacket datagramPacket = new DatagramPacket(data_haveseq, data_haveseq.length, add, PORT);
	        datagramSocket.send(datagramPacket);	
	        //回退n步
			currentSEQ=tempSEQ;
		}
		catch (UnknownHostException e) 
		{
            e.printStackTrace();
        }  
		catch (IOException e) 
		{
            e.printStackTrace();
        } 	
	}
	public void SendData(byte[] data_original) {
		System.out.println("拥塞大小为:"+congWin);
		//System.out.println("字节相差"+((lastByteSent-lastByteAcked)*MAX_RECEIVE_ACK));
        if((lastByteSent-lastByteAcked)*MAX_RECEIVE_ACK > congWin || (lastByteSent-lastByteAcked)*MAX_RECEIVE_ACK > receWindow)
        {
        	//发送一个种特殊报文,blank1和blank2都为1
        	byte[] data = new byte[1000];
        	byte[] formatData = Formatter.getFormat(0,0,0,0,0,1,1,0,data);

			try {
				InetAddress add = InetAddress.getByName(netAddress);
				DatagramPacket datagramPacket = new DatagramPacket(formatData, formatData.length, add, PORT);
				datagramSocket.send(datagramPacket);
			}catch (IOException e) {
    			e.printStackTrace();
    		}
        }
        else
        {
    		if(timerStart==0) {
    			try {
    				clock.StartClock(timeoutInterval);
    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			}
    			timerStart=1;
    		}
    		try {
    			InetAddress add = InetAddress.getByName(netAddress);
    			DatagramPacket datagramPacket = new DatagramPacket(data_original, data_original.length, add, PORT);
    	        datagramSocket.send(datagramPacket);
    	        int sendSEQ = Formatter.getSEQ(data_original);
    	        WriteLog("send a message is SEQ:" + sendSEQ);
    		}
    		catch (UnknownHostException e) 
    		{
                e.printStackTrace();
            }  
    		catch (IOException e) 
    		{
                e.printStackTrace();
            } 
    		lastByteSent = currentSEQ+1;        
    		lastByteAcked =  ACKedSEQ;    
        }
	}
	public void ReceiveACK() {
		int ack=-1;
		try {
			DatagramPacket receiveACK=new DatagramPacket(receACK, receACK.length);
			datagramSocket.receive(receiveACK);
			
			ack = GetAckNumber(receiveACK.getData());
			WriteLog("Before "+(ack)+" Is Confirmed ,currentSeq is"+currentSEQ);
			//oneslicer.senderBuffer.ReleasePacket(ack);
			receWindow = Formatter.getReceiveWindow(receiveACK.getData());
			
	        //正常收到ACK的拥塞避免机制
	        if(congestionState == 0)
	        {
		        congWin = congWin + MAX_RECEIVE_ACK;
		        if(congWin > threshold)
		        {
		        	congestionState = 1;    //切换到拥塞避免状态
		        }	        	
	        }
	        else
	        {
	        	congWin = congWin + MAX_RECEIVE_ACK * MAX_RECEIVE_ACK / congWin;
	        }
	        
			//所有包已经发完，等待接收最后一个包的ack
			if(oneslicerIsEnd && ack == EndSEQ+1)
			{
				isComplete = true;
				clock.CloseClock();
			}
			
			if(ack>ACKedSEQ) { //这里代表y之前的所有字节都已经收到
				ACKedSEQ=ack;
			}else {
	        	//y <= SendBase的情况
	        	//收到ACK y值的次数++;可以使用map来保存对应ack值和ack的次数
	        	if(ackMap.containsKey(ack))
	        	{
	        		int temp = ackMap.get(ack) + 1;
	        		ackMap.put(ack, temp);
	        		if(temp >= 3)                       //就以为跟在序号y后面的报文段以及丢失
	        		{
		        		RetransMission(); 
		        		 //收到3个相同ACK的拥塞避免机制
		        		threshold = congWin / 2;
						congWin = threshold;
						congestionState = 1;   //状态转到拥塞避免
		        		ackMap.put(ack, 0);
		        		System.out.println("三个冗余的ack出现了");
	        		}
	        	}
	        	else
	        	{
	        		ackMap.put(ack, 1);
	        	}	
			}
			
    		lastByteSent = currentSEQ+1;        
    		lastByteAcked =  ACKedSEQ; 
		}
		catch (IOException e) 
		{
            e.printStackTrace();
        } 	
	}
	private int GetAckNumber(byte[] ack)
	{
		int acknumber=Formatter.getACK(ack);//packetBuffer.byteArrayToInt(ack, 0);//!!!!特别注意 ，bug预定
		return acknumber;

	}
	
	public void WriteLog(String toWrite) {
		//log.println(toWrite+'\n');
		try {
			TCPSenderLog.write(toWrite.getBytes("utf-8"));
			TCPSenderLog.write("\n".getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//TCPLog.write("\n".getBytes("UTF-8"));
	}
}
