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
package com.shuffle.otr4j;

import com.shuffle.otr4j.session.SessionID;

/**
 * This interface should be implemented by the host application. It notifies
 * about session status changes.
 *
 * @author George Politis
 */
public interface OtrEngineListener {
	void sessionStatusChanged(SessionID sessionID);

	void multipleInstancesDetected(SessionID sessionID);

	void outgoingSessionChanged(SessionID sessionID);
}
