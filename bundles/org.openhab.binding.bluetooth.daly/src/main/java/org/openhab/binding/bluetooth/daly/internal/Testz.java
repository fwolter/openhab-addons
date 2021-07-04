package org.openhab.binding.bluetooth.daly.internal;

import java.util.function.Function;

public class Testz {
    public Testz() {
        Function<Daly.SocVoltageCurrent, Double> p = Daly.SocVoltageCurrent::pressureV;

        Function<Daly.Frame, Double> t = f -> ((Daly.SocVoltageCurrent) f.data()).pressureV();
    }
}
