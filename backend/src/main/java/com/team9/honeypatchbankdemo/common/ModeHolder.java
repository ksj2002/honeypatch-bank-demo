// ModeHolder.java
package com.team9.honeypatchbankdemo.common;

import org.springframework.stereotype.Component;

@Component
public class ModeHolder {
    private volatile Mode mode = Mode.VULNERABLE;
    public Mode get() { return mode; }
    public void set(Mode mode) { this.mode = mode; }
}