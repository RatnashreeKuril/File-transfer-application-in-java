import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
class FileUploadEvent
{
private String uploaderId;
private File file;
private long numberOfBytesUploaded;
FileUploadEvent()
{
this.uploaderId=null;
this.file=null;
numberOfBytesUploaded=0;
}
public void setUploaderId(String uploaderId)
{
this.uploaderId=uploaderId;
}
public String getUploaderId()
{
return this.uploaderId;
}
public void setFile(File file)
{
this.file=file;
}
public File getFile()
{
return this.file;
}
public void setNumberOfBytesUploaded(long numberOfBytesUploaded)
{
this.numberOfBytesUploaded=numberOfBytesUploaded;
}
public long getNumberOfBytesUploaded()
{
return this.numberOfBytesUploaded;
}
}
interface FileUploadListener
{
public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent);
}
class FileModel extends AbstractTableModel
{
private ArrayList<File> files;
FileModel()
{
this.files=new ArrayList<>();
}
public int getRowCount()
{
return this.files.size();
}
public int getColumnCount()
{
return 2;
}
public String getColumnName(int column)
{
if(column==0) return "S.No.";
return "File";
}
public Class getColumnClass(int column)
{
if(column==0) return Integer.class;
return String.class;
}
public Object getValueAt(int row, int column)
{
if(column==0) return (row+1);
return this.files.get(row).getAbsolutePath();
}
public boolean isCellEditable(int row, int column)
{
return false;
}
public void add(File file)
{
this.files.add(file);
fireTableDataChanged();
}
public ArrayList<File> getFiles()
{
return files;
}
public boolean exists(File file)
{
for(File f:this.files)
{
if(f.getAbsolutePath().equalsIgnoreCase(file.getAbsolutePath())) return true;
}
return false;
}
}

