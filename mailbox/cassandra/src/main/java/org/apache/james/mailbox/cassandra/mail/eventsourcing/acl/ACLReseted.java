/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.cassandra.mail.eventsourcing.acl;

import java.util.Objects;

import org.apache.james.eventsourcing.AggregateId;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventId;
import org.apache.james.mailbox.acl.ACLDiff;
import org.apache.james.mailbox.cassandra.ids.CassandraId;

public class ACLReseted implements Event {
    private final MailboxAggregateId id;
    private final EventId eventId;
    private final ACLDiff aclDiff;

    public ACLReseted(MailboxAggregateId id, EventId eventId, ACLDiff aclDiff) {
        this.id = id;
        this.eventId = eventId;
        this.aclDiff = aclDiff;
    }

    public CassandraId mailboxId() {
        return id.asMailboxId();
    }

    public ACLDiff getAclDiff() {
        return aclDiff;
    }

    @Override
    public EventId eventId() {
        return eventId;
    }

    @Override
    public AggregateId getAggregateId() {
        return id;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof ACLReseted) {
            ACLReseted that = (ACLReseted) o;

            return Objects.equals(this.eventId, that.eventId)
                && Objects.equals(this.id, that.id)
                && Objects.equals(this.aclDiff, that.aclDiff);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(eventId, id, aclDiff);
    }
}
