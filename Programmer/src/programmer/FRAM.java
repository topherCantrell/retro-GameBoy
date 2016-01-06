package programmer;

import jssc.SerialPort;
import jssc.SerialPortException;

public class FRAM {
    
    SerialPort port;
    
    public FRAM(String portName) throws SerialPortException {
        
        //String[] portNames = SerialPortList.getPortNames();
        //System.out.println(Arrays.toString(portNames));
        
        SerialPort port = new SerialPort(portName);
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
        // "Cxxxxxx"
        // returns "xxxxxx"
        String command = Integer.toString(address,16).toUpperCase();
        while(command.length()<6) command = "0"+command;
        command = "C"+command;
        String ret = sendCommand(command);
        System.out.println("::"+ret+"::");
    }
    
    public void write(int [] data) { 
        // "aabbccddeeff"
        // returns "xxxxxx aabbccddeeff cccc"
        // TODO break the data up into digestable chunks
    }
    
    public int[] read(int size) {
        // "R"
        // returns "xxxxxx aabbccddeeff..."
        // TODO always 16 bytes in at a time
        return null;
    }
    
    public void fill(int address, int value) throws SerialPortException {
        setCursor(address);
        // "Fvvssssss"
        // returns "xxxxxx cccc"        
        // TODO
    }
    
    public int getChecksum(int address, int size) throws SerialPortException {        
        setCursor(address);
        // "Xssssss"
        // returns "xxxxxx cccc"
        // TODO
        return 0;
    }
    
    //    
    
    public int[] read(int address, int size) throws SerialPortException {
        setCursor(address);
        return read(size);
    }
    
    public void write(int address, int [] data) throws SerialPortException {
        setCursor(address);
        write(data);        
    }
    
    public String sendCommand(String command) throws SerialPortException {
        port.writeString(command+"\r");
        return port.readString();
    }
    
    public static void main(String [] args) throws Exception
    {
        
        FRAM fram = new FRAM("COM4");
        fram.setCursor(0);
        
    }

}
