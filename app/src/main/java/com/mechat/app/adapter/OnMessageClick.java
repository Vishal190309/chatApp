package com.mechat.app.adapter;

import com.mechat.app.databinding.IncomingMessageBinding;
import com.mechat.app.databinding.OutgoingMessageBinding;

public interface OnMessageClick {
    void OnReadMoreClick(OutgoingMessageBinding binding);

    void OnReadMoreClick(IncomingMessageBinding binding);
}
