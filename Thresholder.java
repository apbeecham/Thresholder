import java.util.*;

/**
 * A runnable java class that reads in a greyscale (.pgm) image
 * and produces a thresholded version of the .pgm image and a .pgm of the threshold used. The program takes
 * the following arguments:
 * 
 * Arg 1 - The filename of .pgm file to read in.
 * 
 * Arg 2 - The number of divisions to threshold. the splits the image into
 * a specified number of sections, calculates the average pixel intensity for each
 * section, and then thresholds the image based on this average value.
 * 
 * Arg 3 - Toggle smoothing (1 for yes, 0 for no). Smoothing uses interpolates the average intensity
 * value for each division which may produce a better threshold.
 * 
 * Arg 4 - Toggle validation (1 for yes, 0 for no). Allows you to specify a minimum average intensity
 * value for the threshold algorithm. Each division that has an average intensity below this value is 
 * corrected automatically.
 * 
 * Arg 5 - Only applicable if value for Arg 4 is 1. sets the minimum average intensity value.* 
 *
 * @author Adam Beecham
 */

public class Thresholder {
    double total;
    Image image;
    Image thresholdSurface;
    static String fileNameIn;

    Window[][] windows;

    public static void main(String[] args)
        throws java.io.IOException
    {
        //parse input
        int length = args.length;
        fileNameIn = args[0];
        String fileNameOut = "result.pgm";
        int divisions =  Integer.parseInt(args[1]);
        int toggleSmoothing =  Integer.parseInt(args[2]);
        int toggleValid =  Integer.parseInt(args[3]);
        int tolerance = 0;
        if(length > 4)
        {
            tolerance =  Integer.parseInt(args[4]);
        }

        //make a threshold surface and threshold the image
        Thresholder thresh = new Thresholder(fileNameIn);
        thresh.makeThresholdSurface(divisions, fileNameOut, tolerance, toggleValid, toggleSmoothing);
        thresh.thresholdImage(divisions, fileNameOut, tolerance, toggleValid, toggleSmoothing);       
        
    }
       
    /*
     * Extract intensities from the image for calculating standard deviation
     */
    public int[] getIntensities(int in[][], int width, int height, int xStart, int yStart)
    {
        int[] intensities = new int[256];
        
        //count the number of occurences of each intensity in the image
        for (int y = yStart; y < height; y++)
        {
            for (int x = xStart; x < width; x++)
            {
                intensities[in[x][y]] ++;
            }
        }

        return intensities;
    }
    
    /*
     * Constructor
     */
    public Thresholder(String fileNameIn)
    {
        image = new Image();
        thresholdSurface = new Image();        
        image.ReadPGM(fileNameIn);
        thresholdSurface.ReadPGM(fileNameIn);
             
        total = image.width * image.height;
    }


    /*
     * make a window for the threshold surface
     */
    public void makeThresholdWindow(int threshold, int width, int height,int xStart,int yStart)
    {
        //the intensity for each pixel in the window is the same as the threshold
        for(int x = xStart; x < width; x++)
        {
            for(int y = yStart; y < height; y++)
            {
                thresholdSurface.pixels[x][y] = threshold;
            }
        }
    }

    /*
     * threshold the section of the image that corresponds to this window of the
     * threshold surface
     */
    public void thresholdWindow(int threshold, int width, int height,int xStart,int yStart)
    {
        //compare each pixel in the image with the corresponding pixel in the
        //threshold surface and colour it black or white accordingly
        for(int x = xStart; x < width; x++)
        {
            for(int y = yStart; y < height; y++)
            {
                if(image.pixels[x][y] < thresholdSurface.pixels[x][y])
                    image.pixels[x][y] = 0;
                else
                    image.pixels[x][y] = 255;
            }
        }
    }

    /*
     * threshold the image
     */
    public void thresholdImage(int numWindows, String fileNameOut, int tolerance,  int toggleValid, int toggleSmoothing)
    {        
        int width = thresholdSurface.width/numWindows;
        int height = thresholdSurface.height/numWindows;

        //threshold the image according to the threshold of each corresponding window in the threshold surface
        for(int y = 0; y < numWindows; y++)
        {
            for(int x = 0; x < numWindows; x++)
            {                
                thresholdWindow(windows[x][y].threshold,width * (x + 1), height * (y + 1),width * x, height * y);
            }
        }

        image.WritePGM("result.pgm");
    }

