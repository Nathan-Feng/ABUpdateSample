package com.nathan.abupdate.api;

import android.content.Context;


public interface HiABUpdate {

    void init();

    void setUpdateCallbackListener(HiABUpdateImpl.UpdateCallback callback);

    void applyUpdateConfig(String jsonUrl, Context context);

    void sendUpdateAction(@HiABUpdateImpl.UpdateAction int action);

    void switchSlot();

    void destroy();

}