class FTClientFrame extends JFrame
{
private String host;
private int portNumber;
private FileSelectionPanel fileSelectionPanel;
private FileUploadViewPanel fileUploadViewPanel;
private Container container;

FTClientFrame(String host, int portNumber)
{
this.host=host;
this.portNumber=portNumber;
fileSelectionPanel=new FileSelectionPanel();
fileUploadViewPanel=new FileUploadViewPanel();
container=getContentPane();
container.setLayout(new GridLayout(1,2));
container.add(fileSelectionPanel);
container.add(fileUploadViewPanel);
int x,y,width,height;
width=800;
height=600;
Dimension d=Toolkit.getDefaultToolkit().getScreenSize();
x=(d.width/2)-(width/2);
y=(d.height/2)-(height/2);
setSize(width,height);
setLocation(x,y);
setVisible(true);
}
class FileSelectionPanel extends JPanel implements ActionListener
{
private JLabel titleLabel;
private FileModel model;
private JTable table;
private JScrollPane jsp;
private JButton addFileButton;
FileSelectionPanel()
{
setLayout(new BorderLayout());
titleLabel=new JLabel("Selected Files");
model=new FileModel();
table=new JTable(model);
jsp=new JScrollPane(table,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
addFileButton=new JButton(new ImageIcon("c:"+File.separator+"icons"+File.separator+"add_icon.png"));
addFileButton.addActionListener(this);
add(titleLabel,BorderLayout.NORTH);
add(jsp,BorderLayout.CENTER);
add(addFileButton,BorderLayout.SOUTH);
}
public void actionPerformed(ActionEvent ev)
{
JFileChooser jfc=new JFileChooser();
jfc.setCurrentDirectory(new File("."));
int selectedOption=jfc.showOpenDialog(this);
if(selectedOption==jfc.APPROVE_OPTION)
{
File selectedFile=jfc.getSelectedFile();
// check if file already selected for uploading (Assignment)
if(model.exists(selectedFile)) 
{
JOptionPane.showMessageDialog(this,"Files already exists");
return;
}
model.add(selectedFile);
}
}
public ArrayList<File> getFiles()
{
return model.getFiles();
}
}// File selection panel ends
class FileUploadViewPanel extends JPanel implements ActionListener,FileUploadListener
{
private JButton uploadFilesButton;
private JPanel progressPanelsContainer;
private JScrollPane jsp;
private ArrayList<ProgressPanel> progressPanels;
private ArrayList<File> files;
private ArrayList<FileUploadThread> fileUploaders;
FileUploadViewPanel()
{
fileUploaders=new ArrayList<>();
uploadFilesButton=new JButton("Upload Files");
setLayout(new BorderLayout());
add(uploadFilesButton,BorderLayout.NORTH);
uploadFilesButton.addActionListener(this);
}
public void actionPerformed(ActionEvent ev)
{
files=fileSelectionPanel.getFiles();
if(files.size()==0)
{
JOptionPane.showMessageDialog(FTClientFrame.this,"No file is selected to upload");
return;
}
int rowCount=files.size();
progressPanelsContainer=new JPanel();
progressPanels=new ArrayList<>();
progressPanelsContainer.setLayout(new GridLayout(rowCount,1));
ProgressPanel pp;
FileUploadThread fileUploadThread;
String uploaderId;
for(File file:files)
{
uploaderId=UUID.randomUUID().toString();
pp=new ProgressPanel(file,uploaderId);
progressPanels.add(pp);
progressPanelsContainer.add(pp);
fileUploadThread=new FileUploadThread(this,uploaderId,file,host,portNumber);
fileUploaders.add(fileUploadThread);
}
jsp=new JScrollPane(progressPanelsContainer,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
add(jsp,BorderLayout.CENTER);
this.revalidate();
this.repaint();
for(FileUploadThread fut:fileUploaders)
{
fut.start();
}


}
public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent)
{
String uploaderId=fileUploadEvent.getUploaderId();
long numberOfBytesUploaded=fileUploadEvent.getNumberOfBytesUploaded();
File file=fileUploadEvent.getFile();
for(ProgressPanel progressPanel:progressPanels)
{
if(progressPanel.getId().equals(uploaderId))
{
progressPanel.updateProgressBar(numberOfBytesUploaded);
break;
}
}

}
class ProgressPanel extends JPanel
{
private File file;
private JLabel fileNameLabel;
private JProgressBar progressBar;
private String id;
private long fileLength;
public ProgressPanel(File file, String id)
{
this.id=id;
this.file=file;
this.fileLength=file.length();
this.fileNameLabel=new JLabel("Uploading :"+file.getAbsolutePath());
this.progressBar=new JProgressBar(1,100);
setLayout(new GridLayout(2,1));
add(fileNameLabel);
add(progressBar);
}
public String getId()
{
return this.id;
}
public void updateProgressBar(long bytesCount)
{
int percentage=0;
if(percentage==fileLength) percentage=100;
else percentage=(int)((bytesCount*100)/fileLength);
progressBar.setValue(percentage);
if(percentage==100)
{
this.fileNameLabel.setText("Uploaded :"+file.getAbsolutePath());

}
}
}// Progress panel ends



} // File selection panel ends
public static void main(String gg[])
{
FTClientFrame fcf=new FTClientFrame("localhost",5500);
}

}

class FileUploadThread extends Thread
{
private FileUploadListener fileUploadListener;
private String id;
private File file;
private String host;
private int portNumber;
FileUploadThread(FileUploadListener fileUploadListener, String id, File file, String host, int portNumber)
{
this.fileUploadListener=fileUploadListener;
this.id=id;
this.file=file;
this.host=host;
this.portNumber=portNumber;
}
public void run()
{
try
{
long lengthOfFile=file.length();
String name=file.getName();
byte header[]=new byte[1024];
long x;
int i,j;
long k=lengthOfFile;
i=0;
while(k>0)
{
header[i]=(byte)(k%10);
k=k/10;
i++;
}
header[i]=(byte)',';
i++;
x=name.length();
j=0;
while(j<x)
{
header[i]=(byte)name.charAt(j);
i++;
j++;
}
while(i<=1023)
{
header[i]=(byte)(32);
i++;
}

Socket socket;
OutputStream os;
InputStream is;
socket=new Socket(host,portNumber);
os=socket.getOutputStream();
is=socket.getInputStream();
os.write(header,0,1024);
os.flush();
int bytesReadCount;
byte ack[]=new byte[1];
while(true)
{
bytesReadCount=is.read(ack);
if(bytesReadCount==-1) continue;
break;
}
x=0;
FileInputStream fis=new FileInputStream(file);
int chunkSize=4096;
byte bytes[]=new byte[chunkSize];
k=0;
while(k<lengthOfFile)
{
bytesReadCount=fis.read(bytes);
os.write(bytes,0,bytesReadCount);
os.flush();
k=k+bytesReadCount;
long brc=k;
SwingUtilities.invokeLater(()->{
FileUploadEvent fue=new FileUploadEvent();
fue.setFile(file);
fue.setUploaderId(id);
fue.setNumberOfBytesUploaded(brc);
fileUploadListener.fileUploadStatusChanged(fue);
});
}
while(true)
{
bytesReadCount=is.read(ack);
if(bytesReadCount==-1) continue;
break;
}
fis.close();
socket.close();
}catch(Exception e)
{
System.out.println(e);
}
}
}