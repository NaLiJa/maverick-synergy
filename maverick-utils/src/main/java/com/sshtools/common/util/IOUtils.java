package com.sshtools.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 * @author $author$
 */
public class IOUtils {

    /**
     * Default buffer size for stream utility methods
     */
    public static int BUFFER_SIZE = 65535;

    /**
     * Copy from an input stream to an output stream. It is up to the caller to
     * close the streams.
     * 
     * @param in input stream
     * @param out output stream
     * @throws IOException on any error
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        copy(in, out, -1);
    }
    
    /**
     * Copy from an input stream to an output stream. It is up to the caller to
     * close the streams.
     * 
     * @param in input stream
     * @param out output stream
     * @param forceFlush force flush of the OutputStream on each block
     * @throws IOException on any error
     */
    public static void copy(InputStream in, OutputStream out, boolean forceFlush) throws IOException {
        copy(in, out, -1, BUFFER_SIZE, true);
    }


    /**
     * Copy the specified number of bytes from an input stream to an output
     * stream. It is up to the caller to close the streams.
     * 
     * @param in input stream
     * @param out output stream
     * @param count number of bytes to copy
     * @throws IOException on any error
     */
    public static void copy(InputStream in, OutputStream out, long count) throws IOException {
    	copy(in, out, count, BUFFER_SIZE, false);
    }

    /**
     * Copy the specified number of bytes from an input stream to an output
     * stream. It is up to the caller to close the streams.
     * 
     * @param in input stream
     * @param out output stream
     * @param count number of bytes to copy
     * @param bufferSize buffer size
     * @throws IOException on any error
     */
    public static void copy(InputStream in, OutputStream out, long count, int bufferSize, boolean forceFlush) throws IOException {
        byte buffer[] = new byte[bufferSize];
        int i = bufferSize;
        if (count >= 0) {
            while (count > 0) {
                if (count < bufferSize)
                    i = in.read(buffer, 0, (int) count);
                else
                    i = in.read(buffer, 0, bufferSize);

                if (i == -1)
                    break;

                count -= i;
                out.write(buffer, 0, i);
                if(forceFlush) {
                	out.flush();
                }
            }
        } else {
            while (true) {
                i = in.read(buffer, 0, bufferSize);
                if (i < 0)
                    break;
                out.write(buffer, 0, i);
                if(forceFlush) {
                	out.flush();
                }
            }
        }
    }
    
    /**
     * Copy from an input stream to an output stream. It is up to the caller to
     * close the streams.
     * 
     * @param in input stream
     * @param out output stream
     * @throws IOException on any error
     */
    public static long copyWithCount(InputStream in, OutputStream out) throws IOException {
        return copyWithCount(in, out, -1);
    }
    
    /**
     * Copy from an input stream to an output stream. It is up to the caller to
     * close the streams.
     * 
     * @param in input stream
     * @param out output stream
     * @param forceFlush force flush of the OutputStream on each block
     * @throws IOException on any error
     */
    public static long copyWithCount(InputStream in, OutputStream out, boolean forceFlush) throws IOException {
    	return copyWithCount(in, out, -1, BUFFER_SIZE, true);
    }


    /**
     * Copy the specified number of bytes from an input stream to an output
     * stream. It is up to the caller to close the streams.
     * 
     * @param in input stream
     * @param out output stream
     * @param count number of bytes to copy
     * @throws IOException on any error
     */
    public static long copyWithCount(InputStream in, OutputStream out, long count) throws IOException {
    	return copyWithCount(in, out, count, BUFFER_SIZE, false);
    }
    
