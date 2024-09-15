# Journal

## 9/15/2024

Almost 10 years later! I cleaned up the repo and stared on a UV EPROM version of the dev cart.

## 12/27/2015

Here is where I am.

I got the cartridge all made save for soldering the wires to the FRAM. I made the FRAM programmer
first, but it isn't consistent. It seems to program many of the bytes, but some are stubborn.
Sometimes I get "old values" and then the new ones on another read. It could be the access cycles
need tweaking.

I wrote a serial program for the propeller to read/write/checksum the RAM. 
The datasheet says the minimum power is 4.5V. But one of the links shows a guy using it in the 
gameboy (3V). But he programmed it with a 5V programmer. The prop programmer powers the chip 
with 3.3V.

Maybe the power supply from the prop chip isn't strong enough to drive the FRAM chip too. Try using an
external 3.3V supply. Try using a 5V programmer. The 1808 is obsolete. Try using a modern FRAM
chip even if you have to make a DIP adapter board. But I'd like to see the FRAM chip work since I've
taken it this far and there is a video of it working.

Pausing a bit to work on other projects. I'll be back.
