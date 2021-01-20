/******************************************************************************************************************
 * File:SourceFilter.java
 * Project: Assignment 1
 * Copyright: Copyright (c) 2003 Carnegie Mellon University
 * Versions:
 *	1.0 November 2008 - Sample Pipe and Filter code (ajl).
 *
 * Description:
 *
 * This class serves as an example for how to use the SourceFilterTemplate to create a source filter. This particular
 * filter is a source filter that reads some input from the FlightData.dat file and writes the bytes up stream.
 *
 * Parameters: 		None
 *
 * Internal Methods: None
 *
 ******************************************************************************************************************/

import java.io.*; // note we must add this here since we use BufferedReader class to read from the keyboard
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

public class MergeFilter extends FilterFramework
{
    public void run()
    {

        int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
        int IdLength = 4;				// This is the length of IDs in the byte stream

        long measurementA = 0;				// This is the word used to store all measurements - conversions are illustrated.
        long measurementB = 0;				// This is the word used to store all measurements - conversions are illustrated.
        int idA = 0;							// This is the measurement id
        int idB = 0;							// This is the measurement id
        int i;							// This is a loop counter

        int bytesreadA = 0;					// Number of bytes read from the input file.
        int bytesreadB = 0;					// Number of bytes read from the input file.
        int byteswrittenA = 0;				// Number of bytes written to the stream.
        int byteswrittenB = 0;				// Number of bytes written to the stream.
        byte databyteA = 0;					// The byte of data read from the file
        byte databyteB = 0;					// The byte of data read from the file

        byte[] byteId = new byte[IdLength];
        byte[] byteData = new byte[MeasurementLength];

        Calendar TimeStampA = Calendar.getInstance();
        Calendar TimeStampB = Calendar.getInstance();
        Date timeA;
        Date timeB;

        Boolean moveA = true;
        Boolean moveB = true;

        while(true)
        {
            try
            {
                if (moveA) {

                    idA = 0;

                    for (i=0; i<IdLength; i++ )
                    {
                        databyteA = ReadFilterInputPortA();	// This is where we read the byte from the stream...

                        idA = idA | (databyteA & 0xFF);		// We append the byte on to ID...

                        if (i != IdLength-1)				// If this is not the last byte, then slide the
                        {									// previously appended byte to the left by one byte
                            idA = idA << 8;					// to make room for the next byte we append to the ID

                        } // if

                        bytesreadA++;						// Increment the byte count

                    } // for
                }

                if (moveB) {

                    idB = 0;

                    for (i=0; i<IdLength; i++ )
                    {
                        databyteB = ReadFilterInputPortB();	// This is where we read the byte from the stream...

                        idB = idB | (databyteB & 0xFF);		// We append the byte on to ID...

                        if (i != IdLength-1)				// If this is not the last byte, then slide the
                        {									// previously appended byte to the left by one byte
                            idB = idB << 8;					// to make room for the next byte we append to the ID

                        } // if

                        bytesreadB++;						// Increment the byte count

                    } // for
                }

                /****************************************************************************
                 // Here we read measurements. All measurement data is read as a stream of bytes
                 // and stored as a long value. This permits us to do bitwise manipulation that
                 // is neccesary to convert the byte stream into data words. Note that bitwise
                 // manipulation is not permitted on any kind of floating point types in Java.
                 // If the id = 0 then this is a time value and is therefore a long value - no
                 // problem. However, if the id is something other than 0, then the bits in the
                 // long value is really of type double and we need to convert the value using
                 // Double.longBitsToDouble(long val) to do the conversion which is illustrated.
                 // below.
                 *****************************************************************************/

                if (moveA) {

                    measurementA = 0;

                    for (i=0; i<MeasurementLength; i++ )
                    {
                        databyteA = ReadFilterInputPortA();
                        measurementA = measurementA | (databyteA & 0xFF);	// We append the byte on to measurement...

                        if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
                        {												// previously appended byte to the left by one byte
                            measurementA = measurementA << 8;				// to make room for the next byte we append to the
                            // measurement
                        } // if

                        bytesreadA++;									// Increment the byte count

                    } // if
                }

                if (moveB) {

                    measurementB = 0;

                    for (i=0; i<MeasurementLength; i++ )
                    {
                        databyteB = ReadFilterInputPortB();
                        measurementB = measurementB | (databyteB & 0xFF);	// We append the byte on to measurement...

                        if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
                        {												// previously appended byte to the left by one byte
                            measurementB = measurementB << 8;				// to make room for the next byte we append to the
                            // measurement
                        } // if

                        bytesreadB++;									// Increment the byte count

                    } // if
                }

                if (idA == 0) {
                    moveA = false;
                }

                if (idB == 0) {
                    moveB = false;
                }


                if (idA == 0 && idB == 0)
                {
                    TimeStampA.setTimeInMillis(measurementA);
                    timeA = TimeStampA.getTime();
                    TimeStampB.setTimeInMillis(measurementB);
                    timeB = TimeStampB.getTime();
                    if (timeA.before(timeB) || timeA.equals(timeB)) {
                        moveA = true;
                    } else {
                        moveB = true;
                    }
                }

                if (moveA) {
                    ByteBuffer.wrap(byteId).putInt(idA);
                    ByteBuffer.wrap(byteData).putLong(measurementA);
                    for (byte bid : byteId) {
                        WriteFilterOutputPort(bid);
                        byteswrittenA++;
                    }

                    for (byte bdata : byteData) {
                        WriteFilterOutputPort(bdata);
                        byteswrittenA++;
                    }
                }

                if (moveB) {
                    ByteBuffer.wrap(byteId).putInt(idB);
                    ByteBuffer.wrap(byteData).putLong(measurementB);

                    for (byte bid : byteId) {
                        WriteFilterOutputPort(bid);
                        byteswrittenB++;
                    }

                    for (byte bdata : byteData) {
                        WriteFilterOutputPort(bdata);
                        byteswrittenB++;
                    }
                }

            } // try
            catch (EndOfStreamException e)
            {
                if (isAvailableA) {
                    ByteBuffer.wrap(byteId).putInt(idA);
                    ByteBuffer.wrap(byteData).putLong(measurementA);
                    for (byte bid : byteId) {
                        WriteFilterOutputPort(bid);
                        byteswrittenA++;
                    }

                    for (byte bdata : byteData) {
                        WriteFilterOutputPort(bdata);
                        byteswrittenA++;
                    }
                    while(true)
                    {
                        try {
                            databyteA = ReadFilterInputPortA();
                            bytesreadA++;
                            WriteFilterOutputPort(databyteA);
                            byteswrittenA++;
                        } catch (EndOfStreamException err) {
                            CloseABPorts();
                            System.out.print( "\n" + this.getName() + "::Merge Exiting; bytes read: A: " + bytesreadA + " B: " + bytesreadB + " bytes written: A: " + byteswrittenA + " B: " + byteswrittenB);
                            break;
                        }

                    } // while
                }

                if (isAvailableB) {
                    ByteBuffer.wrap(byteId).putInt(idB);
                    ByteBuffer.wrap(byteData).putLong(measurementB);

                    for (byte bid : byteId) {
                        WriteFilterOutputPort(bid);
                        byteswrittenB++;
                    }

                    for (byte bdata : byteData) {
                        WriteFilterOutputPort(bdata);
                        byteswrittenB++;
                    }
                    while(true)
                    {
                        try {
                            databyteB = ReadFilterInputPortB();
                            bytesreadB++;
                            WriteFilterOutputPort(databyteB);
                            byteswrittenB++;
                        } catch (EndOfStreamException err) {
                            CloseABPorts();
                            System.out.print( "\n" + this.getName() + "::Merge Exiting; bytes read: A: " + bytesreadA + " B: " + bytesreadB + " bytes written: A: " + byteswrittenA + " B: " + byteswrittenB);
                            break;
                        }

                    } // while
                }
                break;
            } // catch

        } // while
    } // run

} // SourceFilter