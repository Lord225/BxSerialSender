# BxSerialSender

Minecraft plugin that allows for bi-directional serial comunication.

## Commands
* `/serialsend <COM> <data>` - This command is used to send data to an Serial Port. The first argument, `<COM>`, specifies the name of the port, other arguments specifies the data (including spaces)
* `/serialopen <COM> <BOUNDRATE>` - This command is used to open the serial port. The first argument, `<COM>`, specifies the name of the port to open and `<BOUNDRATE>` specifies disaried boundrate (for example `9600`)
* `/serialclose <COM>` - Closes the connection with given port
* `/serialaddcallback <COM> <byte> <command>` - Adds new callback for the given serial port and sensitivity byte, If given byte will be recived specified `command` will fire.
* `/serialclearcallbacks` - Removes all callbacks

## Sending Data
```
/serialopen COM6 9600
/serialsend COM6 Hello World
/serialsend COM6 123
/serialsend COM6 aabb
...
/serialclose COM6
```
## Reciving data
Commands below will crate redstone block when `1` is recived and remove it when `0` is recived.   
```
/serialopen COM6 9600
/serialaddcallback COM6 1 /setblock 0 0 0 minecraft:redstone_block
/serialaddcallback COM6 0 /setblock 0 0 0 minecraft:air
```
To transmit bigger amount of data you can use two values as clock and 8 values to set individual bits.
Lets say `w` means `writing`, `r` means ready and numbers `0-7` are responsible for bits.
when `w` is recived, bits are cleard and SerialPort can send bits that should be set to high. Then it sends `r` to indicate end of transmission.
```
w0123r    - this sequence means that bits 0, 1, 2 and 3 are set to high
```
