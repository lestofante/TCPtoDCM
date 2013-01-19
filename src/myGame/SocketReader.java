package myGame;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class SocketReader implements Runnable{
	
	//public static void main(String args[]){
		//main for test
		//SocketReader s = new SocketReader();
		//s.connect();
	//}

	Socket reader = new Socket();
	private boolean leggi;
	
	final DCMlogic dcm;
	
	public SocketReader(DCMlogic dcm){
		this.dcm = dcm;
	}
	
	public void connect(){
		System.out.println("Starto tutto");
		leggi = false;
		try {
			reader.connect(new InetSocketAddress("10.8.0.6", 2345),5000);
			leggi = true;
			Thread thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		} catch (IOException e) {
			System.out.println("Connection failed. DEBUG MODE");
			//connection failed fallback on debug mode
			Timer timer = new Timer(true);
			timer.scheduleAtFixedRate(new TimerTask() {
				
				@Override
				public void run() {
					float f = FastMath.sin(System.nanoTime()/500000000f)*2f;
					dcm.MadgwickAHRSupdate(f, f, 0, 0, 0, 1, 1, 0, 0);//bha, if you say so (i know, that what SHE said)
				}
			}, new Date(), 1);
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		InputStream inputStream;
		try {
			inputStream = reader.getInputStream();
			while (leggi){
				if (inputStream.available()>0){
					byte[] read = new byte[inputStream.available()];
					inputStream.read(read);
					analyze(read);
				} else {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				//il mio procio ringrazia
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	int offset = -1;
	private byte tipoSensore;
	private int val;
	Vector3f temp = new Vector3f();
	Vector3f magVec, accVec, gyroVec;
	int choosen=0;
	
	private synchronized void analyze(byte[] read) {
		if (offset==-1){
			System.out.println("Analizing: "+read.length);
			//we don't know where we are on the stream. find occurence of "A", "G" and "M" to find it
			int index = findOccurence(read, 0);
			System.out.println("Index: "+index);
			int tmpIndex = index;
			boolean ok = true;
			if (tmpIndex < 0){
				ok = false;
			}
			while (tmpIndex >= 0 && tmpIndex<read.length){
				if ( !(read[tmpIndex]=='A') && !(read[tmpIndex]=='G') && !(read[tmpIndex]=='M')){
					System.out.println("Error on index: "+index);
					ok = false;
					break;
				}
				tmpIndex+=7;
			}
			if (ok == true){
				System.out.println("Setting index: "+index);
				offset = index;
				analyze(read);
				return;
			}
		}else{
			for (int i=0; i < read.length; i++){
				//System.out.println("leggo dati"+offset);
				switch (offset) {
				case 0:
					tipoSensore = read[i];
					switch (tipoSensore) {
					case 'G':
						//selezionato = writerGyro;
						choosen=1;
						break;
					case 'A':
						//selezionato = writerAcce;
						choosen = 2;
						break;
					case 'M':
						//selezionato = writerAcce;
						choosen = 3;
						break;
					default:
						choosen=0;
						//selezionato = null;
						System.out.print("BHO!: ");
						offset=-2;
						//System.out.println("Index errato, ritento");
						i = read.length;//force exit
						break;
					}
					offset++;
					break;
				case 1:// MS x
					readMSbyte(read[i]);
					offset++;
					break;
				case 2:// LS x
					readLAbyte(read[i]);
					temp.x = val;
					//if(choosen!=null)
					//	if(choosen.equals(gyroQueue))
					//		temp.x += 26;
					//selezionato.print(val + " ");
					offset++;
					break;
				case 3:// MS y
					readMSbyte(read[i]);
					offset++;
					break;
				case 4:// LS y
					readLAbyte(read[i]);
					temp.y = val;
					//if(choosen!=null)
					//	if(choosen.equals(gyroQueue))
					//		temp.y += 7;
					//selezionato.print(val + " ");
					offset++;
					break;
				case 5:// MS z
					readMSbyte(read[i]);
					offset++;
					break;
				case 6:// MS z
					readLAbyte(read[i]);
					temp.z = val;					
					if (choosen != 0){
						//if(choosen.equals(gyroQueue))
						//	temp.z += 26;
						//choosen.add(temp);
						float swap;
						
						if (choosen==1){
							temp.z *= -1;
							
							swap = temp.y;
							temp.y = temp.x;
							temp.x = swap;
							
							gyroVec = temp;
						}
						
						if (choosen==2){//ACC
							swap = temp.y;
							temp.y = -temp.x;
							temp.x = -swap;
							
							accVec = temp;
						}
						
						if (choosen==3){
							temp.z *= -1;
							
							temp.x *= -1;
							
							magVec = temp;
						}
						choosen=0;
						temp = new Vector3f();
					}
					offset = 0;
					break;
				}
				//magVec=null;
				
				if (gyroVec!= null && accVec!=null && magVec!=null){
					System.out.println("distanza: "+accVec.subtract(magVec)+" dist: "+accVec.distance(magVec));
					dcm.MadgwickAHRSupdate(gyroVec.x, gyroVec.y, gyroVec.z, accVec.x, accVec.y, accVec.z, magVec.x, magVec.y, magVec.z);
					gyroVec = accVec = magVec = null;
					choosen = 0;
			
				}else if (gyroVec!= null && accVec!=null){
					//dcm.MadgwickAHRSupdate(gyroVec.x, gyroVec.y, gyroVec.z, accVec.x, accVec.y, accVec.z, 0, 0, 0);
					//gyroVec = accVec = null;
					//choosen = 0;
				}
			}
		}
		
	}
	
	private int findOccurence(byte[] read, int index) {
		for (int i=index;i<read.length;i++){
			System.out.print(read[i]+" ");
			if (read[i]=='A' || read[i]=='G' || read[i]=='M'){
				return i-index;
			}
			if (read[i]==65 || read[i]==71 || read[i]==77){
				System.out.println("Alternative find ok"); //charset problem? maybe 16bit vs 8 bit?
				return i-index;
			}
		}
		System.out.println();
		return -1;
	}
	
	private void readLAbyte(byte tmp) {
		// quindi il più significativo, che sarà già stato letto, spostalo a
		// sinistra
		val = val << 8;
		// copia in val i byte ad uno del byte meno significativo, completando
		// così il dato (or logico, per evitare casini con i signed/unsigned se
		// avessi usato +)
		val |= tmp & 0xff;
	}

	private void readMSbyte(byte tmp) {
		// azzera il valore precendente
		val = 0;
		// e metti il valore di tmp in val (or logico, per evitare casini con i
		// signed/unsigned se avessi usato +)
		val |= tmp;
	}
}