    /*
     * generate the threshold surface
     */
    public void makeThresholdSurface(int numWindows, String fileNameOut, int tolerance,  int toggleValid, int toggleSmoothing)
    {
        windows = new Window[numWindows][numWindows];
        int width = thresholdSurface.width/numWindows;
        int height = thresholdSurface.height/numWindows;


         // for each window in the surface, calculate the best threshold and apply it to the window
        for(int y = 0; y < numWindows; y++)
        {
            for(int x = 0; x < numWindows; x++)
            {
                windows[x][y] = new Window(x, y, width, height);

                int[] intensities = getIntensities(thresholdSurface.pixels,width * (x + 1), height * (y + 1),width * x, height * y);
                windows[x][y].findBestThreshold(intensities, width * height, tolerance);
                makeThresholdWindow(windows[x][y].threshold,width * (x + 1), height * (y + 1),width * x, height * y);
                System.out.printf("STANDARD DEVIATION: (%02d, %02d) = %.3f", x, y, windows[x][y].summedSD);
                System.out.println();
            }
        }
        
        //label invalid windows and correct them
        if(toggleValid == 1)
        {
            labelWindows(numWindows, tolerance);
            correctInvalidThresholds(numWindows);
        }

        //perform bilinear interpolation
        if(toggleSmoothing == 1)
        {
            smooth(numWindows);
        }
      
        thresholdSurface.WritePGM("threshold.pgm");
    }

    /*
     *  correct windows with standard deviation below tolerance
     */
    public void correctInvalidThresholds(int numWindows)
    {
        int width = thresholdSurface.width/numWindows;
        int height = thresholdSurface.height/numWindows;
        boolean done = false;
        //System.out.println("total windows " + numWindows * numWindows);

        int iteration = 1;

        while(!done)
        {
            boolean noInvalid =  true;
            int invalidCount = 0;
            
            //check if windows which were previously invalid are now valid
            for(int y = 0; y < numWindows; y++)
            {
                for(int x = 0; x < numWindows; x++)
                {
                    if(!windows[x][y].isValid)
                    {
                        if(windows[x][y].madeValid)
                        {
                            windows[x][y].isValid = true;
                        }
                    }
                }
            }
            
            //check each windows neighbours to see if any of them are valid, then replace
            //the windows threshold with the mean threshold of the valid neighbours
            for(int y = 0; y < numWindows; y++)
            {
                for(int x = 0; x < numWindows; x++)
                {
                    if(!windows[x][y].isValid)
                    {
                        boolean print = compareToNeighbours(windows[x][y], x, y, numWindows, numWindows);
                        noInvalid = false;
                        invalidCount ++;
                        if(print)
                        {
                            System.out.printf("UPDATE THRESHOLD (ITERATION %d): (%02d, %02d) NEW THRESHOLD = %d", iteration, x, y, windows[x][y].threshold);
                            System.out.println();
                        }
                    }
                    //replace the window
                    makeThresholdWindow(windows[x][y].threshold,width * (x + 1), height * (y + 1),width * x, height * y);
                }
            }
            iteration ++;

            //finish if all windows are corrected or all windows are invalid
            if(noInvalid || invalidCount >= (numWindows * numWindows))
                done = true;
        }

    }

    /*
     * label each window as valid or invalid
     */
    public void labelWindows(int numWindows, float tolerance)
    {
        int width = thresholdSurface.width/numWindows;
        int height = thresholdSurface.height/numWindows;

        //check if each window is valid
        for(int y = 0; y < numWindows; y++)
        {
            for(int x = 0; x < numWindows; x++)
            {
                checkIfValid(windows[x][y],width * (x + 1), height * (y + 1),width * x, height * y, tolerance);
                
            }
        }

        thresholdSurface.WritePGM("valid.pgm");
    }

    /*
     * check if the window should be labeled valid (white) or invalid (black)
     */
    public void checkIfValid(Window w, int width, int height, int xStart, int yStart, float tolerance)
    {
        double sd = w.summedSD;

        //if the windows standard deviation is below the tolerance it is invalid
        if(sd <= tolerance)
        {
            for(int x = xStart; x < width; x++)
            {
                for(int y = yStart; y < height; y++)
                {
                    thresholdSurface.pixels[x][y] = 0;
                }
            }
            w.isValid = false;
        }
        //otherwise it is fine
        else
        {
            for(int x = xStart; x < width; x++)
            {
                for(int y = yStart; y < height; y++)
                {
                    thresholdSurface.pixels[x][y] = 255;
                }
            }
        }
    }

    /*
     * smooth the threshold surface with bilinear interpolation
     */
    public void smooth(int numWindows)
    {
        //interpolate all the pixels between 4 neighouring windows
        for(int x = 0; x < numWindows - 1; x++ )
        {
            for (int y = 0; y < numWindows - 1; y ++)
            {
                //the x and y distances between the center points of each image
                float distanceY = distanceY(windows[x][y], windows[x][y + 1]);
                float distanceX = distanceX(windows[x][y], windows[x + 1][y]);
                interpolate(distanceX , distanceY , windows[x][y].centerX + 1, windows[x][y].centerY + 1, windows[x][y + 1].threshold, windows[x][y].threshold, windows[x + 1][y + 1].threshold, windows[x + 1][y].threshold);
            }
        }
        
    }

