

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class helloword {
    public static void main(String[] args) {
        //System.out.println("Hello World");
    //	byte[] test=TCP.SetAckPackage(23, 55);

    //	int test1=TCP.GetAckNumber(test);
    //	int test2=TCP.GetRecWindow(test);
   // 	System.out.println(test1+"dfsdf"+test2);
   

			slicer temp = null;
			try {
				temp = new slicer(new String("1.pdf"));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			packer packer=new packer("output.pdf");
			//packer packer1=new packer("outpu.txt");
			int count=0;
			while(temp.IsEnd()==false) {
				packer.pack(temp.getPack(count));
				count++;
			}
			//packer.pack(temp.getPack(0));
			//packer.pack(temp.getPack(1));
			//packer.pack(temp.getPack(2));
			//packer.pack(temp.getPack(3));
			temp.senderBuffer.ReleasePacket(10);
			temp.senderBuffer.Print();
			//packer.pack(temp.getPack(3));
			//packer1.pack(temp.getPack(0));
	
    }


}