    /**
     * Copy the specified number of bytes from an input stream to an output
     * stream. It is up to the caller to close the streams.
     * 
     * @param in input stream
     * @param out output stream
     * @param count number of bytes to copy
     * @param bufferSize buffer size
     * @throws IOException on any error
     */
    public static long copyWithCount(InputStream in, OutputStream out, long count, int bufferSize, boolean forceFlush) throws IOException {
        byte buffer[] = new byte[bufferSize];
        int i = bufferSize;
        long total = 0L;
        if (count >= 0) {
            while (count > 0) {
                if (count < bufferSize)
                    i = in.read(buffer, 0, (int) count);
                else
                    i = in.read(buffer, 0, bufferSize);

                if (i == -1)
                    break;

                count -= i;
                total += i;
                
                out.write(buffer, 0, i);
                if(forceFlush) {
                	out.flush();
                }
            }
        } else {
            while (true) {
                i = in.read(buffer, 0, bufferSize);
                if (i < 0)
                    break;
                out.write(buffer, 0, i);
                total += i;
                if(forceFlush) {
                	out.flush();
                }
            }
        }
        return total;
    }
  /**
   *
   *
   * @param in
   *
   * @return
   */
  public static boolean closeStream(InputStream in) {
    try {
      if (in != null) {
        in.close();
      }

      return true;
    }
    catch (IOException ioe) {
      return false;
    }
  }

  /**
   *
   *
   * @param out
   *
   * @return
   */
  public static boolean closeStream(OutputStream out) {
    try {
      if (out != null) {
        out.close();
      }

      return true;
    }
    catch (IOException ioe) {
      return false;
    }
  }


	/**
	 * Recursively delete all contents of the given directory. By default symbolic links will not be followed.
	 * The directory itself will NOT be deleted.
	 * 
	 * @param directory directory containing files or directories
	 * @param options options
	 * @throws UncheckedIOException on any error
	 */
	public static void recursiveContentsDelete(File directory, FileVisitOption... options) {
		recursiveContentsDelete(directory.toPath(), options);
	}
	
