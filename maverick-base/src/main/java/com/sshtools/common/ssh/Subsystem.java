package com.sshtools.common.ssh;

/*-
 * #%L
 * Base API
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
import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.ByteBufferPool;
import com.sshtools.common.util.UnsignedInteger32;

/**
 * Defines the abstract attributes of an SSH Subsystem.
 * 
 * @author Lee David Painter
 */
public abstract class Subsystem {
	
	public static final Integer SUBSYSTEM_INCOMING = ExecutorOperationQueues.generateUniqueQueue("Subsystem.queue.in");
	public static final Integer SUBSYSTEM_OUTGOING = ExecutorOperationQueues.generateUniqueQueue("Subsystem.queue.out");
	
	protected SessionChannel session;
	protected Context context;
	String name;
	ByteBuffer buffer;
	int message_length = -1;
	int maximumPacketSize = 0;
	ByteBufferPool bufferPool;
	
	boolean shutdown = false;
	long bytesSinceLastWindowIssue = 0;
	
	public Subsystem(String name) {
		this.name = name;
	}

	public Context getContext() {
		return session.getConnection().getContext();
	}
	
	public SshConnection getConnection() {
		return session.getConnection();
	}
	
	public SessionChannel getSession() {
		return session;
	}
	
	/**
	 * Initialize the subsystem with the current session and configuration.
	 * 
	 * @param session
	 * @param context
	 * @throws IOException
	 * @throws PermissionDeniedException 
	 */
	public void init(SessionChannel session, Context context)
			throws IOException, PermissionDeniedException {
		
		bufferPool = context.getByteBufferPool();

		this.session = session;
		this.context = context;

		// We will manage our own data window
		session.haltIncomingData();
		
		session.addEventListener(new ChannelEventListener() {

			@Override
			public void onChannelClose(Channel channel) {
				
				if(!channel.isRemoteEOF()) {
					session.getConnection().addTask(SUBSYSTEM_INCOMING, new ConnectionAwareTask(getConnection()) {
	
						@Override
						protected void doTask() throws Throwable {
							cleanup();
						}
						
					});
				}
				
				ChannelEventListener.super.onChannelClose(channel);
			}

			@Override
			public void onChannelEOF(Channel channel) {
				
				session.getConnection().addTask(SUBSYSTEM_INCOMING, new ConnectionAwareTask(getConnection()) {

					@Override
					protected void doTask() throws Throwable {
						cleanup();
					}
					
				});
				
				ChannelEventListener.super.onChannelEOF(channel);
			}
			
		});

	}
	
	protected void executeOperation(Integer messageQueue, ConnectionAwareTask r) {
		
		if(Boolean.getBoolean("maverick.additionalSFTPIncomingQueue")) {
			session.getConnection().addTask(messageQueue, r);
		} else {
			try {
				r.doTask();
			} catch (Throwable e) {
				Log.error("Caught error in processing SFTP message", e);
				cleanup();
			}
		}
	}
	
	protected synchronized void cleanup() {
		
		if(!shutdown) {

			cleanupSubsystem();
			session.close();
			
			shutdown = true;
		}
	}

	protected abstract void cleanupSubsystem();

	/**
	 * Process channel data and transform into a subsystem message when enough
	 * data has arrived.
	 * 
	 * @param data
	 */
	public void processMessage(ByteBuffer data) throws IOException {
		parseMessage(data);
	}
	
	class ProcessMessageOperation extends ConnectionAwareTask {

		byte[] msg;
		
		ProcessMessageOperation(byte[] msg) {
			super(session.getConnection());
			this.msg = msg;
		}
		
		@Override
		protected void doTask() {
			try {
				onMessageReceived(msg);
			} catch (IOException e) {
				Log.error("Failed to process SFTP message", e);
				cleanup();
			}
		}
		
	}
	
