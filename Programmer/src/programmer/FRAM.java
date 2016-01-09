package programmer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * Control the FRAM programmer through USB PropStick
 * 
 * This class coordinates communication with the FRAM programmer board. The board
 * understands several serial commands. This class speaks that protocol.
 */
public class FRAM {
    
    SerialPort port;
    private static final boolean DEBUG = true;
        
    /**
     * This creates an open connection to the FRAM programmer board
     * @param portName local COM port name
     * @throws SerialPortException
     */
    public FRAM(String portName) throws SerialPortException {
        
        String[] portNames = SerialPortList.getPortNames();
        System.out.println("Available ports:"+Arrays.toString(portNames));
        
        port = new SerialPort(portName);
        port.openPort();
        port.setParams(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        
        port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        
    }
    
    /**
     * Set the programmer's data cursor
     * @param address the cursor address
     * @throws SerialPortException
     */
    public void setCursor(int address) throws SerialPortException {     
        // "Cxxxxxxxx"
        // returns "Cxxxxxxxx xxxxxxxx"
        String addr = Integer.toString(address,16).toUpperCase();
        while(addr.length()<8) addr = "0"+addr;
        String command = "C"+addr;
        String ret = sendCommand(command);
        if(!ret.equals(command+" "+addr)) {
            throw new RuntimeException("Bad return:"+ret+":");
        }        
    }
    
    /**
     * Write the given data to the FRAM at the current data cursor
     * @param data the data to write
     * @throws SerialPortException
     */
    public void write(int [] data) throws SerialPortException { 
        // "Waabbccddeeff"
        // returns "Waabbccddeeff xxxxxxxx cccc"
        int pos = 0;
        while(pos<data.length) {
            int toSend = data.length - pos;
            if(toSend>16) toSend=16;
            String command = "W";
            for(int x=0;x<toSend;++x) {
                String d = Integer.toString(data[pos+x],16).toUpperCase();
                while(d.length()<2) d="0"+d;
                command = command + d;
            }
            String ret = sendCommand(command);
            System.out.println(":"+ret+":");
            // TODO check
            pos = pos + toSend;
        }        
    }
    
    /**
     * Read bytes from the FRAM from the current data cursor
     * @param size number of bytes to read
     * @return the bytes read
     * @throws SerialPortException
     */
    public int[] read(int size) throws SerialPortException {
        
        int[] ret = new int[size];        
        int pos = 0;
        while(pos<ret.length) {
            int [] chunk = read16();
            int toCopy = ret.length - pos;
            if(toCopy>16) toCopy=16;
            for(int x=0;x<toCopy;++x) {
                ret[pos+x] = chunk[x];
            }
            pos = pos + toCopy;            
        }        
        return ret;
    }
    
    // The programmer board reads 16 bytes at a time
    private int[] read16() throws SerialPortException {
        // "R"
        // returns "xxxxxxxx aabbccddeeff..."
        String command = "R";
        String ret = sendCommand(command);
        //System.out.println(":"+ret+":");
        // TODO check
        int [] data = new int[16];
        int pos = 9;
        for(int x=0;x<16;++x) {
            data[x] = Integer.parseInt(ret.substring(pos+x*2, pos+x*2+2),16);
        }
        return data;
    }
        
    /**
     * Fill an area of the FRAM with the given value
     * @param address the starting address
     * @param size the number of bytes to fill
     * @param value the fill value
     * @throws SerialPortException
     */
    public void fill(int address, int size, int value) throws SerialPortException {
        setCursor(address);
        // "Fvvssssss"
        // returns "xxxxxx cccc"        
        // TODO
    }
    
    /**
     * Get a two-byte checksum of the requested FRAM area
     * @param address starting address
     * @param size number of bytes to check
     * @return the checksum
     * @throws SerialPortException
     */
    public int getChecksum(int address, int size) throws SerialPortException {        
        setCursor(address);
        // "Xssssss"
        // returns "xxxxxx cccc"
        // TODO
        return 0;
    }
    
    /**
     * Read bytes from the FRAM
     * @param address the starting address
     * @param size the number of bytes to read
     * @return the FRAM data
     * @throws SerialPortException
     */
    public int[] read(int address, int size) throws SerialPortException {
        setCursor(address);
        return read(size);
    }
    
    /**
     * Write bytes to the FRAM
     * @param address the starting address
     * @param data the bytes to write
     * @throws SerialPortException
     */
    public void write(int address, int [] data) throws SerialPortException {
        setCursor(address);
        write(data);        
    }
    
    /**
     * Send a command to the FRAM and return the response
     * @param command the command to send
     * @return the response line
     * @throws SerialPortException
     */
    public String sendCommand(String command) throws SerialPortException {
        if(DEBUG) System.out.println("SEND:"+command+":");
        port.writeString(command+"\r");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        String incoming = "";
        while(true) {            
            String g = port.readString();
            if(g!=null) {
                incoming = incoming + g;
                int i = incoming.indexOf("\r");
                if(i>=0) {
                    if(DEBUG) System.out.println("RECV:"+incoming.substring(0,i)+":");
                    return incoming.substring(0,i);
                }
            }            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public static void main(String [] args) throws Exception
    {
        
        // Super-simple file transfer
        
        // java programmer.FRAM toFRAM filename
        // java programmer.FRAM fromFRAM filename
        
        FRAM fram = new FRAM("COM3");
        
        if(args.length>0 && args[0].equals("fromFRAM")) {
            
            int [] data = fram.read(0,32*1024);
            OutputStream os = new FileOutputStream(args[1]);
            for(int x=0;x<data.length;++x) {
                os.write(data[x]);
            }
            os.flush();
            os.close();
            
            System.out.println("Copied FRAM to file");
            
        } else if(args.length>0 && args[0].equals("toFRAM")) {
            
            int chk = 0;
            
            InputStream is = new FileInputStream(args[1]);
            int [] data = new int[is.available()];
            for(int x=0;x<data.length;++x) {
                data[x] = is.read();
                chk = chk + data[x];
                chk = chk & 0xFFFF;
            }
            is.close();          
            
            System.out.println("Checksum: "+Integer.toHexString(chk));
            fram.write(0,data);
            
            System.out.println("Copied file to FRAM");
            
        } else {
            throw new Exception("Invalid operation '"+args[0]+"'");
        }
        
    }

}
