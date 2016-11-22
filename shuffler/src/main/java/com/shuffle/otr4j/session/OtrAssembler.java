/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shuffle.otr4j.session;

import java.net.ProtocolException;

/**
 *
 * @author Felix Eckhofer
 *
 */
public final class OtrAssembler {

	/**
	 * Accumulated fragment thus far.
	 */
	private StringBuffer fragment;

	/**
	 * Number of last fragment received.
	 * This variable must be able to store an unsigned short value.
	 */
	private int fragmentCur;

	/**
	 * Total number of fragments in message.
	 * This variable must be able to store an unsigned short value.
	 */
	private int fragmentMax;

	/**
	 * Relevant instance tag.
	 * OTRv3 fragments with a different instance tag are discarded.
	 */
	private final InstanceTag ownInstance;

	private static final String HEAD_FRAGMENT_V2 = "?OTR,";
	private static final String HEAD_FRAGMENT_V3 = "?OTR|";

	public OtrAssembler(InstanceTag ownInstance) {
		this.ownInstance = ownInstance;
		discard();
	}

	/**
	 * Appends a message fragment to the internal buffer and returns
	 * the full message if msgText was no fragmented message or all
	 * the fragments have been combined. Returns null, if there are
	 * fragments pending or an invalid fragment was received.
	 * <p>
	 * A fragmented OTR message looks like this:
	 * (V2) ?OTR,k,n,piece-k,
	 *  or
	 * (V3) ?OTR|sender_instance|receiver_instance,k,n,piece-k,
	 *
	 * @param msgText Message to be processed.
	 *
	 * @return String with the accumulated message or
	 *         null if the message was incomplete or malformed
     * @throws ProtocolException MVN_PASS_JAVADOC_INSPECTION
	 * XXX ProtocolException might not be a good choice for this,
	 *   as it is meant to be used for network related things,
	 *   and does not allow to add either a message or cause
     * @throws UnknownInstanceException MVN_PASS_JAVADOC_INSPECTION
	 */
	public String accumulate(String msgText)
		throws ProtocolException, UnknownInstanceException
	{
		// if it's a fragment, remove everything before "k,n,piece-k"
		if (msgText.startsWith(HEAD_FRAGMENT_V2)) {
			// v2
			msgText = msgText.substring(
					HEAD_FRAGMENT_V2.length());
		} else if (msgText.startsWith(HEAD_FRAGMENT_V3)) {
			// v
			msgText = msgText.substring(
					HEAD_FRAGMENT_V3.length());

			// break away the v2 part
			String[] instancePart = msgText.split(",", 2);
			// split the two instance ids
			String[] instances = instancePart[0].split("\\|", 2);

			if (instancePart.length != 2 || instances.length != 2) {
				discard();
				throw new ProtocolException();
			}

			int receiverInstance;
			try {
				receiverInstance = Integer.parseInt(instances[1], 16);
			} catch (NumberFormatException e) {
				discard();
				throw new ProtocolException();
			}
			if (receiverInstance != 0
					&& receiverInstance != ownInstance.getValue())
			{
				// discard message for different instance id
				throw new UnknownInstanceException(
						"Message for unknown instance tag "
						+ String.valueOf(receiverInstance)
						+ " received: " + msgText);
			}

			// continue with v2 part of fragment
			msgText = instancePart[1];
		} else {
			// not a fragmented message
			discard();
			return msgText;
		}

		String[] params = msgText.split(",", 4);

		int k, n;
		try {
			k = Integer.parseInt(params[0]);
			n = Integer.parseInt(params[1]);
		} catch (NumberFormatException e) {
			discard();
			throw new ProtocolException();
		} catch (ArrayIndexOutOfBoundsException e) {
			discard();
			throw new ProtocolException();
		}

		if (k == 0 || n == 0 || k > n || params.length != 4 || params[3].length() != 0) {
			discard();
			throw new ProtocolException();
		}

		msgText = params[2];

		if (k == 1) {
			// first fragment
			discard();
			fragmentCur = k;
			fragmentMax = n;
			fragment.append(msgText);
		} else if (n == fragmentMax && k == fragmentCur+1) {
			// consecutive fragment
			fragmentCur++;
			fragment.append(msgText);
		} else {
			// out-of-order fragment
			discard();
			throw new ProtocolException();
		}

		if (n == k && n > 0) {
			String result = fragment.toString();
			discard();
			return result;
		} else {
			return null; // incomplete fragment
		}
	}

	/**
	 * Discard current fragment buffer and reset the counters.
	 */
	public void discard() {
		fragment = new StringBuffer();
		fragmentCur = 0;
		fragmentMax = 0;
	}
}
