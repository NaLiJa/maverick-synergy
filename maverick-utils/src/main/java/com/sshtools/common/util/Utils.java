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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class Utils {

	/**
	 * From https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
	 */
	private final static char[] hexArray = "0123456789abcdef".toCharArray();

	public static String before(String value, char token) {
		int idx = value.indexOf(token);
		if(idx < 0) {
			return value;
		}
		return value.substring(0, idx);
	}
	
	public static String after(String value, char token) {
		int idx = value.indexOf(token);
		if(idx < 0) {
			return value;
		}
		if(value.length() > idx-1) {
			return value.substring(idx+1);
		} else {
			return "";
		}
	}
	
	public static String after(String value, String token) {
		int idx = value.indexOf(token);
		if(idx < 0) {
			throw new IndexOutOfBoundsException();
		}
		return value.substring(idx+token.length());
	}
	
	public static String before(String value, String token) {
		int idx = value.indexOf(token);
		if(idx < 0) {
			throw new IndexOutOfBoundsException();
		}
		return value.substring(0, idx);
	}
	
	public static String bytesToHex(byte[] bytes) {
		return bytesToHex(bytes, 0, bytes.length);
	}
	
	public static String bytesToHex(byte[] bytes, int off, int len) {
	    return bytesToHex(bytes, off, len, 0, false, false);
	}
	
	public static String bytesToHex(byte[] bytes, int bytesPerLine, boolean separateBytes, boolean showText) {
		return bytesToHex(bytes, 0, bytes.length, bytesPerLine, separateBytes, showText);
	}
	
	public static String bytesToHex(byte[] bytes, int off, int len, int bytesPerLine, boolean separateBytes, boolean showText) {
	    StringBuffer buffer = new StringBuffer();
	    StringBuffer text = new StringBuffer();
	    if(bytesPerLine==0) {
	    	bytesPerLine = len;
	    }
	    int remaining = len;
	    int lines = len / bytesPerLine;
	    for( int i = 0; i < lines; i++) {
		    for ( int j = 0; j < bytesPerLine; j++ ) {
		        int v = bytes[off+(i*bytesPerLine)+j] & 0xFF;
		        buffer.append(hexArray[v >>> 4]);
		        buffer.append(hexArray[v & 0x0F]);
		        if(showText) {
		        	if(v >= 32 && v <= 126) {
		        		text.append((char)v);
		        	} else {
		        		text.append(".");
		        	}
		        }
		        if(separateBytes) {
		        	buffer.append(" ");
		        }
		        remaining--;
		    }
		    
        	if(showText) {
        		buffer.append(" [ ");
        		buffer.append(text.toString());
        		buffer.append(" ]");
        		text.setLength(0);
        	}
        	
        	if(bytesPerLine < len) {
        		buffer.append(System.lineSeparator());
        	}

	    }
	    
	    while(remaining > 0) {
	    	int v = bytes[off+(len-remaining)] & 0xFF;
	        buffer.append(hexArray[v >>> 4]);
	        buffer.append(hexArray[v & 0x0F]);
	        if(showText) {
	        	if(v >= 32 && v <= 126) {
	        		text.append((char)v);
	        	} else {
	        		text.append(".");
	        	}
	        }
	        if(separateBytes) {
	        	buffer.append(" ");
	        }
	        remaining--;
	    }
	    
	    if(len % bytesPerLine > 0) {
	    	remaining = bytesPerLine - len % bytesPerLine;
	    }
	    if(showText) {
	    	for(int i=0;i<remaining;i++) {
	    		buffer.append("  ");
	    		if(separateBytes) {
	    			buffer.append(" ");
	    		}
	    		text.append(" ");
	    	}
    		buffer.append(" [ ");
    		buffer.append(text.toString());
    		buffer.append(" ]");
    		text.setLength(0);
    	}
	    
	    return buffer.toString();
	}

	public static byte[] stripLeadingZeros(byte[] data) {
		int x;
		for(x=0;x<data.length;x++) {
			if(data[x] != 0) {
				break;
			}
		}
		if((data[x] & 0x80) != 0) {
			x--;
		}
		if(x > 0) {
			byte[] tmp = new byte[data.length - x];
			System.arraycopy(data, x, tmp, 0, tmp.length);
			return tmp;
		} else {
			return data;
		}
	}

	public static int nearestMultipleOf(int length, int i) {
		
		int difference;
		if((difference = length % i) == 0) {
			return length;
		}
		
		if(difference < (i/2)) {
			return length - difference;
		} else {
			return length + (i - difference);
		}
	}
	
	public static String[] splitToArgsArray(String args) {
		
		boolean quoted = false;
		List<String> results = new ArrayList<>();
		StringBuffer buf = new StringBuffer();
		for(int i=0;i<args.length();i++) {
			switch(args.charAt(i)) {
			case '"':
			{
				quoted = !quoted;
				break;
			}				
			case ' ':
			{
				if(!quoted) {
					String r = buf.toString().trim();
					if(r.length() > 0) {
						results.add(r);
					}
					buf.setLength(0);
				} else {
					buf.append(args.charAt(i));
				}
				break;
			}
			default:
				buf.append(args.charAt(i));
			}
		}
			
		if(buf.length() > 0) {
			results.add(buf.toString());
		}
		
		return results.toArray(new String[0]);
	}

	/**
	 * From https://crunchify.com/how-to-generate-java-thread-dump-programmatically/
	 * @return
	 */
	public static String generateThreadDump(Thread.State... states) {
        final StringBuilder dump = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 1000);
        Set<Thread.State> enabledStates = new HashSet<>(Arrays.asList(states));
        for (ThreadInfo threadInfo : threadInfos) {
        	if(enabledStates.isEmpty() || enabledStates.contains(threadInfo.getThreadState())) {
	            dump.append('"');
	            dump.append(threadInfo.getThreadName());
	            dump.append("\" ");
	            final Thread.State state = threadInfo.getThreadState();
	            dump.append("\n   java.lang.Thread.State: ");
	            dump.append(state);
	            final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
	            for (final StackTraceElement stackTraceElement : stackTraceElements) {
	                dump.append("\n        at ");
	                dump.append(stackTraceElement);
	            }
	            dump.append("\n\n");
        	}
        }
        return dump.toString();
    }

	public static boolean isBlank(String str) {
		return Objects.isNull(str) || str.trim().length() == 0;
	}
	
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

	public static String mergeToArgsString(String[] args) {
		StringBuffer buf = new StringBuffer();
		for(String arg : args) {
			if(buf.length() > 0) {
				buf.append(" ");
			}
			if(arg.matches(".*\\s.*")) {
				buf.append("\"");
				buf.append(arg);
				buf.append("\"");
			} else {
				buf.append(arg);
			}
		}
		return buf.toString();
	}

	public static byte[] getUTF8Bytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Your system does not appear to support UTF-8 character encoding!", e);
		}
	}

	public static String join(String[] args, String str) {
		StringBuffer buf = new StringBuffer();
		for(String arg : args) {
			if(buf.length() > 0) {
				buf.append(str);
			}
			buf.append(arg);
		}
		return buf.toString();
	}

	public static String rightPad(String rendered, int minWidth) {
		StringBuffer buf= new StringBuffer(rendered);
		while(buf.length() < minWidth) {
			buf.append(" ");
		}
		return buf.toString();
	}

	public static String defaultString(String value, String defaultValue) {
		if(Objects.isNull(value) || value.length()==0) {
			return defaultValue;
		}
		return value;
	}
	
	public static String csv(String... elements) {
		return csv(Arrays.asList(elements));
	}
	
	public static String csv(Collection<String> elements) {
		StringBuffer b = new StringBuffer();
		for(String element : elements) {
			if(b.length() > 0) {
				b.append(",");
			}
			b.append(element);
		}
		return b.toString();
	}

	public static String csv(String separator, String... elements) {
		return csv(separator, Arrays.asList(elements));
	}
	
	public static String csv(String separator, Collection<String> elements) {
		StringBuffer b = new StringBuffer();
		for(String element : elements) {
			if(b.length() > 0) {
				b.append(separator);
			}
			b.append(element);
		}
		return b.toString();
	}
	
	public static String randomAlphaNumericString(int length) {
		 return new BigInteger(length * 8, new Random()).toString(32).substring(0,  length);
	}
	
	public static String exec(String... cmd) throws IOException, InterruptedException {
		
		Process process = new ProcessBuilder(cmd).start();
		
		StringBuilder output = new StringBuilder();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line + "\n");
        }

        int exitVal = process.waitFor();
        if (exitVal == 0) {
            return output.toString();
        } else {
            throw new IOException("Unexpected exit code " + exitVal + "[" + output.toString() + "]");
        }
	}
	
	public static String prompt(BufferedReader reader, String message) throws IOException {
		
		System.out.print(String.format("%s: ", message));
		return reader.readLine();
		
	}
	
	public static String prompt(BufferedReader reader, String message, String defaultValue) throws IOException {
		
		System.out.print(String.format("%s [%s]: ", message, defaultValue));
		String line = reader.readLine();
		if(isBlank(line)) {
			return defaultValue;
		}
		return line;
		
	}

	public static boolean hasPort(String hostname) {
		int idx = hostname.indexOf(":");
		if(idx > -1) {
			String portString = hostname.substring(idx+1);
			if(portString.matches("^[0-9]*$")) {
				int value = Integer.parseInt(portString);
				if(value > 0 && value < 65536) {
					return true;
				}
			} 
			return false;
		}
		return false;
	}
	
	public static String formatHostnameAndPort(String hostname, int port) {
		return port == 22 ? hostname : hostname + ":" + port;
	}
	
	public static int getPort(String hostname) {
		int idx = hostname.indexOf(":");
		if(idx > -1) {
			String portString = hostname.substring(idx+1);
			if(portString.matches("^[0-9]*$")) {
				int value = Integer.parseInt(portString);
				if(value > 0 && value < 65536) {
					return value;
				}
			} 
		}
		throw new IllegalArgumentException("Input does not contain a port value");
	}

	/**
     * Converts a hex string to bytes.
     * @param s the hex string to be converted.
     * @return the byte[]
     */
    public static byte[] hexToBytes(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String pad(int num) {
		String str = "";

		if (num > 0) {
			for (int i = 0; i < num; i++) {
				str += " ";
			}
		}

		return str;
	}

	public static Optional<String> emptyOptionalIfBlank(String str) {
		return "".equals(str) ? Optional.empty() : Optional.ofNullable(str);
	}

	public static Optional<char[]> emptyOptionalIfBlank(char[] str) {
		return str != null && str.length == 0 ? Optional.empty() : Optional.ofNullable(str);
	}
	
	public static String translatePathString(Path path) {
		return path.toString().replace('\\', '/');
	}

	public static String encodeUserInfo(String userinfo) {
		try {
			return new URI("sftp", userinfo, "localhost", 22, null, null, null).getRawUserInfo();
		} catch (URISyntaxException e) {
			/* NOTE: Will NEVER be thrown, and can't be tested for coverage :( */
			throw new IllegalArgumentException(e);
		}
	}
}
