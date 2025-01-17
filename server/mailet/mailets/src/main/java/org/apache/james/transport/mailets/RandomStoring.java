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

package org.apache.james.transport.mailets;

import static org.apache.james.mailbox.MailboxManager.MailboxSearchFetchType.Minimal;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import jakarta.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.james.core.Username;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.apache.james.transport.mailets.delivery.MailStore;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.util.streams.Iterators;
import org.apache.mailet.Attribute;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import reactor.core.publisher.Mono;

/**
 * Process messages and randomly assign them to 4 to 8 mailboxes.
 */
public class RandomStoring extends GenericMailet {

    private static final int MIN_NUMBER_OF_RECIPIENTS = 4;
    private static final int MAX_NUMBER_OF_RECIPIENTS = 8;
    private static final Duration CACHE_DURATION = Duration.ofMinutes(15);

    private final Mono<List<ReroutingInfos>> reroutingInfos;
    private final UsersRepository usersRepository;
    private final MailboxManager mailboxManager;
    private final Supplier<Integer> randomRecipientsNumbers;

    @Inject
    public RandomStoring(UsersRepository usersRepository, MailboxManager mailboxManager) {
        this.usersRepository = usersRepository;
        this.mailboxManager = mailboxManager;
        this.randomRecipientsNumbers = () -> ThreadLocalRandom.current().nextInt(MIN_NUMBER_OF_RECIPIENTS, MAX_NUMBER_OF_RECIPIENTS + 1);
        this.reroutingInfos = Mono.fromCallable(this::retrieveReroutingInfos).cache(CACHE_DURATION);
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        Collection<ReroutingInfos> reroutingInfos = generateRandomMailboxes();
        Collection<MailAddress> mailAddresses = reroutingInfos
            .stream()
            .map(Throwing.function(info -> info.getUser().asMailAddress()))
            .collect(ImmutableList.toImmutableList());

        mail.setRecipients(mailAddresses);
        reroutingInfos.forEach(reroutingInfo ->
            mail.setAttribute(Attribute.convertToAttribute(MailStore.DELIVERY_PATH_PREFIX + reroutingInfo.getUser().asString(), reroutingInfo.getMailbox())));
    }

    @Override
    public String getMailetInfo() {
        return "Random Storing Mailet";
    }

    @Override
    public void init() throws MessagingException {
    }

    private Collection<ReroutingInfos> generateRandomMailboxes() {
        List<ReroutingInfos> reroutingInfos = this.reroutingInfos.block();

        // Replaces Collections.shuffle() which has a too poor statistical distribution
        return ThreadLocalRandom
            .current()
            .ints(0, reroutingInfos.size())
            .distinct()
            .mapToObj(reroutingInfos::get)
            .limit(randomRecipientsNumbers.get())
            .collect(ImmutableSet.toImmutableSet());
    }

    private List<ReroutingInfos> retrieveReroutingInfos() throws UsersRepositoryException {
        return Iterators.toStream(usersRepository.list())
            .flatMap(this::buildReRoutingInfos)
            .collect(ImmutableList.toImmutableList());
    }

    private Stream<ReroutingInfos> buildReRoutingInfos(Username username) {
        try {
            MailboxSession session = mailboxManager.createSystemSession(username);
            return mailboxManager
                .search(MailboxQuery.privateMailboxesBuilder(session).build(), Minimal, session)
                .toStream()
                .map(metaData -> new ReroutingInfos(metaData.getPath().getName(), username));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class ReroutingInfos {
        private final String mailbox;
        private final Username username;

        ReroutingInfos(String mailbox, Username username) {
            this.mailbox = mailbox;
            this.username = username;
        }

        public String getMailbox() {
            return mailbox;
        }

        public Username getUser() {
            return username;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ReroutingInfos) {
                ReroutingInfos that = (ReroutingInfos) o;

                return Objects.equals(this.mailbox, that.mailbox)
                    && Objects.equals(this.username, that.username);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mailbox, username);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("mailbox", mailbox)
                .add("username", username)
                .toString();
        }
    }
}

