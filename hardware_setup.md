## What you need to get started
Note, the following describes what I am currently using and recommend, it could definitely change and you can definitely use other components. Just know that this is all that it has been tested with!

* An Android Device
 * Must be running 4.3 to support BLE
 * Also must have BLE support
* [A Wixel](http://www.pololu.com/product/1337)
 * I recommend the one without headers(linked) if you want to use wires, if you plan to use a breadboard or make a custom PCB feel free to get the one with headers.
* [An HM-10 BLE Module](http://www.amazon.com/SunFounder-Bluetooth-Master-Compatible-Arduino/dp/B00N7CA8Y6/ref=sr_1_cc_1?s=aps&ie=UTF8&qid=1416917892&sr=1-1-catcorr&keywords=hm10+sunfounder)
 * I use the one linked, its great, make sure yours is 3.3v or lower if you wish to power it through the wixel
 * The android app specificly looks for the uuid of the HM10, any other device may require changes to the android app in order to work
* [Battery Power](http://www.adafruit.com/products/258)
 * I use the one linked and have no problem getting through a day, I have not done extensive battery testing at this point
* [Battery Charger](http://www.adafruit.com/products/1904)
 * I use this one, its nice and smallish and does the trick.
* [Wires](http://www.adafruit.com/product/2051)
 * I use these 30AWG silicon wires because they are super small and flexible
 * Larger wires will make it harder to keep things nice and compact, keep with a small guage!
* Solder and Soldering Iron
 * Or a breadboard if you dont mind the bulk
 * Or a custom made PCB (and one for me too please?)
 * I needed a fairly hot soldering iron (40w) to remove the pins from the HM10, be careful not to burn the boards though
 * I used a regular not terrifying soldering iron for the rest of it!
 
 ## Putting it together!
 ![SETUP](/http://i.imgur.com/EIGki5R.png)