    /*
     * perform bilinear interpolation on the pixels between 4 neighbouring windows
     */
    public void interpolate(float xDistance, float yDistance, int xStart, int yStart, int threshold1, int threshold2, int threshold3, int threshold4)
    {
        //go through each pixel and compute its correct intensity, depending on how
        //far between each of the pixels it is
        for(int x  = xStart; x < (int)(xDistance) + xStart ; x ++ )
        {
            for(int y = yStart; y < (int)yDistance + yStart; y ++)
            {
                //functions R and S
                int fr = (int)(threshold2 + ((float)((y - yStart)/yDistance) * (threshold1 - threshold2)));
                int fs = (int)(threshold4 + ((float)((y - yStart)/yDistance) * (threshold3 - threshold4)));
                //function T (the appropriate intensity for this pixel)
                thresholdSurface.pixels[x][y] = (int)(fr + ((float)((x - xStart)/xDistance) * (fs - fr)));
            }
        }
    }
    /*
     * x distance between 2 windows
     */
    public float distanceX(Window w1, Window w2)
    {
        return w2.centerX - w1.centerX;
    }

    /*
     * y distance between 2 windows
     */
    public float distanceY(Window w1, Window w2)
    {
        return w2.centerY - w1.centerY;
    }
    
    /*
     * compare a window to its 8 neighbours and determine if any are valid
     */
    public boolean compareToNeighbours(Window w,int xpos, int ypos, int width, int height)
    {
        double sum = 0;
        double count = 0;
        boolean hasValidNeighbour = false;

        //find a valid neighbour
        for(int x = xpos - 1; x <= xpos + 1; x++)
        {
            for(int y = ypos - 1; y <= ypos + 1; y++)
            {
                //ignore windows out of bounds
                if((x == xpos && y == ypos)
                        || x < 0 || y < 0 ||
                        x >= width || y >= height)
                {                   
                    continue;
                }

                //found a valid neighbour
                if(windows[x][y].isValid)
                {
                    sum += windows[x][y].threshold;
                    count ++;
                    hasValidNeighbour = true;
                }                
            }
        }

        //correct the threshold of this window
        if(hasValidNeighbour)
        {            
           windows[xpos][ypos].threshold = (int)Math.round(sum/count);
           windows[xpos][ypos].madeValid = true;
        }

        return hasValidNeighbour;
    }


}

/*
 *  Helper class for handling windows in the threshold surface
 */
class Window
{
    public double summedSD;
    public int threshold;
    public boolean isValid = true;
    public boolean madeValid = false;
    public int centerX;
    public int centerY;
    public int x;
    public int y;
    public int width;
    public int height;

    /*
     * constructor
     */
    public Window(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        centerX = ((x + 1) * width) - (width/2);
        centerY = ((y + 1) * height) - (height/2);
    }

    /*
     * find the best threshold for this window
     */
    public void findBestThreshold(int[] in, int total, int tolerance)
    {
        int thresh = 0;
        double low =  1000000000;
        double blackSD = 0;
        double whiteSD = 0;
        double pBlack = 0;
        double pWhite = 0;
        int bestThresh = 0;

        //find the threshold with the lowest summed standard deviation
        for(int i = 0; i < in.length; i++)
        {
            whiteSD = calcStandardDeviation(in, thresh + 1, in.length);
            blackSD = calcStandardDeviation(in, 0, thresh + 1);
            pWhite = count(in, thresh + 1, in.length)/total;
            pBlack = count(in, 0, thresh + 1)/total;

            double current = (pBlack * blackSD) + (pWhite * whiteSD);
            //replace the previous best threshold if the summed standard deviation
            // for this threshold is lower
            if(current < low)
            {
                low = current;
                bestThresh = thresh;
            }

            thresh++;
        }
        
        summedSD = low;

        //if there is no variance the best threshold is 128
        if(low == 0)
            bestThresh = 128;

        threshold = bestThresh;
    }

    /*
     * calculate the standard deviation for a given class
     */
    public double calcStandardDeviation(int[] in, int from, int to)
    {
        int count = 0;
        double average = 0;
        double difference = 0;
        double mean = calcMean(in,from,to);

        //calculate the squared difference from the mean
        //for each of the intensities in this class
        for (int i = from; i < to; i++)
        {
                count += in[i];
                for(int k = 1; k <= in[i]; k++)
                {
                    difference = Math.pow((i - mean),2);
                    average += difference;
                }
        }
        //square root the average of the differences
        if(count > 0)
        {
            average = average/count;
            return Math.sqrt(average);
        }
        else
        {
            return 0;
        }


    }

    /*
     * calculate the mean for a given class
     */
    public double calcMean(int[] in, int from, int to)
    {
        double sum = 0;
        double count = 0;
        //sum the total number of occurences of a given intensity
        for (int i = from; i < to; i++)
        {
            sum += in[i] * i;
            count += in[i];
        }
        if(count > 0)
        {
            return sum/count;
        }
        else
        {
            return 0;
        }
    }

    /*
     * count the number of occurences of each intensity
     */
    public double count(int[] in, int from, int to)
    {
        int count = 0;
        for (int i = from; i < to; i++)
        {
            count += in[i];
        }
        return count;
    }

}