	/**
	 * Recursively delete all contents of the given directory. By default symbolic links will not be followed.
	 * The directory itself will NOT be deleted.
	 * 
	 * @param directory directory containing files or directories
	 * @param options options
	 * @throws UncheckedIOException on any error
	 */
	public static void recursiveContentsDelete(Path directory, FileVisitOption... options) {
		try(var stream = Files.newDirectoryStream(directory)) {
			stream.forEach(d -> recursiveDelete(d, options));
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}
	
	/**
	 * Recursively delete a file or directory, and all child files and directories if a directory. By default symbolic links will not be followed.
	 * An exception will NOT be thrown on error, instead <code>false</code> will be returned.
	 * 
	 * @param fileOrDirectory directory containing files or directories
	 * @param options options
	 * @return success
	 */
	public static boolean silentRecursiveDelete(File fileOrDirectory, FileVisitOption... options) {
		return silentRecursiveDelete(fileOrDirectory.toPath(), options);
	}
	
	/**
	 * Recursively delete a file or directory, and all child files and directories if a directory. By default symbolic links will not be followed.
	 * An exception will NOT be thrown on error, instead <code>false</code> will be returned.
	 * 
	 * @param fileOrDirectory directory containing files or directories
	 * @param options options
	 * @return success
	 */
	public static boolean silentRecursiveDelete(Path fileOrDirectory, FileVisitOption... options) {
		try {
			recursiveDelete(fileOrDirectory, options);
		} catch (UncheckedIOException ioe) {
			return false;
		}
		return true;
	}
	
	/**
	 * Recursively delete a file or directory, and all child files and directories if a directory. By default symbolic links will not be followed.
	 * 
	 * @param fileOrDirectory directory containing files or directories
	 * @param options options
	 * @throws UncheckedIOException on any error
	 */
	public static void recursiveDelete(Path fileOrDirectory, FileVisitOption... options) {
		try (var walk = Files.walk(fileOrDirectory, options)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} 
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

  public static void copyFile(File from, File to) throws IOException {

    if (from.isDirectory()) {
      if (!to.exists()) {
        to.mkdir();
      }
      String[] children = from.list();
      for (int i = 0; i < children.length; i++) {
        File f = new File(from, children[i]);
        if (f.getName().equals(".")
            || f.getName().equals("..")) {
          continue;
        }
        if (f.isDirectory()) {
          File f2 = new File(to, f.getName());
          copyFile(f, f2);
        }
        else {
          copyFile(f, to);
        }
      }
    }
    else if (from.isFile() && (to.isDirectory() || to.isFile())) {
      if (to.isDirectory()) {
        to = new File(to, from.getName());
      }
      FileInputStream in = new FileInputStream(from);
      FileOutputStream out = new FileOutputStream(to);
      byte[] buf = new byte[32678];
      int read;
      while ( (read = in.read(buf)) > -1) {
        out.write(buf, 0, read);
      }
      closeStream(in);
      closeStream(out);

    }
  }


  public static int readyFully(InputStream in, byte[] buf) throws IOException {
	
	  int r, c = 0;
	  do {
		  r = in.read(buf, c, buf.length-c);
		  if(r==-1) {
			  if(c==0) {
				  c = -1;
			  }
			  break;
		  }
		  c += r;
	  } while(c < buf.length);
	  
	  return c;
  }

  public static void writeUTF8StringToStream(OutputStream out, String string) throws UnsupportedEncodingException, IOException {
	  writeStringToStream(out, string, "UTF-8");
  }
  
  public static void writeStringToStream(OutputStream out, String string, String charset) throws UnsupportedEncodingException, IOException {
	try {
		out.write(string.getBytes(charset));
		out.flush();
	} finally {
		out.close();
	}	
  }
	
  public static void writeUTF8StringToFile(File file, String string) throws UnsupportedEncodingException, IOException {
	  writeStringToFile(file, string, "UTF-8");
  }
  
  public static void writeStringToFile(File  file, String string, String charset) throws UnsupportedEncodingException, IOException {
	try(OutputStream out = new FileOutputStream(file)) {
		writeStringToStream(out, string, charset);
	}
  }

  public static Long fromByteSize(String val) {
	  if(val.matches("\\d+")) {
		  return Long.parseLong(val);
	  }
	  
	  Pattern p = Pattern.compile("(\\d+)(.*)");
	  Matcher m = p.matcher(val);
	  if(!m.matches()) {
		  throw new IllegalArgumentException(String.format("Invalid input %s", val));
	  }
	  String n = m.group(1);
	  String t = m.group(2);
	  
	  t = t.toUpperCase();
	  
	  Long v = Long.parseLong(n);
	  
	  switch(t) {
	  case "P":
	  case "PB":
		  return v * 1000 * 1000 * 1000 * 1000 * 1000;
	  case "PIB":
		  	return v * 1024 * 1024 * 1024 * 1024 * 1024;
	  case "T":
	  case "TB":
		  return v * 1000 * 1000 * 1000 * 1000;
	  case "TIB":
		  return v * 1024 * 1024 * 1024 * 1024;
	  case "G":
	  case "GB":
		  return v * 1000 * 1000 * 1000;
	  case "GIB":
		  return v * 1024 * 1024 * 1024;
	  case "M":
	  case "MB":
		  return v * 1000 * 1000;
	  case "MIB":
		  return v * 1024 * 1024;
	  case "K":
	  case "KB":
		  return v * 1000;
	  case "KIB":
		  return v * 1024;
	  default:
		  throw new IllegalArgumentException(String.format("Invalid input %s", val));
	  }
  }
  

	public static String toByteSize(double t) {
		return toByteSize(t, 2);
	}

	public static String toByteSize(double t, int decimalPlaces) {
		
		if(decimalPlaces < 0) {
			throw new IllegalArgumentException("Number of decimal places must be > 0");
		}
		String[] sizes = { "B", "KB", "MB", "GB", "TB", "PB" };
		int idx = 0;
		double x = t;
		while(x / 1000 >= 1) {
			idx++;
			x = (x / 1000);
		}
		
		return String.format("%." + decimalPlaces + "f%s", x, sizes[idx]);
	}
	
	public static byte[] sha1Digest(File file) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
		return sha1Digest(new FileInputStream(file));
	}
	
	public static byte[] sha1Digest(InputStream in) throws NoSuchAlgorithmException, IOException {
		
		try(DigestOutputStream out = new DigestOutputStream(new OutputStream() {
			public void write(int b) { }
		}, MessageDigest.getInstance("SHA-1"))) {
			copy(in, out);
			return out.getMessageDigest().digest();
		} finally {
			closeStream(in);
		}
	}


	public static void closeStream(Closeable obj) {
		if(Objects.isNull(obj)) {
			return;
		}
		try {
			obj.close();
		} catch (IOException e) {
		}
	}
	
	private static int getExtensionIndex(String filename) {
		int idx = filename.lastIndexOf('.');
		return idx;
	}
	public static String getFilenameExtension(String filename) {
		int idx = getExtensionIndex(filename);
		if(idx > -1) {
			return filename.substring(idx+1);
		}
		return null;
	}
	
	public static String getFilenameWithoutExtension(String filename) {
		int idx = getExtensionIndex(filename);
		if(idx > -1) {
			return filename.substring(0, idx);
		}
		return filename;
	}


	public static void rollover(File logFile, int maxFiles) throws IOException {
		
		String fileExtension = IOUtils.getFilenameExtension(logFile.getName());
		String fileName = IOUtils.getFilenameWithoutExtension(logFile.getName());
		if(!Objects.isNull(fileExtension)) {
			fileExtension = String.format(".%s", fileExtension);
		} else {
			fileExtension = "";
		}
		
		File parentDir = logFile.getParentFile();
		File lastFile = null;
		for(int i=maxFiles; i >= 1; i--) {
			
			File backup = new File(parentDir, String.format("%s.%d%s", fileName, i, fileExtension));
			if(backup.exists()) {
				if(i==maxFiles) {
					if(!backup.delete()) {
						throw new IOException("Failed to delete rolling log file " + fileName);
					}
				} else {
					if(!backup.renameTo(lastFile)) {
						throw new IOException("Faield to rename rolling log file to " + lastFile.getName());
					}
				}
			}
			lastFile = backup;
		}
		
		if(!logFile.renameTo(lastFile)) {
			throw new IOException("Faield to rename rolling log file to " + lastFile.getName());
		}
	}


	public static String readUTF8StringFromFile(File file) throws IOException {
		return readStringFromFile(file, "UTF-8");
	}
	
	public static String readUTF8StringFromStream(InputStream in) throws IOException {
		return readStringFromStream(in, "UTF-8");
	}
	
	public static String readStringFromFile(File file, String charset) throws UnsupportedEncodingException, IOException {
		try(InputStream in = new FileInputStream(file)) {
			return readStringFromStream(in, charset);
		}
	}
	
	public static String readStringFromStream(InputStream in, String charset) throws IOException {
		try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			IOUtils.copy(in, out);
			return new String(out.toByteArray(), charset);	
		} 
	}
	
	public static InputStream toInputStream(String value, String charset) throws IOException {
		return new ByteArrayInputStream(value.getBytes(charset));
	}


	public static void writeBytesToFile(byte[] value, File file) throws IOException {
		
		OutputStream out = new FileOutputStream(file);
		try {
			out.write(value);
		} finally {
			closeStream(out);
		}
		
	}

	public static void copy(File file, OutputStream out) throws IOException {
		try(FileInputStream in = new FileInputStream(file)) {
			in.transferTo(out);
		}
	}

	public static boolean isOlderThan(File file, Duration duration) {
		
		long now = System.currentTimeMillis();
		return file.lastModified() < (now - duration.toMillis()) ; 
	}
}
