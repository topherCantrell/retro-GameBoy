CON
  _clkmode        = xtal1 + pll16x
  _xinfreq        = 5_000_000
                                   
CON

  '               CTR      ADDRESS        DATA
  MASK_CE = %0000_0100_0000000000000000_00000000
  MASK_OE = %0000_0010_0000000000000000_00000000    
  MASK_WE = %0000_0001_0000000000000000_00000000
                         
OBJ

    PST      : "Parallax Serial Terminal"
    
PRI PauseMSec(Duration)
  waitcnt(((clkfreq / 1_000 * Duration - 3932) #> 381) + cnt)

var

  long cursor
  byte combuf[1024]

  byte inMem[1024*16]
      
PUB Main | x, a, c, o

  outa := MASK_OE | MASK_CE | MASK_WE
  dira := $0F_FFFF_00

  'PauseMSec(2000)
  PST.Start(115200)

  ' Cxxxxxxxx       Set the cursor to the given address
  ' Waabbccddeeff   Write bytes and increment cursor
  ' R               Read 16 bytes
  ' Xxxxxxxxx       Checksum the x bytes starting at cursor
  ' Fvvxxxxxxxx     Fill x bytes starting at cursor with vv

  repeat                              
    PST.strIn(@combuf)

    case combuf[0]
      "C":
        cursor := 0
        x := 1
        repeat while (combuf[x] <> 0)
          cursor := cursor * 16
          cursor := cursor + fromHex(combuf[x])
          x := x + 1
        PST.str(@combuf)
        PST.char(" ")
        PST.hex(cursor,8)
        PST.char(13)
      
      "W":
        x := 1
        c := 0
        o := cursor
        repeat while (combuf[x] <> 0) and (combuf[x+1] <> 0)
          a := fromHex(combuf[x])*16 + fromHex(combuf[x+1])
          x := x + 2
          write(cursor,a)
          c := c + a
          c := c & $FFFF
          cursor := cursor + 1
        PST.str(@combuf)
        PST.char(" ")
        PST.hex(o,8)
        PST.char(" ")
        PST.hex(c,4)
        PST.char(13)
        
      "R":
        o := cursor
        x := 1
        PST.str(@combuf)
        PST.char(" ")
        PST.hex(o,8)
        PST.char(" ")
        repeat x from 0 to 15
          a := read(cursor)
          cursor := cursor + 1
          PST.hex(a,2)
        PST.char(13)

      "X":
        o := cursor
        x := 1
        a := 0
        repeat while (combuf[x] <> 0)
          a := a * 16
          a := a + fromHex(combuf[x])
          x := x + 1
        c := 0
        repeat while a>0
          c:=c+read(cursor)
          c:=c&$FFFF
          cursor:=cursor+1
          a:=a-1
        PST.str(@combuf)
        PST.char(" ")
        PST.hex(o,8)
        PST.char(" ")
        PST.hex(c,4)
        PST.char(13)

      "F":
        o := cursor
        c := fromHex(combuf[1])*16+fromHex(combuf[2])
        x := 3
        repeat while (combuf[x] <> 0)
          a := a * 16
          a := a + fromHex(combuf[x])
          x := x + 1
        repeat while a>0
          write(cursor,c)
          cursor:=cursor+1
          a:=a-1
        PST.str(@combuf)
        PST.char(" ")
        PST.hex(o,8)        
        PST.char(13)

      OTHER:
        PST.str(@combuf)
        PST.char(" ")
        PST.char("?")
        PST.char(13)
  
PUB fromHex(a)
  if a=>"A" and a=<"F"
    return 10 + a - "A"

  if a=>"a" and a=<"f"
    return 10 + a - "a"

  return a - "0"

PUB writeRAM(address,value)
  address := address & $3FFF
  inMem[address] := value

PUB readRAM(address)
  address := address & $3FFF
  return inMem[address]

PUB write(address,value)

{{
  - CE=1, OE=1, WR=0
  - CE=1, OE=1, WR=0 ? no need
  - Set output data
  - Set pins to output
  - CE=0, OE=1, WR=0
  - CE=1, OE=1, WR=0
  - CE=1, OE=1, WR=0 ? no need
  - CE=1, OE=1, WR=1
  - Set pins to input   
}}

  address := address & $FFFFF       ' Limit address to 16 bits
  value := value & $FF             ' Limit data to 8 bits    
  address := address<<8            ' Address into position
  address := address | value       ' Add in value

  ' Starts here with:
  ' - WR, CE, OE = 1 (deactivated)
  ' - data direction = input      

  outa := address | MASK_CE | MASK_OE            ' CE=1, OE=1, WR=0
  'PauseMSec(1)
  dira := $0F_FFFF_FF                            ' Data bus is output
  'PauseMSec(1)
  outa := address           | MASK_OE            ' CE=0, OE=1, WR=0
  'PauseMSec(1)  
  outa := address | MASK_CE | MASK_OE            ' CE=1, OE=1, WR=0
  'PauseMSec(1)    
  outa := address | MASK_CE | MASK_OE | MASK_WE  ' Everything off
  'PauseMSec(1)
  dira := $0F_FFFF_00                            ' Databus is input (safest way to leave it)
  'PauseMSec(1)     

PUB read(address) | r

{{
  - Make all pins inputs ? should stay that way
  - CE=1, OE=1, WR=1
  - CE=0, OE=1, WR=1
  - CE=0, OE=0, WR=1
  - Wait 70ns
  - Read the data
  - CE=1, OE=0, WR=1                                         `
  - CE=1, OE=1, WR=1
  - CE=1, OE=1, WR=1 ? no needed
}}

  address := address & $FFFFF       ' Limit address to 16 bits
  address := address<<8            ' Into correct pins

  ' Starts here with:
  ' - WR, CE, OE = 1 (deactivated)
  ' - data direction = input 

  ' Just to make sure .. no shorts
  outa := address | MASK_CE | MASK_OE | MASK_WE  ' Everything off
  'PauseMSec(1)
  dira := $0F_FFFF_00                  ' Databus is input (just to make sure)
  'PauseMSec(1)
  
  outa := address           | MASK_OE | MASK_WE ' CE=0, OE=1, WR=1
  'PauseMSec(1)
  outa := address                     | MASK_WE ' CE=0, OE=0, WR=1   
  'PauseMSec(1)
  r := ina                         ' Read the value
  r := r & $FF                     ' Just the data
  'PauseMSec(1)
  outa := address | MASK_CE           | MASK_WE ' CE=1, OE=0, WR=1
  'PauseMSec(1)
  outa := address | MASK_CE | MASK_OE | MASK_WE ' CE=1, OE=1, WR=1
  'PauseMSec(1)
    
  return r

{{

From RomLoader.c

unsigned char FRAMRead ( void )
{
        unsigned char readData = 0;
        DAT_PORT = 0x00;
        //set_bit(status, RP0);                 //Goto bank 1   
        DAT_TRIS = 0xFF;                                //Make data port all inputs
        //clear_bit(status, RP0);                       //Goto bank 0   
        
        DAT_PORT = 0x00;
        
        set_bit( PORT_WR, BIT_WR );             //Set device to read mode
        clear_bit( PORT_CE, BIT_CE );   //Set CE low to latch address
        clear_bit( PORT_OE, BIT_OE );   //Set DQ to drive when data is valid
        
        //70ns later data is presented on the port... since were are running with a slow clock data is now read.
        _asm
        { 
                nop 
                nop 
                nop 
                nop 
        }
        readData = DAT_PORT;
        _asm
        { 
                nop 
                nop 
                nop 
                nop 
        }
        set_bit( PORT_CE, BIT_CE );             //Disable chip
        set_bit( PORT_OE, BIT_OE );             //Disable outputs
        set_bit( PORT_WR, BIT_WR );             //Set device to read mode
        return readData;
}

void FRAMWrite ( char data )
{
        clear_bit( PORT_WR, BIT_WR );   //Set device to write mode
        set_bit( PORT_OE, BIT_OE );             //Set DQ to inputs
        
        DAT_PORT = data;                                //Set data port to data to write        

        //set_bit(status, RP0);                 //Goto bank 1   
        DAT_TRIS = 0x00;                                //Make data port all outputs
        //clear_bit(status, RP0);                       //Goto bank 0   
        
        clear_bit( PORT_CE, BIT_CE );   //Set CE low to latch address and data
        
        set_bit( PORT_CE, BIT_CE );             //Disable chip
        set_bit( PORT_OE, BIT_OE );             //Disable outputs
        set_bit( PORT_WR, BIT_WR );             //Set device to read mode
        
        //set_bit(status, RP0);                 //Goto bank 1   
        DAT_TRIS = 0xFF;                                //Make data port all inputs
        //clear_bit(status, RP0);                       //Goto bank 0   
}



}}