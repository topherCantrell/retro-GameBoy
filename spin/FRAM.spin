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
      
PUB Main | x, a, c, o

  outa := MASK_OE | MASK_CE | MASK_WE
  dira := $0F_FFFF_00

  PauseMSec(2000)
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

PUB write(address,value)

  address := address & $FFFF       ' Limit address to 16 bits
  value := value & $FF             ' Limit data to 8 bits    
  address := address<<8            ' Address into position
  address := address | value       ' Add in value
    
  dira := $0F_FFFF_FF              ' Data bus is output
  
  outa := address | MASK_OE | MASK_CE | MASK_WE  ' Everything off 
  PauseMSec(10)
  
  outa := address | MASK_OE           | MASK_WE  ' Latch in the address
  PauseMSec(10)

  outa := address | MASK_OE                      ' Strobe the write
  PauseMSec(10)

  outa := address | MASK_OE           | MASK_WE  ' Release the write 
  PauseMSec(10)  

  outa := address | MASK_OE | MASK_CE | MASK_WE  ' Everything off
  dira := $0F_FFFF_00            ' Databus is input (safest way to leave it)    
  PauseMSec(10)

PUB read(address) | r

  address := address & $FFFF       ' Limit address to 16 bits
  address := address<<8            ' Into correct pins
  
  dira := $0F_FFFF_00              ' Databus is input
  
  outa := address | MASK_OE | MASK_CE | MASK_WE  ' Everything off 
  PauseMSec(10)
  
  outa := address | MASK_OE           | MASK_WE  ' Latch the address
  PauseMSec(10)

  outa := address                     | MASK_WE  ' Strobe the output
  PauseMSec(10)
  
  r := ina                         ' Read the value
  r := r & $FF                     ' Just the data

  outa := address | MASK_CE | MASK_OE | MASK_WE                              
  PauseMSec(10)

  return r    
  