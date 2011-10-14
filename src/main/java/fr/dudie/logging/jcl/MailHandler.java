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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * A logger {@link Handler} to forward log records to a list of recipients by mail.
 * 
 * @author Jérémie Huchet
 */
public class MailHandler extends Handler {

    /** The name of the anonymous logger. */
    private final static String ANONYMOUS_LOGGER_NAME = Logger.getAnonymousLogger().getName();

    /** The name of the application. */
    private final String appName;

    /** The mail address of the sender of the mail notifications. */
    private final Address sender;

    /** The list of recipients of the mail notifications. */
    private final Address[] recipients;

    /** The minimum log {@link Level} to consider to forward log records by mail. */
    private final Level minLevel;

    /** Empty property set used to initialize the {@link Session}. */
    private final Properties props = new Properties();

    /**
     * Creates a mail handler.
     * 
     * @param pApplicationName
     *            the application name / descriptor
     * @param pFrom
     *            the mail address of the sender of mail notifications
     * @param pRecipients
     *            the list of recipients
     * @param pMinimumLevel
     *            the minimum level to consider
     * @throws AddressException
     *             one of the given mail addresses isn't valid
     */
    public MailHandler(final String pApplicationName, final String pFrom,
            final String[] pRecipients, final Level pMinimumLevel) throws AddressException {

        appName = pApplicationName;
        minLevel = pMinimumLevel;
        sender = new InternetAddress(pFrom);

        recipients = new Address[pRecipients.length];
        for (int i = 0; i < pRecipients.length; i++) {
            recipients[i] = new InternetAddress(pRecipients[i]);
        }
    }

    /**
     * Sends an email containing the {@link LogRecord} to {@link #recipients}.
     * 
     * <pre>
     * An error event has been logged for application &lt;application name&gt;.
     * 
     * Date: dow mon dd hh:mm:ss zzz yyyy
     * Message: &lt;log record message&gt;
     * Stacktrace:
     * &lt;optional stacktrace&gt;
     * </pre>
     * 
     * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
     */
    @Override
    public void publish(final LogRecord record) {

        if (record.getLevel().intValue() >= minLevel.intValue()
                && !ANONYMOUS_LOGGER_NAME.equals(record.getLoggerName())) {

            final String subject = String.format("[%s] Error event logged", appName);

            final StringWriter body = new StringWriter();
            body.append("An error event has been logged for application ");
            body.append(appName).append(".");

            body.append("\n\nDate: ").append(new Date(record.getMillis()).toString());
            body.append("\nMessage: ");
            body.append(String.format(record.getMessage(), record.getParameters()));

            if (null != record.getThrown()) {
                body.append("\nStacktrace:\n");
                record.getThrown().printStackTrace(new PrintWriter(body));
            }
            body.append("\n");

            sendMail(subject, body.toString());

        } else if (LoggerContextListener.class.getName().equals(record.getSourceClassName())) {

            final String subject = String.format("[%s] Logger handler registering event", appName);

            final StringBuilder body = new StringBuilder();
            body.append("A logger handler have been modified: \n\n");
            body.append(record.getMessage());

            sendMail(subject, body.toString());
        }
    }

    /**
     * Sends a plain text email to {@link #recipients} from {@link #sender}.
     * 
     * @param subject
     *            the mail subject
     * @param body
     *            the mail body
     */
    private void sendMail(final String subject, final String body) {

        final Session session = Session.getDefaultInstance(props, null);

        final Message msg = new MimeMessage(session);
        try {
            msg.setFrom(sender);
            msg.addRecipients(Message.RecipientType.TO, recipients);
            msg.setSubject(subject);
            msg.setText(body);
            Transport.send(msg);
        } catch (final MessagingException e) {
            Logger.getAnonymousLogger().severe(
                    "Can't forward log record to recipients " + e.getMessage());
        }
    }

    /**
     * Does nothing.
     * 
     * @see java.util.logging.Handler#flush()
     */
    @Override
    public void flush() {

    }

    /**
     * Does nothing.
     * 
     * @see java.util.logging.Handler#close()
     */
    @Override
    public void close() throws SecurityException {

    }

}
