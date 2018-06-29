package com.redsponge.networking.chat;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public class TextAreaOutputStream extends OutputStream {

    private JTextArea area;

    public TextAreaOutputStream(JTextArea area) {
        this.area = area;
    }

    @Override
    public void write(int b) {
        area.append(String.valueOf((char)b));
        area.setCaretPosition(area.getDocument().getLength());
    }

}
