package com.flamingo.systemui.dagger;

import android.content.Context;

import com.android.systemui.dagger.GlobalModule;
import com.android.systemui.dagger.GlobalRootComponent;
import com.android.systemui.dagger.WMModule;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;

@Singleton
@Component(modules = {
        GlobalModule.class,
        FlamingoSysUISubcomponentModule.class,
        WMModule.class})
public interface FlamingoGlobalRootComponent extends GlobalRootComponent {

    @Component.Builder
    interface Builder extends GlobalRootComponent.Builder {
        FlamingoGlobalRootComponent build();
    }

    @Override
    FlamingoSysUIComponent.Builder getSysUIComponent();
}
