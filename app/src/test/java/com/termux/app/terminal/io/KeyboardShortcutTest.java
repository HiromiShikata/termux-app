package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

public class KeyboardShortcutTest {

    @Test
    public void exposesCodePointAndShortcutAction() {
        KeyboardShortcut shortcut = new KeyboardShortcut('C', 7);

        Assert.assertEquals('C', shortcut.codePoint);
        Assert.assertEquals(7, shortcut.shortcutAction);
    }

    @Test
    public void supportsZeroAndNegativeValues() {
        KeyboardShortcut shortcut = new KeyboardShortcut(0, -1);

        Assert.assertEquals(0, shortcut.codePoint);
        Assert.assertEquals(-1, shortcut.shortcutAction);
    }
}
