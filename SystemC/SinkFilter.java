/******************************************************************************************************************
 * File:SinkFilter.java
 * Project: Assignment 1
 * Copyright: Copyright (c) 2003 Carnegie Mellon University
 * Versions:
 *	1.0 November 2008 - Sample Pipe and Filter code (ajl).
 *
 * Description:
 *
 * This class serves as an example for using the SinkFilterTemplate for creating a sink filter. This particular
 * filter reads some input from the filter's input port and does the following:
 *
 *	1) It parses the input stream and "decommutates" the measurement ID
 *	2) It parses the input steam for measurments and "decommutates" measurements, storing the bits in a long word.
 *
 * This filter illustrates how to convert the byte stream data from the upstream filterinto useable data found in
 * the stream: namely time (long type) and measurements (double type).
 *
 *
 * Parameters: 	None
 *
 * Internal Methods: None
 *
 ******************************************************************************************************************/
import java.io.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.*;						// This class is used to interpret time words
import java.text.SimpleDateFormat;		// This class is used to format and write time in a string format.

public class SinkFilter extends FilterFramework
{
	public void run()
	{
		/************************************************************************************
		 *	TimeStamp is used to compute time using java.util's Calendar class.
		 * 	TimeStampFormat is used to format the time value so that it can be easily printed
		 *	to the terminal.
		 *************************************************************************************/

		Calendar TimeStamp = Calendar.getInstance();

		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		byte databyte = 0;				// This is the data byte read from the stream
		int bytesread = 0;				// This is the number of bytes read from the stream

		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter

		final String outputDir = "./OutputC.csv";	// This is the output data location

		Date Time = null;
		String timePattern = "yyyy:dd:hh:mm:ss";
		DateFormat df = new SimpleDateFormat(timePattern);
		Double Velocity = null;
		Double Altitude = null;
		Double Pressure = null;
		Double Temperature = null;
		Double Pitch = null;
		String frame = "";

		File out = new File(outputDir);
		FileWriter ow = null;
		try {
			ow = new FileWriter(out);
		} catch (IOException e) {
			System.out.println("SinkFilter failed to find output path.");
		}
		BufferedWriter bw = new BufferedWriter(ow);
		PrintWriter pw = new PrintWriter(bw);

		/*************************************************************
		 *	First we announce to the world that we are alive...
		 **************************************************************/

		System.out.print( "\n" + this.getName() + "::Sink Reading ");

		while (true)
		{
			try
			{
				/***************************************************************************
				 // We know that the first data coming to this filter is going to be an ID and
				 // that it is IdLength long. So we first decommutate the ID bytes.
				 ****************************************************************************/

				id = 0;

				for (i=0; i<IdLength; i++ )
				{
					databyte = ReadFilterInputPort();	// This is where we read the byte from the stream...

					id = id | (databyte & 0xFF);		// We append the byte on to ID...

					if (i != IdLength-1)				// If this is not the last byte, then slide the
					{									// previously appended byte to the left by one byte
						id = id << 8;					// to make room for the next byte we append to the ID

					} // if

					bytesread++;						// Increment the byte count

				} // for

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

				measurement = 0;

				for (i=0; i<MeasurementLength; i++ )
				{
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);	// We append the byte on to measurement...

					if (i != MeasurementLength-1)					// If this is not the last byte, then slide the
					{												// previously appended byte to the left by one byte
						measurement = measurement << 8;				// to make room for the next byte we append to the
						// measurement
					} // if

					bytesread++;									// Increment the byte count

				} // if

				/****************************************************************************
				 // Here we look for an ID of 0 which indicates this is a time measurement.
				 // Every frame begins with an ID of 0, followed by a time stamp which correlates
				 // to the time that each proceeding measurement was recorded. Time is stored
				 // in milliseconds since Epoch. This allows us to use Java's calendar class to
				 // retrieve time and also use text format classes to format the output into
				 // a form humans can read. So this provides great flexibility in terms of
				 // dealing with time arithmetically or for string display purposes. This is
				 // illustrated below.
				 ****************************************************************************/

				Boolean endOfFrame = false;

				Boolean replaced = false;

				if (id > 5) {
					id = id / 6 - 1;
					replaced = true;
				}

				if ( id == 0 )
				{
					TimeStamp.setTimeInMillis(measurement);
					Time = TimeStamp.getTime();
					frame = df.format(Time);

				} // if

				if (id == 1)
				{
					Velocity = Double.longBitsToDouble(measurement);
					frame += "," + Velocity;
				}

				if (id == 2)
				{
					Altitude = Double.longBitsToDouble(measurement);
					frame += "," + Altitude;
				}

				if (id == 3)
				{
					Pressure = Double.longBitsToDouble(measurement);
					frame += "," + Pressure;
				}

				if (id == 4)
				{
					Temperature = Double.longBitsToDouble(measurement);
					frame += "," + Temperature;
				}

				if (id == 5)
				{
					Pitch = Double.longBitsToDouble(measurement);
					frame += "," + Pitch;
					endOfFrame = true;
				}

				if (replaced) {
					frame += "*";
				}

				if (endOfFrame) {
					// combine all data fields into a single frame
					frame += "\n";
					pw.write(frame);
					pw.flush();
				}

			} // try

			/*******************************************************************************
			 *	The EndOfStreamExeception below is thrown when you reach end of the input
			 *	stream (duh). At this point, the filter ports are closed and a message is
			 *	written letting the user know what is going on.
			 ********************************************************************************/

			catch (EndOfStreamException e)
			{
				ClosePorts();
				pw.close();
				System.out.print( "\n" + this.getName() + "::Sink Exiting; bytes read: " + bytesread );
				break;
			} // catch

		} // while

	} // run

} // SingFilter