package com.flamingo.systemui;

import android.content.Context;

import com.flamingo.systemui.dagger.FlamingoGlobalRootComponent;
import com.flamingo.systemui.dagger.DaggerFlamingoGlobalRootComponent;

import com.android.systemui.SystemUIFactory;
import com.android.systemui.dagger.GlobalRootComponent;

public class FlamingoSystemUIFactory extends SystemUIFactory {
    @Override
    protected GlobalRootComponent buildGlobalRootComponent(Context context) {
        return DaggerFlamingoGlobalRootComponent.builder()
                .context(context)
                .build();
    }
}
