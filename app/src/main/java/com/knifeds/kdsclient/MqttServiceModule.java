package com.knifeds.kdsclient;

import com.knifeds.kdsclient.mqtt.MqttService;

import dagger.android.ContributesAndroidInjector;

@dagger.Module
abstract class MqttServiceModule {
    @ContributesAndroidInjector
    abstract MqttService contributeMqttService();
}