	protected void parseMessage(ByteBuffer data) throws IOException {
		
		if(session.isClosed()) {
			throw new IOException("Session is closed");
		}
		
		if (buffer == null)
			buffer = bufferPool.get();

		if(Log.isTraceEnabled())
			Log.trace("Buffer has " + buffer.remaining()
					+ " bytes remaining of " + buffer.capacity());
		if(Log.isTraceEnabled())
			Log.trace("Processing " + data.remaining() + " bytes of data");

		buffer(data, false);

		do {
			if(Log.isTraceEnabled()) {
				Log.trace("Buffer has remaining=" + buffer.remaining()
						+ " messagLength=" + message_length 
						+ " data=" + data.remaining());
			}
			
			if (message_length == -1 && buffer.remaining() >= 4) {
				message_length = buffer.getInt();
				if(Log.isTraceEnabled()) {
					Log.trace("Expecting subsystem packet length " + message_length);
				}
				
				buffer(data, true);
				
				if (message_length < 0
						|| message_length > (context.getMaximumPacketLength() - 4)) {
					if(Log.isErrorEnabled())
						Log.error("Incoming subsystem message length " + message_length
								+ " exceeds maximum supported packet length "
								+ context.getMaximumPacketLength());
					session.getConnection().disconnect("Protocol error");
					return;
				}
			}
			// Process a message in chunks of 'message_length'
			while (message_length >= 0 && buffer.remaining() >= message_length) {
	
				if(message_length > 0) {
					byte[] msg = new byte[message_length];
		
					buffer.get(msg);
		
					session.getConnection().addTask(SUBSYSTEM_INCOMING, new ProcessMessageOperation(msg));
					
					buffer(data, true);
					
				} else {
					Log.warn("Received zero length message in SFTP subsystem!!");
				}
				
				if (buffer.remaining() >= 4) {
					message_length = buffer.getInt();
					
					buffer(data, true);
				} else {
					message_length = -1;
				}
			}
		} while(data.hasRemaining());
		
		if (!buffer.hasRemaining()) {
			bufferPool.add(buffer);
			buffer = null;
		} else {
			// Reset the buffer ready for writing to again
			buffer.compact();
		}

	}

	private void buffer(ByteBuffer data, boolean compact) {
		
		boolean flip = false;
		if(compact) {
			buffer.compact();
			flip = true;
		}
		
		if(data.hasRemaining() && buffer.hasRemaining()) {
			int length = Math.min(buffer.remaining(), data.remaining());
			ByteBuffer slice = data.slice();
			slice.limit(length);
			buffer.put(slice);
			data.position(data.position() + length);
			flip = true;
		}
		
		if(flip) {
			buffer.flip();
		}
		
	}

	public void free() {

		if(Log.isTraceEnabled())
			Log.trace("Cleaning up " + name + " subsystem references");
		
		onSubsystemFree();

		cleanup();
		
		// Put back the buffer
		if (buffer != null)
			bufferPool.add(buffer);

		buffer = null;
	}

	/**
	 * The subsystem has been closed and all resources should be freed.
	 */
	protected abstract void onSubsystemFree();

	/**
	 * Called when a subsystem message has been extracted from the incoming data
	 * stream.
	 * 
	 * @param msg
	 */
	protected abstract void onMessageReceived(byte[] msg) throws IOException;

	/**
	 * Send a subsystem message. NOTE: you do not have to prefix the message
	 * with its length as this operation is performed inside this method.
	 * 
	 * @param msg
	 * @throws IOException
	 */
	public void sendMessage(Packet packet) throws IOException {
		
		if(Boolean.getBoolean("maverick.outgoingSubsystemQueue")) {
			session.getConnection().addTask(SUBSYSTEM_OUTGOING, new ConnectionAwareTask(getConnection()) {
	
				@Override
				protected void doTask() throws Throwable {
					doSendMessage(packet);
				}
			});
		} else {
			doSendMessage(packet);
		}
	}

	private void doSendMessage(Packet packet) throws IOException {
		if (session.isClosed()) {
			throw new IOException("Failed to send subsystem packet, session closed");
		} else {
			if(Log.isTraceEnabled())
				Log.trace("Sending subsystem packet of " + packet.size()
						+ " bytes");
			packet.finish();
			session.sendData(packet.array(), 0, packet.size());
		}
	}
	
	public void onFreeMessage(byte[] msg) {
	
		if(maximumPacketSize < msg.length + 4) {
			maximumPacketSize = msg.length + 4;
		}
		
		bytesSinceLastWindowIssue += msg.length + 4;
		long threshold = Math.min(session.getMaximumWindowSpace().longValue() - session.getMinimumWindowSpace().longValue(), 
				session.getMaximumWindowSpace().longValue() - (Math.max(session.getLocalPacket(), maximumPacketSize) * 2));
		if(bytesSinceLastWindowIssue >= threshold) {
			session.sendWindowAdjust(new UnsignedInteger32(bytesSinceLastWindowIssue));
			bytesSinceLastWindowIssue = 0;
		}
		
	}

}
