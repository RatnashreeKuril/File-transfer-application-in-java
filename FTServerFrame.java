import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
class RequestProcessor extends Thread
{
private Socket socket;
private String id;
private FTServerFrame fsf;
RequestProcessor(Socket socket,String id,FTServerFrame fsf)
{
this.id=id;
this.socket=socket;
this.fsf=fsf;
start();
}
public void run()
{
try
{
SwingUtilities.invokeLater(new Runnable(){
public void run()
{
fsf.updateLog("Client connected and id alloted is : "+id);
}
});
InputStream is=socket.getInputStream();
OutputStream os=socket.getOutputStream();
byte header[]=new byte[1024];
byte []tmp=new byte[1024];
int x=0;
int y;
int i=0;
int bytesReadCount,j;
long e;
while(x<1024)
{
bytesReadCount=is.read(tmp);
if(bytesReadCount==-1) continue;
for(y=0;y<bytesReadCount;y++)
{
header[i]=tmp[y];
i++;
}
x=x+bytesReadCount;
}


long lengthOfFile=0;
i=0;
e=1;
while(header[i]!=',')
{
lengthOfFile=lengthOfFile+(header[i]*e);
e=e*10;
i++;
}
i++;
StringBuffer sb=new StringBuffer();
while(i<=1023)
{
sb.append((char)(header[i]));
i++;
}
String fileName=sb.toString().trim();
long lof=lengthOfFile;
SwingUtilities.invokeLater(()->{
fsf.updateLog("Receiving file : "+fileName+" of length : "+lof);
});
File file=new File("uploads"+File.separator+fileName);
if(file.exists()) file.delete();
FileOutputStream fos=new FileOutputStream(file);
byte ack[]=new byte[1];
ack[0]=1;
os.write(ack,0,1);
os.flush();
int chunkSize=4096;
byte bytes[]=new byte[chunkSize];
i=0;
long m;
m=0;
while(m<lengthOfFile)
{
bytesReadCount=is.read(bytes);
if(bytesReadCount==-1) continue;
fos.write(bytes,0,bytesReadCount);
fos.flush();
m=m+bytesReadCount;
int k=bytesReadCount;
}
fos.close();
ack[0]=1;
os.write(ack,0,1);
os.flush();
socket.close();
SwingUtilities.invokeLater(()->{
fsf.updateLog("File saved to "+file.getAbsolutePath());
fsf.updateLog("Connection with client whose id is : "+id+" closed");
});

}catch(Exception e)
{
System.out.println(e);
}


}
}
class FTServer extends Thread
{
private ServerSocket serverSocket;
private FTServerFrame fsf;
FTServer(FTServerFrame fsf)
{
this.fsf=fsf;
}
public void run()
{
try
{
serverSocket=new ServerSocket(5500);
startListening();
}catch(Exception e)
{
System.out.println(e);
}
}
public void shutDown()
{
try
{
serverSocket.close();
}catch(Exception e)
{
System.out.println(e); // remove after testing
}
}
private void startListening()
{
try
{
RequestProcessor rp;
Socket socket;
while(true)
{
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fsf.updateLog("Server started and is listening on port 5500");
}
});
socket=serverSocket.accept();
rp=new RequestProcessor(socket,UUID.randomUUID().toString(),fsf);
}
}catch(Exception e)
{
System.out.println("Server stopped listening");
System.out.println(e);
}
}
}
class FTServerFrame extends JFrame implements ActionListener
{
private JButton button;
private JTextArea jta;
private Container container;
private FTServer ftServer;
private JScrollPane jsp;
private boolean serverState=false;
FTServerFrame()
{
button=new JButton("Start");
jta=new JTextArea();
jsp=new JScrollPane(jta,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
button.addActionListener(this);
container=getContentPane();
container.setLayout(new BorderLayout());
container.add(jsp,BorderLayout.CENTER);
container.add(button,BorderLayout.SOUTH);
setSize(500,500);
setLocation(100,100);
setVisible(true);
}
public void actionPerformed(ActionEvent ev)
{
if(serverState==false)
{
ftServer=new FTServer(this);
ftServer.start();
serverState=true;
button.setText("Stop");
}
else
{
ftServer.shutDown();
serverState=false;
button.setText("Start");
jta.append("Server stopped\n");
}
}
public void updateLog(String message)
{
jta.append(message+"\n");
}

public static void main(String gg[])
{
FTServerFrame serverFrame=new FTServerFrame();
}
}