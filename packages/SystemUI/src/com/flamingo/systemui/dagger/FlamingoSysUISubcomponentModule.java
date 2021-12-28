package com.flamingo.systemui.dagger;

import dagger.Module;

/**
 * Dagger module for including the WMComponent.
 */
@Module(subcomponents = {FlamingoSysUIComponent.class})
public abstract class FlamingoSysUISubcomponentModule {
}
