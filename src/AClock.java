import java.util.Timer;
import java.util.TimerTask;
//定时器
public class AClock 
{

	private int CurMillisecond;
	
	public boolean isOverTime = false;
	Timer timer;
	//启动定时器按照毫秒计算
	public void StartClock(int limitSec) throws InterruptedException
	{
		this.CurMillisecond = limitSec;
		System.out.println("count down from "+limitSec+" ms ");
		timer = new Timer();
		timer.schedule(new TimerTask(){
			public void run(){
				//玄学需要加下面打印语句才不会阻塞
				System.out.print("");
				--CurMillisecond;
				if(CurMillisecond == 0)
				{
					isOverTime = true;
					System.out.println("Time is out!");
					timer.cancel();
				}
			}
		},0,1);
		
	}
	//启动针对三次握手定时器
	public void StartClockForLink(int limitSec,byte[] data,UDPClient client,UDPServer server) throws InterruptedException
	{
		this.CurMillisecond = limitSec;
		System.out.println("SYN或FIN报文定时 "+limitSec+" ms ");
		timer = new Timer();
		timer.schedule(new TimerTask(){
			public void run(){
				//玄学需要加下面打印语句才不会阻塞
				System.out.print("");
				--CurMillisecond;
				if(CurMillisecond == 0)
				{
					isOverTime = true;
					System.out.println("SYN或FIN报文定时时间到，重新发送报文");
					timer.cancel();
					//重新发送报文
					if(client != null)
						client.SendData(data);
					else
						server.SendData(data);
					try {
						//重新启动定时器
						StartClockForLink(limitSec,data,client,server);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		},0,1);
		
	}
	//取消定时器
	public void CloseClock()
	{
		isOverTime = false;
		timer.cancel();
	}
	//重启定时器
	public void RestartClock(int limitSec) throws InterruptedException
	{
		isOverTime = false;
		timer.cancel();
		StartClock(limitSec);
	}
	

	//启动定时器按照毫秒计算
	public void StartFINClock(int limitSec,TCPReceiver rece,UDPServer ser,int port,UDPClient cli) throws InterruptedException
	{
		CurMillisecond = limitSec;
		System.out.println("count down from "+limitSec+" ms ");
		timer = new Timer();
		timer.schedule(new TimerTask(){
			public void run(){
				//玄学需要加下面打印语句才不会阻塞
				System.out.print("");
				--CurMillisecond;
				isOverTime = false;
				if(CurMillisecond == 0)
				{
					System.out.println("挥手超时直接关闭套接字");
					if(rece != null)
					{
						rece.Close();
					}
					else if(ser != null)
					{
						ser.BreakFromClient(port);
					}
					else if(cli != null)
					{
						cli.CloseSocket();
					}
					timer.cancel();
				}
			}
		},0,1);
		
	}
}
