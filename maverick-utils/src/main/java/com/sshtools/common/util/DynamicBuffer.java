package com.sshtools.common.util;

/*-
 * #%L
 * Utils
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

/**
 * <p>
 * This class provides an alternative method of storing data, used within the
 * API where Piped Streams could have been used. We found that Piped streams
 * would lock if a thread attempted to read to data when the OutputStream attached
 * was not being read; since we have no control over when the user will actually
 * read the data, this behaviour led us to develop this dynamic buffer which
 * will automatically grow if the buffer is full.
 * </p>
 *  *
 * @author Lee David Painter
 */
public class DynamicBuffer {

  /** Buffer size when the dynamic buffer is opened */
  protected static final int DEFAULT_BUFFER_SIZE = 32768;

  /** The buffer */
  protected byte[] buf;

  /** The current write position */
  protected int writepos = 0;

  /** The current read position */
  protected int readpos = 0;

  /** This buffers InputStream */
  protected InputStream in;

  /** This buffers OutputStream */
  protected OutputStream out;
  private boolean closed = false;
  private int interrupt = 1000;
  private int timeout = 0;
  private boolean closedWithError = false;
  
  /**
   * Creates a new DynamicBuffer object.
   */
  public DynamicBuffer() {
    buf = new byte[DEFAULT_BUFFER_SIZE];
    in = new DynamicBufferInputStream();
    out = new DynamicBufferOutputStream();
  }
  
  public DynamicBuffer(int bufferSize) {
	  	if(bufferSize <= 0) {
	  		throw new IllegalArgumentException("Buffer size cannot be negative");
	  	}
	    buf = new byte[bufferSize];
	    in = new DynamicBufferInputStream();
	    out = new DynamicBufferOutputStream();
	  }

  /**
   * Get the InputStream of this buffer. Use the stream to read data from
   * this buffer.
   *
   * @return
   */
  public InputStream getInputStream() {
    return in;
  }

  /**
   * Get the OutputStream of the buffer. Use this stream to write data to
   * the buffer.
   *
   * @return
   */
  public OutputStream getOutputStream() {
    return out;
  }

  public long getTimeout() {
	 return timeout;
  }

  public void setTimeout(int timeout) {
	 this.timeout = timeout;
  }
  
  private synchronized void verifyBufferSize(int count) {
    // If there is not enough data in the buffer, then first attempt to
    // move the unread data back to the beginning
    if (count > (buf.length - writepos)) {
      System.arraycopy(buf, readpos, buf, 0, writepos - readpos);
      writepos -= readpos;
      readpos = 0;
    }

    // Now double check and increase the buffer size if necersary
    while(count > (buf.length - writepos)) {
      byte[] tmp = new byte[buf.length + DEFAULT_BUFFER_SIZE];
      System.arraycopy(buf, 0, tmp, 0, writepos - readpos);
      buf = tmp;
    }
  }

  /**
   * Return the number of bytes of data available to be read from the buffer
   * @return
   */
  protected synchronized int available() {
    return writepos - readpos > 0
        ? writepos - readpos
        : 0;
  }

  private synchronized void block() throws InterruptedException, IOException {

		long start = System.currentTimeMillis();
		
	    // Block and wait for more data
	    if (!closed) {
	    
	      while ((readpos >= writepos) && !closed) {
    		if(closedWithError) {
    			throw new IOException("The buffer was closed due to an unspecified error");
    		}
	    	wait(interrupt);
	        if(timeout > 0 && (System.currentTimeMillis() - start) > timeout) {
	        	throw new InterruptedIOException();
	        }
	      }
	    }
	  }

  public synchronized void close() {
	  close(false);
  }
  
  /**
   * Closes the buffer
   */
  public synchronized void close(boolean closedWithError) {
	this.closedWithError = closedWithError;
    if(!this.closedWithError) {
		if (!closed) {
	      closed = true;
	      notifyAll();
		}
	}
  }

  /**
   * Write a byte array to the buffer
   *
   * @param b
   *
   * @throws IOException
   */
  protected synchronized void write(int b) throws IOException {
    
	if(closedWithError) {
		throw new IOException("The buffer was closed due to an unspecified error");
	}
	
	if (closed) {
      throw new IOException("The buffer is closed");
    }

    verifyBufferSize(1);
    buf[writepos] = (byte) b;
    writepos++;

    notifyAll();
  }

  /**
   *
   *
   * @param data
   * @param offset
   * @param len
   *
   * @throws IOException
   */
  protected synchronized void write(byte[] data, int offset, int len) throws
      IOException {

	if(closedWithError) {
		throw new IOException("The buffer was closed due to an unspecified error");
	}
		
    if (closed) {
      throw new IOException("The buffer is closed");
    }

    verifyBufferSize(len);
    System.arraycopy(data, offset, buf, writepos, len);
    writepos += len;

    notifyAll();
  }

  public void setBlockInterrupt(int interrupt) {
    this.interrupt = interrupt;
  }

  /**
   * Read a byte from the buffer
   *
   * @return
   *
   * @throws IOException
   * @throws InterruptedIOException
   */
  protected synchronized int read() throws IOException {
    try {
      block();
    }
    catch (InterruptedException ex) {
      throw new InterruptedIOException(
          "The blocking operation was interrupted");
    }

    if (closed && available() <= 0) {
      return -1;
    }

    return buf[readpos++] & 0xFF;

  }

  /**
   * Read a byte array from the buffer
   *
   * @param data
   * @param offset
   * @param len
   *
   * @return
   *
   * @throws IOException
   * @throws InterruptedIOException
   */
  protected synchronized int read(byte[] data, int offset, int len) throws
      IOException {
	  
    try {
       block();
    }
    catch (InterruptedException ex) {
      throw new InterruptedIOException(
          "The blocking operation was interrupted");
    }
	
    if (closed && available() <= 0) {
      return -1;
    }

    int read = (len > (writepos - readpos)) ? (writepos - readpos) : len;
    System.arraycopy(buf, readpos, data, offset, read);
    readpos += read;

    return read;

  }

  /**
   * Flush data
   *
   * @throws IOException
   */
  protected synchronized void flush() throws IOException {
    notifyAll();
  }

  class DynamicBufferInputStream
      extends InputStream {
    public int read() throws IOException {
      return DynamicBuffer.this.read();
    }

    public int read(byte[] data, int offset, int len) throws IOException {
      return DynamicBuffer.this.read(data, offset, len);
    }

    public int available() {
      return DynamicBuffer.this.available();
    }

    public void close() {
      DynamicBuffer.this.close();
    }
  }

  class DynamicBufferOutputStream
      extends OutputStream {
    public void write(int b) throws IOException {
      DynamicBuffer.this.write(b);
    }

    public void write(byte[] data, int offset, int len) throws IOException {
      DynamicBuffer.this.write(data, offset, len);
    }

    public void flush() throws IOException {
      DynamicBuffer.this.flush();
    }

    public void close() {
      DynamicBuffer.this.close();
    }
  }
}
