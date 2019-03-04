/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.eveningoutpost.dexdrip.importedLibraries.usbserial.driver;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import java.io.IOException;

/**
 * Interface for a single serial port.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public interface UsbSerialPort {

    /** 5 data bits. */
    int DATABITS_5 = 5;

    /** 6 data bits. */
    int DATABITS_6 = 6;

    /** 7 data bits. */
    int DATABITS_7 = 7;

    /** 8 data bits. */
    int DATABITS_8 = 8;

    /** No flow control. */
    int FLOWCONTROL_NONE = 0;

    /** RTS/CTS input flow control. */
    int FLOWCONTROL_RTSCTS_IN = 1;

    /** RTS/CTS output flow control. */
    int FLOWCONTROL_RTSCTS_OUT = 2;

    /** XON/XOFF input flow control. */
    int FLOWCONTROL_XONXOFF_IN = 4;

    /** XON/XOFF output flow control. */
    int FLOWCONTROL_XONXOFF_OUT = 8;

    /** No parity. */
    int PARITY_NONE = 0;

    /** Odd parity. */
    int PARITY_ODD = 1;

    /** Even parity. */
    int PARITY_EVEN = 2;

    /** Mark parity. */
    int PARITY_MARK = 3;

    /** Space parity. */
    int PARITY_SPACE = 4;

    /** 1 stop bit. */
    int STOPBITS_1 = 1;

    /** 1.5 stop bits. */
    int STOPBITS_1_5 = 3;

    /** 2 stop bits. */
    int STOPBITS_2 = 2;

    UsbSerialDriver getDriver();

    /**
     * Port number within driver.
     */
    int getPortNumber();

    /**
     * The serial number of the underlying UsbDeviceConnection, or {@code null}.
     */
    String getSerial();

    /**
     * Opens and initializes the port. Upon success, caller must ensure that
     * {@link #close()} is eventually called.
     *
     * @param connection an open device connection, acquired with
     *            {@link UsbManager#openDevice(android.hardware.usb.UsbDevice)}
     * @throws IOException on error opening or initializing the port.
     */
    void open(UsbDeviceConnection connection) throws IOException;

    /**
     * Closes the port.
     *
     * @throws IOException on error closing the port.
     */
    void close() throws IOException;

    /**
     * Reads as many bytes as possible into the destination buffer.
     *
     * @param dest the destination byte buffer
     * @param timeoutMillis the timeout for reading
     * @return the actual number of bytes read
     * @throws IOException if an error occurred during reading
     */
    int read(final byte[] dest, final int timeoutMillis) throws IOException;
    int read(final byte[] dest, final int timeoutMillis, final UsbDeviceConnection connection) throws IOException;

    /**
     * Writes as many bytes as possible from the source buffer.
     *
     * @param src the source byte buffer
     * @param timeoutMillis the timeout for writing
     * @return the actual number of bytes written
     * @throws IOException if an error occurred during writing
     */
    int write(final byte[] src, final int timeoutMillis) throws IOException;

    /**
     * Sets various serial port parameters.
     *
     * @param baudRate baud rate as an integer, for example {@code 115200}.
     * @param dataBits one of {@link #DATABITS_5}, {@link #DATABITS_6},
     *            {@link #DATABITS_7}, or {@link #DATABITS_8}.
     * @param stopBits one of {@link #STOPBITS_1}, {@link #STOPBITS_1_5}, or
     *            {@link #STOPBITS_2}.
     * @param parity one of {@link #PARITY_NONE}, {@link #PARITY_ODD},
     *            {@link #PARITY_EVEN}, {@link #PARITY_MARK}, or
     *            {@link #PARITY_SPACE}.
     * @throws IOException on error setting the port parameters
     */
    void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException;

    /**
     * Gets the CD (Carrier Detect) bit from the underlying UART.
     *
     * @return the current state, or {@code false} if not supported.
     * @throws IOException if an error occurred during reading
     */
    boolean getCD() throws IOException;

    /**
     * Gets the CTS (Clear To Send) bit from the underlying UART.
     *
     * @return the current state, or {@code false} if not supported.
     * @throws IOException if an error occurred during reading
     */
    boolean getCTS() throws IOException;

    /**
     * Gets the DSR (Data Set Ready) bit from the underlying UART.
     *
     * @return the current state, or {@code false} if not supported.
     * @throws IOException if an error occurred during reading
     */
    boolean getDSR() throws IOException;

    /**
     * Gets the DTR (Data Terminal Ready) bit from the underlying UART.
     *
     * @return the current state, or {@code false} if not supported.
     * @throws IOException if an error occurred during reading
     */
    boolean getDTR() throws IOException;

    /**
     * Sets the DTR (Data Terminal Ready) bit on the underlying UART, if
     * supported.
     *
     * @param value the value to set
     * @throws IOException if an error occurred during writing
     */
    void setDTR(boolean value) throws IOException;

    /**
     * Gets the RI (Ring Indicator) bit from the underlying UART.
     *
     * @return the current state, or {@code false} if not supported.
     * @throws IOException if an error occurred during reading
     */
    boolean getRI() throws IOException;

    /**
     * Gets the RTS (Request To Send) bit from the underlying UART.
     *
     * @return the current state, or {@code false} if not supported.
     * @throws IOException if an error occurred during reading
     */
    boolean getRTS() throws IOException;

    /**
     * Sets the RTS (Request To Send) bit on the underlying UART, if
     * supported.
     *
     * @param value the value to set
     * @throws IOException if an error occurred during writing
     */
    void setRTS(boolean value) throws IOException;

    /**
     * Flush non-transmitted output data and / or non-read input data
     * @param flushRX {@code true} to flush non-transmitted output data
     * @param flushTX {@code true} to flush non-read input data
     * @return {@code true} if the operation was successful, or
     * {@code false} if the operation is not supported by the driver or device
     * @throws IOException if an error occurred during flush
     */
    boolean purgeHwBuffers(boolean flushRX, boolean flushTX) throws IOException;

}
