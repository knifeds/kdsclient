package com.knifeds.kdsclient.hardware;

public class HardwareControllerFactory {
    HardwareController controller = null;

    public HardwareController getController() {
        if (controller != null)
            return controller;

        controller = new GenericHardwareController();
        return controller;
    }
}
