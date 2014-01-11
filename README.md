Thresholder
===========

A command line java program that thresholds .pgm images. Sample images are provided for demonstration purposes.

How To Use
----------

Place the .pgm file in the same directory as the program files. Then open the console & switch to the program directory, for example (windows):

```
cd c:\user\documents\progdir
```

Finally run the program from the console passing in any arguments:

(if running source code)
```
javac *.java
java Thresholder arg1 arg2 arg3 arg4 arg5

```

### Arguments

* arg 1 - The filename of .pgm file to read in.
* arg 2 - The number of divisions to threshold. this splits the image into a specified number of sections, calculates the average pixel intensity for each section, and then thresholds the image based on this average value.
* arg 3 - Toggle smoothing (1 for yes, 0 for no). Smoothing uses interpolates the average intensity value for each division which may produce a better threshold.
* arg 4 - Toggle validation (1 for yes, 0 for no). Allows you to specify a minimum average intensity value for the threshold algorithm. Each division that has an average intensity below this value is corrected automatically.
* arg 5 - Only applicable if value for Arg 4 is 1. sets the minimum average intensity value. 
