# GameBoyColor-Development

I wrote code for the GameBoyAdvanced years ago. See [Circuit Cellar Magazine, February 2006](http://www.cc-webshop.com/Circuit-Cellar-Issue-187-February-2006-PDF-FI-2006-187.htm). Back 
then I had a nice XPort cartridge to link my computer to the GBA. There was no hardware work
involved -- just C++ and learning the hardware registers on the GBA.

Gameboy/GameboyColor homebrew development is nothing new. There are lots of tutorials on GB/GBC 
hacking (see the links below). This repo is a journal of my experience and any code I produced 
along the way. 

I started with a Game Boy Color and several cartridges I bought from ebay. 

![](art/IMG_0310.JPG)

## Links

Nice, concise listing of registers and opcode changes:
[Tech info](http://fms.komkon.org/GameBoy/Tech/Software.html)

Here is a cartridge like I made (I got the idea here). It explains how to remove the existing 
ROM and replace with an FRAM. There is even a circuit here to program the FRAM:
[Gameboy Fram Cart](http://www.robotdungeon.com/ElectronicProjectGameboyROM.html)

[http://www.reinerziegler.de/readplus.htm#programmable_MBC5_game_cartridge](http://www.reinerziegler.de/readplus.htm#programmable_MBC5_game_cartridge)

[Wikipedia](https://en.wikipedia.org/wiki/Game_Boy_Color)

[http://www.loirak.com/gameboy/gbprog.php](http://www.loirak.com/gameboy/gbprog.php)

[http://belial.blarzwurst.de/gbpaper/paper.pdf](http://belial.blarzwurst.de/gbpaper/paper.pdf)

[http://gbdev.gg8.se/wiki/articles/Main_Page](http://gbdev.gg8.se/wiki/articles/Main_Page)

[Javascript Emulator](http://imrannazar.com/GameBoy-Emulation-in-JavaScript:-Interrupts)

FRAM datasheet:
[http://datasheet.octopart.com/FM1808-70-PG-Ramtron-datasheet-8328945.pdf](http://datasheet.octopart.com/FM1808-70-PG-Ramtron-datasheet-8328945.pdf)

## Hardware Overview

The Gameboy hardware line is wonderfully backwards compatible. The Gameboy Advanced (GBA) has 
two cartridge slots: one for GBA games and one for old Gameboy/GameboyColor games you may own.

The Gameboy (GB) has a special Z80 processor with a few opcode differences. There is a boot ROM in the console that 
draws a splash screen and transfers control to any cartridge plugged in.

The GameboyColor (GBC) is the GB with additional hardware. The processor and memory layout is the same. GB games play fine 
on the GBC. The GB uses 4 AA batteries (6V). The GBC uses 2 AA batteries (3V) but has a power boot to crank 3V up to 5 to 
run the console and cartridge.

The cartridge connector is one-sided, 32 pins. The signals are power, ground, clock, read, write, chip-select, reset, audio, 16 address lines, and 8 data lines. 

![](art/cartPinout.png)

The Z80's address space is split nicely in half. The lower 32K (A15=0) is ROM space in the cartridge. The upper
32K (A15=1) is for hardware registers, system RAM, and an additional 8K RAM area in the cartridge.

Most cartridges include extra RAM (often battery-backed) and ROMs much larger than 32K. Nintendo used a line of
bank-switching chips like MBC5 in each cartridge to select banks of RAM and ROM in the cartridge. I'll discuss
the MBC more in the programming section below. The chip generates the upper address lines (A14 and up) from
internal latches you twiddle by writing to the ROM address space.

Below are the boards from two cartridges. The Tarzan cartridge has a ROM chip on the left and the MBC5
bank switching chip on the right. The Winnie the Pooh cartridge has RAM and ROM (left and right) with the
MBC5 chip above the RAM. The 8 pin chip in the top left monitors the cartridge's power and keeps the battery-backed
RAM in backup mode to protect it when the power drops. The battery is the big silver disk in the upper right.

![](art/IMG_0322.JPG)

## Development Cartridge

The easiest way to start is to modify an existing cartridge: replace the ROM chip with something programmable.

The cartridge is one-sided. It slides into the back of the unit so that the connector pads and parts are facing 
out (not in towards the unit).

Cartridges are hardware-backwards-compatible. A simple game with only one bank of ROM will work fine in a cartridge
that includes battery backed RAM and room for many ROM banks. Thus a modified Zelda cartridge, which has all
the possible resources, could be used to play any game.

I decided to use the simplest hardware: the Tarzan cartridge. I used a screwdriver and needle-nose pliars to unscrew 
the Nintendo screw to open the cartridge. I used diagonal cutters to carefully remove the ROM chip, and I cleaned off 
the legs with a fine tip soldering iron. 

This picture shows the cleaned board. All of the surface mount pads are traced to through-holes making it
easy to solder wires. I have labeled the ROM's holes and the cartridge connector.

![](art/cart2.png)

The MBC5 bank switch divides the 32K ROM space into two 16K banks. The data lines and address lines A0-A13 (16K) 
are wired directly from the cartridge connector to the ROM chip. The upper two processor address lines A14 and A15 
are wired to the MBC5 chip. MBC5 outputs the RA14-RA19 address lines for larger ROMs as banks are
switched in and out through software.

Here are the pinouts for the Nintendo ROM chip and the MBC5 bank switcher. The Tarzan ROM is an LN538, and the
upper left pin (pin 1) is A19. 20 address lines -- that's a 1M ROM divided by the MBC5 into 64 16K banks.

![](art/NROM.png)

I found an old 32K FRAM chip in DIP form on ebay. These are getting harder and harder to find. A much better way 
would be to use a modern FRAM chip, but these are all surface mount. I wanted to start with something easier 
to work with.

![](art/FM1808.png)

The FRAM chip is a little tricky in its bus cycle. It uses CE to latch in the address, and a little later you can
assert OE to read from it. The FRAM is also sensitive to the CE level. If the CE driver tristates then the
data can get mangled (and it does in my playing with it). The datasheet strongly suggests using a pullup resistor
on the CE line.

On the GB cartridges, the ROM's CE line is connected directly to the A15 line. As soon as the A15 goes low then the 
remainder of the address is latched into the FRAM chip. Makes sense because the entire A15=0 area is reserved
for the ROM chip.

The Z80 tristates the address lines between access cycles. You must add a pullup resistor (10K does nicely) to the 
CE line in the modified cartridge. Then when A15 tristates between cycles the CE is pulled back up to begin another
access cycle.

I used a 32 pin chip socket for the FRAM even though it only needs 28. That leaves room for future expansion if
I ever rewire it for a larger FRAM.

The FRAM wiring is simple. Use the labeled board picture above. Wire up all the signals D0-D8, A0-A13, RD, CE, 
and RA14 directly from the Nintendo ROM through-holes. Wire up VDD and GND. Wire in a 10K resistor between
VDD and CE.

![](art/cardTop.png)
![](art/cardBottom.png)

![](art/shellTop.png)
![](art/shellBottom.png)

## Programmer

Many people connect the WR line from the cartridge connector to the FRAM's WR signal. Then they make a cartridge 
reader/writer box to program the cartridge. 

I went with a socketed FRAM chip. You take it off the development cartridge and put it in a programmer to load new code. 
Very old-school.

I made my own programmer using a Propeller prop-stick board.

The propeller output pins are only 3.3V. But the FRAM chip recognizes 2.0 as high. The VDD of the FRAM chip
must be 5V. The propeller tolerates the 5V outputs from the FRAM chip.

Again, the 10K pull-up resistor is very important. You must pull the CE pin to VDD so that the FRAM does not 
get mangled during reset when the propeller chip's GPIO lines are tristated.

![](art/Programmer.png)

![](art/Prog2.png)

## Testing Development Cycle

I downloaded the GB Tetris ROM to my PC. I used my programmer to write it to the FRAM chip. Then I put the
FRAM chip in the cartridge and put it in the GBC. Works great!

![](art/tetris.png)


