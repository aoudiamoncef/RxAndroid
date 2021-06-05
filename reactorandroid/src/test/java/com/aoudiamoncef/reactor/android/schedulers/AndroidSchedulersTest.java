/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aoudiamoncef.reactor.android.schedulers;

import android.os.Build;
import android.os.Looper;
import android.os.Message;

import com.aoudiamoncef.reactor.android.plugins.ReactorAndroidPlugins;
import com.aoudiamoncef.reactor.android.testutil.EmptyScheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowMessageQueue;
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.scheduler.Scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public final class AndroidSchedulersTest {

    @Before @After
    public void setUpAndTearDown() {
        ReactorAndroidPlugins.reset();
    }

    @Test
    public void mainThreadCallsThroughToHook() {
        final AtomicInteger called = new AtomicInteger();
        final Scheduler newScheduler = new EmptyScheduler();
        ReactorAndroidPlugins.setMainThreadSchedulerHandler(scheduler -> {
            called.getAndIncrement();
            return newScheduler;
        });

        assertSame(newScheduler, AndroidSchedulers.mainThread());
        assertEquals(1, called.get());

        assertSame(newScheduler, AndroidSchedulers.mainThread());
        assertEquals(2, called.get());
    }

    @Test
    public void fromNullThrows() {
        try {
            AndroidSchedulers.from(null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("looper == null", e.getMessage());
        }
    }

    @Test
    public void fromNullThrowsTwoArg() {
        try {
            AndroidSchedulers.from(null, false);
            fail();
        } catch (NullPointerException e) {
            assertEquals("looper == null", e.getMessage());
        }
    }

    @Test
    public void fromReturnsUsableScheduler() {
        assertNotNull(AndroidSchedulers.from(Looper.getMainLooper()));
    }

    @Test
    public void mainThreadAsyncMessagesByDefault() {
        ShadowLooper mainLooper = ShadowLooper.getShadowMainLooper();
        mainLooper.pause();
        ShadowMessageQueue mainMessageQueue = shadowOf(Looper.getMainLooper().getQueue());

        Scheduler main = AndroidSchedulers.mainThread();
        main.schedule(() -> {
        });

        Message message = mainMessageQueue.getHead();
        assertTrue(message.isAsynchronous());
    }

    @Test
    public void fromAsyncMessagesByDefault() {
        ShadowLooper mainLooper = ShadowLooper.getShadowMainLooper();
        mainLooper.pause();
        ShadowMessageQueue mainMessageQueue = shadowOf(Looper.getMainLooper().getQueue());

        Scheduler main = AndroidSchedulers.from(Looper.getMainLooper());
        main.schedule(() -> {
        });

        Message message = mainMessageQueue.getHead();
        assertTrue(message.isAsynchronous());
    }

    @Test
    public void asyncIgnoredPre16() {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 14);

        ShadowLooper mainLooper = ShadowLooper.getShadowMainLooper();
        mainLooper.pause();
        ShadowMessageQueue mainMessageQueue = shadowOf(Looper.getMainLooper().getQueue());

        Scheduler main = AndroidSchedulers.from(Looper.getMainLooper(), true);
        main.schedule(() -> {
        });

        Message message = mainMessageQueue.getHead();
        assertFalse(message.isAsynchronous());
    }
}