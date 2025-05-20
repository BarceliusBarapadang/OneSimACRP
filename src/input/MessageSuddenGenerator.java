/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import java.util.Random;

import core.Settings;
import core.SettingsError;

/**
 * Message creation -external events generator. Creates uniformly distributed
 * message creation patterns whose message size and inter-message intervals can
 * be configured.
 */
public class MessageSuddenGenerator extends MessageEventGenerator {

    public MessageSuddenGenerator(Settings s) {
        super(s);
    }

    /**
     * Returns the next message creation event
     *
     * @see input.EventQueue#nextEvent()
     */
    public ExternalEvent nextEvent() {
        int responseSize = 0;
        /* zero stands for one way messages */
        int msgSize;
        int interval;
        int from;
        int to;

        /* Get two *different* nodes randomly from the host ranges */
        from = drawHostAddress(this.hostRange);
        to = drawToAddress(hostRange, from);

        msgSize = drawMessageSize();
        interval = drawNextEventTimeDiff();

        /* Create event and advance to next event */
        MessageCreateEvent mce = new MessageCreateEvent(from, 0, this.getID(),
                msgSize, responseSize, this.nextEventsTime);
        this.nextEventsTime += interval;

        if (this.msgTime != null && this.nextEventsTime > this.msgTime[1]) {
            /* next event would be later than the end time */
            this.nextEventsTime = Double.MAX_VALUE;
        }

        return mce;
    }
    
    
}
