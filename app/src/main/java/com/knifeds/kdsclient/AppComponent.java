package com.knifeds.kdsclient;

import com.knifeds.kdsclient.hardware.HardwareController;
import com.knifeds.kdsclient.hardware.HardwareControllerFactory;
import com.knifeds.kdsclient.ui.MainActivity;

import javax.inject.Singleton;

import dagger.Provides;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import dagger.android.ContributesAndroidInjector;

@Singleton
@dagger.Component(modules = {
        AndroidInjectionModule.class,
        AppComponent.Module.class,
        MqttServiceModule.class
})
public interface AppComponent extends AndroidInjector<MyApplication> {
    @dagger.Module
    abstract class Module {
        @ContributesAndroidInjector
        abstract MainActivity contributeActivityInjector();

        @Provides
        public static HardwareController provideHardwareController() {
            return new HardwareControllerFactory().getController();
        }
    }
}
