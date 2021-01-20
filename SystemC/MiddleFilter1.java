import java.io.*;
import java.nio.ByteBuffer;

/******************************************************************************************************************
* File:MiddleFilter1.java
* Project: Assignment 1
* Copyright: Copyright (c) 2003 Carnegie Mellon University
* Versions:
*	1.0 November 2008 - Sample Pipe and Filter code (ajl).
*
* Description:
*
* This class serves as an example for how to use the FilterRemplate to create a standard filter. This particular
* example is a simple "pass-through" filter that reads data from the filter's input port and writes data out the
* filter's output port.
*
* Parameters: 		None
*
* Internal Methods: None
*
******************************************************************************************************************/

public class MiddleFilter1 extends FilterFramework
{
	public void run()
    {

		int MeasurementLength = 8;		// This is the length of all measurements (including time) in bytes
		int IdLength = 4;				// This is the length of IDs in the byte stream

		long measurement;				// This is the word used to store all measurements - conversions are illustrated.
		int id;							// This is the measurement id
		int i;							// This is a loop counter

		int bytesread = 0;					// Number of bytes read from the input file.
		int byteswritten = 0;				// Number of bytes written to the stream.
		byte databyte = 0;					// The byte of data read from the file

		final String wildpointDirPressure = "./WildPointsPressure.csv";

		Double prepreData = null;
		Double preData = null;
		Double Data = null;

		byte[] byteId = new byte[IdLength];
		byte[] byteData = new byte[MeasurementLength];

		File out = new File(wildpointDirPressure);
		FileWriter ow = null;
		try {
			ow = new FileWriter(out);
		} catch (IOException e) {
			System.out.println("MiddleFilter failed to find wildpoints output path.");
		}
		BufferedWriter bw = new BufferedWriter(ow);
		PrintWriter pw = new PrintWriter(bw);

		// Next we write a message to the terminal to let the world know we are alive...

		System.out.print( "\n" + this.getName() + "::Middle Reading ");

		while (true)
		{
			/*************************************************************
			*	Here we read a byte and write a byte
			*************************************************************/

			try
			{
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

				Data = Double.longBitsToDouble(measurement);

				Boolean replaced = false;

				if (id == 3)
				{
					if (preData == null) { preData = Data; }
					if (Data > 90 || Data < 45) {
						pw.write(String.format("%s\n", Double.toString(Data)));
						pw.flush();
						if (prepreData != null) {
							Data = (prepreData + preData) / 2;
						} else {
							Data = preData;
						}
						replaced = true;
					}
					prepreData = preData;
					preData = Data;
				}

				if (replaced) {
					ByteBuffer.wrap(byteId).putInt((id+1)*6);
				} else {
					ByteBuffer.wrap(byteId).putInt(id);
				}
				ByteBuffer.wrap(byteData).putDouble(Data);

				for (byte bid : byteId) {
					WriteFilterOutputPort(bid);
					byteswritten++;
				}

				for (byte bdata : byteData) {
					WriteFilterOutputPort(bdata);
					byteswritten++;
				}

			} // try

			catch (EndOfStreamException e)
			{
				ClosePorts();
				pw.close();
				System.out.print( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				break;

			} // catch

		} // while

   } // run

} // MiddleFilter