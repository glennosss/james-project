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

package org.apache.mailet.base;

import java.util.Optional;

import jakarta.mail.MessagingException;

import org.apache.mailet.MailetConfig;

import com.google.common.base.Strings;

/**
 * Collects utility methods.
 */
public class MailetUtil {
    
    /**
     * <p>Gets a boolean valued init parameter.</p>
     * @param config not null
     * @param name name of the init parameter to be queried
     * @return true when the init parameter is <code>true</code> (ignoring case);
     * false when the init parameter is <code>false</code> (ignoring case);
     * otherwise the default value
     */
    public static Optional<Boolean> getInitParameter(MailetConfig config, String name) {
        String value = config.getInitParameter(name);
        if ("true".equalsIgnoreCase(value)) {
            return Optional.of(true);
        }
        if ("false".equalsIgnoreCase(value)) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    public static int getInitParameterAsStrictlyPositiveInteger(String condition, int defaultValue) throws MessagingException {
        String defaultStringValue = String.valueOf(defaultValue);
        return getInitParameterAsStrictlyPositiveInteger(condition, Optional.of(defaultStringValue));
    }

    public static int getInitParameterAsStrictlyPositiveInteger(String condition) throws MessagingException {
        return getInitParameterAsStrictlyPositiveInteger(condition, Optional.empty());
    }

    private static int getInitParameterAsStrictlyPositiveInteger(String condition, Optional<String> defaultValue) throws MessagingException {
        String value = Optional.ofNullable(condition)
            .orElse(defaultValue.orElse(null));

        if (Strings.isNullOrEmpty(value)) {
            throw new MessagingException("Condition is required. It should be a strictly positive integer");
        }

        int valueAsInt = tryParseInteger(value);

        if (valueAsInt < 1) {
            throw new MessagingException("Expecting condition to be a strictly positive integer. Got " + value);
        }
        return valueAsInt;
    }

    private static int tryParseInteger(String value) throws MessagingException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new MessagingException("Expecting condition to be a strictly positive integer. Got " + value);
        }
    }
}
