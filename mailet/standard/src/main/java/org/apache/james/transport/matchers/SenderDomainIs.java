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

package org.apache.james.transport.matchers;

import java.util.Collection;

import jakarta.mail.MessagingException;

import org.apache.james.core.Domain;
import org.apache.james.core.MailAddress;
import org.apache.james.core.MaybeSender;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * <P>
 * SenderDomainIs will look at the envelope sender's domain. If equal to
 * configured value, then all recipients will be returned. Overwise an empty
 * list will be returned.
 * </P>
 * <p>
 * Sample configuration:
 * </p>
 * 
 * <PRE>
 * <CODE>
 * &lt;mailet match=&quot;SenderDomainIs=&lt;domain.com&gt;&quot; class=&quot;&lt;any-class&gt;&quot;&gt;
 * </CODE>
 * </PRE>
 * 
 * @version CVS $Revision$ $Date$
 * @since 3.2.0
 */

public class SenderDomainIs extends GenericMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SenderDomainIs.class);

    private Collection<Domain> senderDomains;

    @Override
    public void init() {
        String condition = getCondition();
        Preconditions.checkNotNull(condition, "'condition' should not be null");

        senderDomains = parseDomainsList(condition);
    }

    @VisibleForTesting
    Collection<Domain> parseDomainsList(String condition) {
        return Splitter.onPattern("(, |,| )")
                .omitEmptyStrings()
                .splitToList(condition)
                .stream()
                .map(Domain::of)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public Collection<MailAddress> match(Mail mail) throws MessagingException {
        try {
            MaybeSender maybeSender = mail.getMaybeSender();
            if (maybeSender.asOptional()
                    .map(MailAddress::getDomain)
                    .map(senderDomains::contains)
                    .orElse(false)) {
                return mail.getRecipients();
            }
        } catch (Exception e) {
            LOGGER.error("Exception happened while finding sender domain match", e);
        }
        return ImmutableList.of();
    }

}
