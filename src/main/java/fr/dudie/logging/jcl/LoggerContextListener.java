/*
 * Copyright (C) 2011 Jeremie Huchet
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.dudie.logging.jcl;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * A filter to initialize the mail logger handler.
 * <p>
 * Sample configuration to use in <code>web.xml</code> file:
 * 
 * <pre>
 * &lt;web-app&gt;
 * [...]
 *     &lt;listener&gt;
 *         &lt;listener-class&gt;fr.dudie.logging.jcl.LoggerContextListener&lt;/listener-class&gt;
 *     &lt;/listener&gt;
 *     &lt;context-param&gt;
 *         &lt;param-name&gt;application-name&lt;/param-name&gt;
 *         &lt;param-value&gt;Appname Server&lt;/param-value&gt;
 *     &lt;/context-param&gt;
 *     &lt;context-param&gt;
 *         &lt;param-name&gt;sender&lt;/param-name&gt;
 *         &lt;param-value&gt;noreply@appname.appspotmail.com&lt;/param-value&gt;
 *     &lt;/context-param&gt;
 *     &lt;context-param&gt;
 *         &lt;param-name&gt;recipient&lt;/param-name&gt;
 *         &lt;param-value&gt;observer@company.com&lt;/param-value&gt;
 *     &lt;/context-param&gt;
 *     &lt;context-param&gt;
 *         &lt;param-name&gt;lin-level&lt;/param-name&gt;
 *         &lt;param-value&gt;SEVERE&lt;/param-value&gt;
 *     &lt;/context-param&gt;
 * [...]
 * &lt;web-app&gt;
 * </pre>
 * 
 * @see https://github.com/kops/jcl-mail-handler
 * @author Jérémie Huchet
 */
public class LoggerContextListener implements ServletContextListener {

    /** The event logger. */
    private static final Logger LOGGER = Logger.getLogger(LoggerContextListener.class.getName());

    /** The application name. */
    private static final String APPLICATION = "application-name";

    /** The send mail address. */
    private static final String SENDER = "sender";

    /** The recipients mail addresses. */
    private static final String RECIPIENT = "recipient";

    /** The minimum log level of log events to forward. */
    private static final String MIN_LOG_LEVEL = "min-level";

    /**
     * Initializes the mail logger handler from parameters set in <code>web.xml</code> file.
     * <p>
     * {@inheritDoc}
     * 
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(final ServletContextEvent sce) {

        final ServletContext context = sce.getServletContext();

        // get the application name
        final String appName = context.getInitParameter(APPLICATION);
        if (null == appName || "".equals(appName.trim())) {
            throw new IllegalArgumentException(String.format(
                    "You must specify filter init-param named %s", APPLICATION));
        }

        // get the sender mail address
        final String from = context.getInitParameter(SENDER);

        // get the recipients mail addresses
        final String recipients = context.getInitParameter(RECIPIENT);
        if (null == recipients || recipients.trim().equals("")) {
            throw new IllegalArgumentException(String.format("You must scpecify filter init-param "
                    + "named %s (comma-separated list of mail addresses)", RECIPIENT));
        }

        final String[] to = recipients.split("[, ]");
        if (to.length <= 0) {
            throw new IllegalArgumentException("You must scpecify at least one mail address");
        }

        // get the minimum log level
        final Level level;
        final String minLevel = context.getInitParameter(MIN_LOG_LEVEL);
        if (null == minLevel) {
            level = Level.SEVERE;
        } else {
            level = Level.parse(minLevel);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Recipients: %s", Arrays.toString(to)));
            LOGGER.fine(String.format("Min level: %s", level.toString()));
        }

        try {
            final MailHandler h = new MailHandler(appName, from, to, level);
            Logger.getLogger("").addHandler(h);
        } catch (final AddressException e) {
            throw new IllegalArgumentException("Sender or recipients seems to be invalid", e);
        }

        final LogRecord handlerRegistered = new LogRecord(Level.INFO, "MailHandler registered");
        handlerRegistered.setSourceClassName(LoggerContextListener.class.getName());
        LOGGER.log(handlerRegistered);

    }

    /**
     * Send a notification when the context is destroyed. {@inheritDoc}
     * 
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(final ServletContextEvent sce) {

        final LogRecord handlerunRegistered = new LogRecord(Level.INFO, "MailHandler unregistered");
        handlerunRegistered.setSourceClassName(LoggerContextListener.class.getName());
        LOGGER.log(handlerunRegistered);
    }

}
