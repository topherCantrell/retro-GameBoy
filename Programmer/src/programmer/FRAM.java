package programmer;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public class FRAM {
    
    SerialPort port;
    static final boolean DEBUG = true;
        
    public FRAM(String portName) throws SerialPortException {
        
        //String[] portNames = SerialPortList.getPortNames();
        //System.out.println(Arrays.toString(portNames));
        
        port = new SerialPort(portName);
        port.openPort();
        port.setParams(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        
        port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        
        // Use like this
        
        /*
        port.addEventListener(new SerialPortEventListener() {

            @Override
            public void serialEvent(SerialPortEvent event) {
                try {                    
                    String data = port.readString(event.getEventValue());
                    System.out.println(data);
                    
                    
                } catch (SerialPortException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
            
        },SerialPort.MASK_RXCHAR);       
        
        while(true) {
            Thread.sleep(5000);            
            port.writeString("Hello\r");
            Thread.sleep(1000);
            String a = port.readString();
            System.out.println(":"+a+":");
            Thread.sleep(1000);
            a = port.readString();
            System.out.println(":"+a+":");
        }
        
         */
        
    }
    
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
    
    public int[] read16() throws SerialPortException {
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
    
    public void fill(int address, int value) throws SerialPortException, SerialPortTimeoutException {
        setCursor(address);
        // "Fvvssssss"
        // returns "xxxxxx cccc"        
        // TODO
    }
    
    public int getChecksum(int address, int size) throws SerialPortException, SerialPortTimeoutException {        
        setCursor(address);
        // "Xssssss"
        // returns "xxxxxx cccc"
        // TODO
        return 0;
    }
    
    //    
    
    public int[] read(int address, int size) throws SerialPortException, SerialPortTimeoutException {
        setCursor(address);
        return read(size);
    }
    
    public void write(int address, int [] data) throws SerialPortException, SerialPortTimeoutException {
        setCursor(address);
        write(data);        
    }
    
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
        
        Thread.sleep(5000);
        FRAM fram = new FRAM("COM3");
        //int [] data = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
        //fram.write(0x00,data);
        //fram.read(0x00,100);    
        //fram.read(0x00,100); 
        fram.read(0x00,100); 
        Thread.sleep(5000);
        
    }

}
