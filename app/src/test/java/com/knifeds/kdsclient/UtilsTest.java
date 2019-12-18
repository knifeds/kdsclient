package com.knifeds.kdsclient;

import com.knifeds.kdsclient.utils.StayForTrigger;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {
    @Test
    public void stayForTrigger_shouldFireOnTimeout() {
        StayForTrigger sft = StayForTrigger.getInstance();
        sft.reset();
        sft.setPeriod(5);
        try {
            sft.addEvent("testEvent");
            Thread.sleep(100);
            sft.addEvent("testEvent");
            Thread.sleep(900);
            sft.addEvent("testEvent");
            Thread.sleep(500);
            sft.addEvent("testEvent");
            Thread.sleep(500);
            sft.addEvent("testEvent");
            Thread.sleep(300);
            sft.addEvent("testEvent");
            Thread.sleep(700);
            sft.addEvent("testEvent");
            Thread.sleep(400);
            sft.addEvent("testEvent");
            Thread.sleep(600);
            sft.addEvent("testEvent");
            Thread.sleep(200);
            sft.addEvent("testEvent");
            Thread.sleep(700);
            sft.addEvent("testEvent");
            Thread.sleep(100);
            sft.addEvent("testEvent");
            assertTrue(sft.getFired());
        } catch (InterruptedException ignored) {

        }
    }

    @Test
    public void stayForTrigger_shouldNotFireUnderLimit() {
        StayForTrigger sft = StayForTrigger.getInstance();
        sft.reset();
        sft.setPeriod(2);
        try {
            sft.addEvent("testEvent");
            Thread.sleep(100);
            sft.addEvent("testEvent");
            Thread.sleep(900);
            sft.addEvent("testEvent");
            Thread.sleep(500);
            sft.addEvent("testEvent");
            Thread.sleep(1000);
            sft.addEvent("testEvent");
            Thread.sleep(300);
            sft.addEvent("testEvent");
            Thread.sleep(700);
            sft.addEvent("testEvent");
            Thread.sleep(400);
            sft.addEvent("testEvent");
            Thread.sleep(1100);
            sft.addEvent("testEvent");
            Thread.sleep(200);
            sft.addEvent("testEvent");
            Thread.sleep(700);
            sft.addEvent("testEvent");
            Thread.sleep(1200);
            sft.addEvent("testEvent");
            assertFalse(sft.getFired());
        } catch (InterruptedException ignored) {

        }
    }
}
