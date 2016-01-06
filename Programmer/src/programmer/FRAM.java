package programmer;

import java.util.Arrays;

import jssc.SerialPort;
import jssc.SerialPortList;

public class FRAM {
    
    public static void main(String [] args) throws Exception
    {
        
        String[] portNames = SerialPortList.getPortNames();
        System.out.println(Arrays.toString(portNames));
        
        SerialPort port = new SerialPort("COM4");
        port.openPort();
        port.setParams(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        
        port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        
        
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
        */
        
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
        
    }

}
