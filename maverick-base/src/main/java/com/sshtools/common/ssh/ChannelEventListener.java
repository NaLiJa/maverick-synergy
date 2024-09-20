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

import java.nio.ByteBuffer;

/**
 * Interface for receiving {@link Channel} events (currently only supports the
 * close event).
 *
 * @author Lee David Painter
 */
public interface ChannelEventListener {

  /**
   * The channel has been opened.
   * @param channel channel
   */
  default void onChannelOpen(Channel channel) {
  }

  /**
   * The channel has been closed
   * @param channel channel
   */
  default void onChannelClose(Channel channel) {
  }

  /**
   * The channel has received an EOF from the remote client
   * @param channel
   * @param channel channel
   */
  default void onChannelEOF(Channel channel) {
  }


  /**
   * Called when a channel is disconnected because of a connection loss. You may still receive onChannelClose 
   * event after this.
   * @param channel channel
   */
  default void onChannelError(Channel channel, Throwable e) {
  }
  
  /**
   * The channel is closing, but has not sent its SSH_MSG_CHANNEL_CLOSE
   * @param channel Channel
   */
  default void onChannelClosing(Channel channel) {
  }

  /**
   * When the remote side adjusts its window.
   * @param channel channel
   * @param currentWindowSpace current window space
   */
  default void onWindowAdjust(Channel channel, long currentWindowSpace) {
  }

  /**
   * Data has been received on the channel. The buffer provided is the same buffer that will
   * be passed on to any thread reading the channels streams.
   * 
   * @param channel Channel
   * @param buffer buffer
   */
  default void onChannelDataIn(Channel channel, ByteBuffer buffer) {
  }

  /**
   * Data has been received on the extended channel. The buffer provided is the same buffer that will
   * be passed on to any thread reading the channels streams.
   * 
   * @param channel Channel
 * @param buffer buffer
 * @param type type
   */
  default void onChannelExtendedData(Channel channel, ByteBuffer buffer, int type) {
  }

  /**
   * Data has been sent on the channel. The buffer provided is the same buffer that will
   * be passed on to any thread writing the channels streams.
   * 
   * @param channel Channel
   * @param buffer buffer
   */
  default void onChannelDataOut(Channel channel, ByteBuffer buffer) {
  }
}
